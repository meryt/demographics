package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.person.PersonOccupationPeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Estate;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.response.calendar.PropertyTransferEvent;

@Slf4j
@Service
public class InheritanceService {

    private final FamilyGenerator familyGenerator;
    private final FamilyService familyService;
    private final PersonService personService;
    private final AncestryService ancestryService;
    private final HeirService heirService;
    private final HouseholdService householdService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;
    private final DwellingPlaceService dwellingPlaceService;
    private final TitleService titleService;

    public InheritanceService(@NonNull FamilyGenerator familyGenerator,
                              @NonNull FamilyService familyService,
                              @NonNull PersonService personService,
                              @NonNull AncestryService ancestryService,
                              @NonNull HeirService heirService,
                              @NonNull HouseholdService householdService,
                              @NonNull HouseholdDwellingPlaceService householdDwellingPlaceService,
                              @NonNull DwellingPlaceService dwellingPlaceService,
                              @NonNull TitleService titleService) {
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.personService = personService;
        this.ancestryService = ancestryService;
        this.heirService = heirService;
        this.householdService = householdService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.titleService = titleService;
    }

    List<CalendarDayEvent> processDeath(@NonNull Person person) {
        LocalDate date = person.getDeathDate();
        List<CalendarDayEvent> events = new ArrayList<>(distributeRealEstateToHeirs(person, date));
        // Do cash after real estate, since a person inheriting cash might want to move
        distributeCashToHeirs(person, date);
        return events;
    }


    private void distributeCashToHeirs(@NonNull Person person, @NonNull LocalDate onDate) {

        PersonCapitalPeriod period = person.getCapitalPeriod(onDate);
        if (period == null) {
            return;
        }
        period.setToDate(onDate);
        personService.save(person);

        double cash = period.getCapital();
        if (cash == 0.0) {
            return;
        }

        // Find heirs, but don't pass a debt to an unrelated heir.
        List<Person> heirs = heirService.findHeirsForCashInheritance(person, onDate, (cash > 0));

        if (heirs.isEmpty()) {
            log.info("Failed to find any heirs. Inheritance will be lost.");
            return;
        }

        double cashPerPerson = cash / heirs.size();
        for (Person heir : heirs) {
            log.info(String.format("%.2f was inherited by %s.", cashPerPerson,
                    ancestryService.getLogMessageForHeirWithRelationship(heir, person)));
            heir.addCapital(cashPerPerson, onDate, ancestryService.getCapitalReasonMessageForHeirWithRelationship(heir, person));
            SocialClass newSocialClass = WealthGenerator.getSocialClassForInheritance(heir.getSocialClass(), cashPerPerson);
            if (newSocialClass.getRank() > heir.getSocialClass().getRank()) {
                log.info(String.format("%d %s has increased in rank from %s to %s", heir.getId(), heir.getName(),
                        heir.getSocialClass().getFriendlyName(), newSocialClass.getFriendlyName()));
                heir.setSocialClass(newSocialClass);
                maybeQuitJob(heir, onDate);
            }
            personService.save(heir);

            householdDwellingPlaceService.maybeMoveHouseholdToBetterHouse(heir, onDate,
                    ancestryService.getNewHouseUponInheritanceMessageWithRelationship(heir, person),
                    ancestryService.getPurchasedHouseUponInheritanceWithRelationshipMessage(heir, person));
        }
    }

    /**
     * After an inheritance that may change social rank, check the person's occupation to ensure it is still
     * compatible with his social class, and quite if not. Also ensure that he does not now have so much money that
     * he no longer needs to work.
     */
    private void maybeQuitJob(@NonNull Person person, @NonNull LocalDate onDate) {
        Occupation occupation = person.getOccupation(onDate);
        if (occupation == null) {
            return;
        }
        Integer expectedWage = WealthGenerator.getYearlyIncomeValueRangeForPersonWithOccupation(person, occupation)
                .getFirst();
        double expectedInterest = person.getCapitalNullSafe(onDate) * 0.04;
        if (expectedWage < expectedInterest || occupation.getMaxClass().getRank() < person.getSocialClassRank()) {
            List<PersonOccupationPeriod> periods = person.getOccupations().stream()
                    .filter(p -> p.contains(onDate)
                            && p.getOccupation().getMaxClass().getRank() < person.getSocialClassRank())
                    .collect(Collectors.toList());
            for (PersonOccupationPeriod period : periods) {
                log.info(String.format("%s is quitting job as %s after an inheritance",
                        person.getIdAndName(), occupation.getName()));
                period.setToDate(onDate);
            }
        }
    }

    private List<CalendarDayEvent> distributeRealEstateToHeirs(@NonNull Person person, @NonNull LocalDate onDate) {
        List<CalendarDayEvent> results = new ArrayList<>();
        List<DwellingPlace> realEstate = person.getOwnedDwellingPlaces(onDate.minusDays(1));
        if (realEstate.isEmpty()) {
            return results;
        }

        List<DwellingPlace> entailedToTitlePlaces = realEstate.stream()
                .filter(p -> p.getEntailedTitle() != null)
                .collect(Collectors.toList());

        List<DwellingPlace> entailedPlaces = realEstate.stream()
                .filter(DwellingPlace::isEntailed)
                .collect(Collectors.toList());

        List<DwellingPlace> unentailedEstates = realEstate.stream()
                .filter(dp -> !dp.isEntailed() &&
                        (dp.getType() == DwellingPlaceType.FARM || dp.getType() == DwellingPlaceType.ESTATE))
                .sorted(Comparator.comparing(DwellingPlace::getValue).reversed())
                .collect(Collectors.toList());

        List<DwellingPlace> unentailedHouses = realEstate.stream()
                .filter(dp -> !dp.isEntailed() && dp.getType() == DwellingPlaceType.DWELLING)
                .sorted(Comparator.comparing(DwellingPlace::getValue).reversed())
                .collect(Collectors.toList());

        Person unrelatedHeirForTitleEntailments = null;
        for (DwellingPlace dwelling : entailedToTitlePlaces) {
            Title title = dwelling.getEntailedTitle();
            Person titleHolder = getHeirForRealEstateEntailedToTitle(title, dwelling, onDate);

            if (titleHolder == null && title.isExtinct()) {
                if (unrelatedHeirForTitleEntailments == null) {
                    unrelatedHeirForTitleEntailments = titleService.findNewOwnerForEntailedProperties(title, person, onDate);
                }
                titleHolder = unrelatedHeirForTitleEntailments;
                dwelling.setEntailedTitle(null);
                title.getEntailedProperties().remove(dwelling);
                titleService.save(title);
            }

            if (titleHolder != null && !titleHolder.equals(dwelling.getOwner(onDate))) {
                String relationshipString = ancestryService.getLogMessageForHeirWithRelationship(titleHolder, person);
                log.info(String.format("%s is entailed to %s. Giving to title heir %s.",
                        dwelling.getFriendlyName(),
                        title.getName(),
                        relationshipString));
                dwelling.addOwner(titleHolder, onDate, titleHolder.getDeathDate(),
                        ancestryService.getDwellingPlaceReasonForHeirWithRelationship(titleHolder, person));
                dwelling = dwellingPlaceService.save(dwelling);
                results.add(new PropertyTransferEvent(onDate, dwelling));
                log.info(String.format("%s %s is inherited by %s on %s",
                        dwelling.getType().getFriendlyName(),
                        dwelling.getLocationString(),
                        relationshipString,
                        onDate));
                personService.save(titleHolder);
            }
            if (titleHolder != null && titleHolder.equals(dwelling.getOwner(onDate))) {
                // This dwelling should have been taken care of by the inheritance service. We don't need to do
                // anything here.
                entailedPlaces.remove(dwelling);
                unentailedEstates.remove(dwelling);
                unentailedHouses.remove(dwelling);
                if (dwelling instanceof Dwelling) {
                    maybeMoveHeirIntoInheritedHouse(person, titleHolder, onDate, (Dwelling) dwelling);
                }
            }
            // If there is currently no title holder (extinct or in abeyance), or for some reason the title holder
            // does not own it, proceed with inheritance as usual.
        }

        boolean ownsEntailedProperty = !entailedPlaces.isEmpty();
        Person maleHeirForEntailments = null;
        String relationToMaleHeirForEntailments = null;
        if (ownsEntailedProperty) {
            maleHeirForEntailments = heirService.findMaleHeirForEntailments(person, onDate);
            if (maleHeirForEntailments == null) {
                log.info(String.format(
                        "No male heir found for %s. Entailed dwelling place will go to random new person from elsewhere.",
                        person.getName()));
                maleHeirForEntailments = findOrGenerateNewOwnerForEntailedDwelling(person, onDate);
            }
            relationToMaleHeirForEntailments = ancestryService.getLogMessageForHeirWithRelationship(maleHeirForEntailments, person);
        }

        for (DwellingPlace dwelling : entailedPlaces) {
            if (dwelling.isEntailed() && maleHeirForEntailments != null) {
                log.info(String.format("%d %s is entailed. Giving to male heir %s.",
                        dwelling.getId(),
                        dwelling.getFriendlyName(),
                        relationToMaleHeirForEntailments));
                dwelling.addOwner(maleHeirForEntailments, onDate, maleHeirForEntailments.getDeathDate(),
                        ancestryService.getDwellingPlaceReasonForHeirWithRelationship(maleHeirForEntailments, person));
                dwelling = dwellingPlaceService.save(dwelling);
                results.add(new PropertyTransferEvent(onDate, dwelling));
                log.info(String.format("%d %s inherits %s %s on %s", maleHeirForEntailments.getId(),
                        maleHeirForEntailments.getName(), dwelling.getType().getFriendlyName(),
                        dwelling.getLocationString(), onDate));
                if (dwelling instanceof Estate) {
                    personService.maybeUpdateLastNameForNewOwnerOfEstate(maleHeirForEntailments, (Estate) dwelling, onDate);
                }
                if (dwelling instanceof Dwelling) {
                    results.addAll(maybeMoveHeirIntoInheritedHouse(person, maleHeirForEntailments,
                            onDate, (Dwelling) dwelling));
                }
                personService.save(maleHeirForEntailments);
            }
        }

        List<Person> heirs = heirService.findHeirsForRealEstate(person, onDate);
        int i = 0;
        for (DwellingPlace estateOrFarm : unentailedEstates) {
            List<DwellingPlace> places = estateOrFarm.getDwellingPlaces().stream()
                    .filter(dp -> person.equals(dp.getOwner(onDate.minusDays(1))))
                    .sorted(Comparator.comparing(DwellingPlace::getValue).reversed())
                    .collect(Collectors.toList());
            unentailedHouses.removeAll(places);
            Person heir;
            if (heirs.isEmpty()) {
                heir = heirService.findPossibleHeirForDwellingPlace(estateOrFarm, onDate.plusDays(1));
                if (heir == null) {
                    heir = findOrGenerateNewOwnerForEntailedDwelling(person, onDate);
                }
            } else {
                if (i == heirs.size()) {
                    i = 0;
                }
                heir = heirs.get(i++);
            }
            String heirMessage = ancestryService.getLogMessageForHeirWithRelationship(heir, person);
            // Make him the owner of the estate as well as the given places.
            estateOrFarm.addOwner(heir, onDate, heir.getDeathDate(),
                    ancestryService.getDwellingPlaceReasonForHeirWithRelationship(heir, person));
            estateOrFarm = dwellingPlaceService.save(estateOrFarm);
            results.add(new PropertyTransferEvent(onDate, estateOrFarm));
            if (estateOrFarm instanceof Estate) {
                personService.maybeUpdateLastNameForNewOwnerOfEstate(heir, (Estate) estateOrFarm, onDate);
            }
            log.info(String.format("%s %s is inherited by %s on %s",
                    estateOrFarm.getType().getFriendlyName(),
                    estateOrFarm.getLocationString(),
                    heirMessage,
                    onDate));
            for (DwellingPlace estateBuilding : places) {
                log.info(String.format("%s %s is inherited by %s on %s",
                        estateBuilding.getType().getFriendlyName(),
                        estateBuilding.getLocationString(),
                        heirMessage,
                        onDate));
                estateBuilding.addOwner(heir, onDate, heir.getDeathDate(),
                        ancestryService.getDwellingPlaceReasonForHeirWithRelationship(heir, person));
                results.add(new PropertyTransferEvent(onDate, estateBuilding));
                estateBuilding = dwellingPlaceService.save(estateBuilding);
                if (estateBuilding instanceof Dwelling) {
                    results.addAll(maybeMoveHeirIntoInheritedHouse(person, heir, onDate,
                            (Dwelling) estateBuilding));
                }
            }
            personService.save(heir);
        }

        for (DwellingPlace house : unentailedHouses) {
            Person heir;
            if (heirs.isEmpty()) {
                heir = heirService.findPossibleHeirForDwellingPlace(house, onDate.plusDays(1));
                if (heir == null) {
                    heir = findOrGenerateNewOwnerForEntailedDwelling(person, onDate);
                }
            } else {
                if (i == heirs.size()) {
                    i = 0;
                }
                heir = heirs.get(i++);
            }
            log.info(String.format("%d %s in %s is inherited by %s on %s", house.getId(),
                    house.getType().getFriendlyName(),
                    house.getLocationString(),
                    ancestryService.getLogMessageForHeirWithRelationship(heir, person),
                    onDate));
            house.addOwner(heir, onDate, heir.getDeathDate(),
                    ancestryService.getDwellingPlaceReasonForHeirWithRelationship(heir, person));
            house = dwellingPlaceService.save(house);
            results.add(new PropertyTransferEvent(onDate, house));
            personService.save(heir);
            results.addAll(maybeMoveHeirIntoInheritedHouse(person, heir, onDate, (Dwelling) house));
        }
        return results;
    }

    @NonNull
    private Person findOrGenerateNewOwnerForEntailedDwelling(@NonNull Person formerOwner, @NonNull LocalDate onDate) {
        List<Person> livingRelatives = personService.findLivingRelatives(formerOwner, onDate, 8L).stream()
                .filter(p -> p.isMale()
                        && p.getSocialClassRank() == formerOwner.getSocialClassRank()
                        && p.getHouseholds().isEmpty())
                .collect(Collectors.toList());

        if (!livingRelatives.isEmpty()) {
            Collections.shuffle(livingRelatives);
            return livingRelatives.get(0);
        }

        List<Person> livingNonRelatives = personService.findBySocialClassAndGenderAndIsLiving(
                Collections.singletonList(formerOwner.getSocialClass()), Gender.MALE, onDate).stream()
                .filter(p -> p.getHouseholds().isEmpty())
                .collect(Collectors.toList());
        if (!livingNonRelatives.isEmpty()) {
            Collections.shuffle(livingNonRelatives);
            return livingNonRelatives.get(0);
        }

        RandomFamilyParameters familyParameters = new RandomFamilyParameters();
        familyParameters.setReferenceDate(onDate);
        familyParameters.setPercentMaleFounders(1.0);
        familyParameters.setAllowExistingSpouse(true);
        familyParameters.setChanceGeneratedSpouse(0.9);
        familyParameters.setAllowMaternalDeath(true);
        familyParameters.setCycleToDeath(false);
        familyParameters.setMaxSocialClass(formerOwner.getSocialClass().plusOne());
        familyParameters.setMinSocialClass(formerOwner.getSocialClass().minusOne());
        familyParameters.setPersist(true);

        Family family = familyGenerator.generate(familyParameters);
        familyService.save(family);

        return family.getHusband();
    }

    private List<CalendarDayEvent> maybeMoveHeirIntoInheritedHouse(@NonNull Person deceasedPerson,
                                                                   @NonNull Person heir,
                                                                   @NonNull LocalDate onDate,
                                                                   @NonNull Dwelling dwelling) {
        List<CalendarDayEvent> results = new ArrayList<>();
        DwellingPlace heirsCurrentResidence = heir.getResidence(onDate);
        if (dwelling.equals(heirsCurrentResidence)) {
            // He already lives in the house, just return.
            log.info(String.format("Not moving %d %s into house since he or she already lives there", heir.getId(),
                    heir.getName()));
            return results;
        }
        if (heirsCurrentResidence == null || heirsCurrentResidence.getValue() < dwelling.getValue()) {
            log.info(String.format("Will move %d %s into house", heir.getId(),
                    heir.getName()));

            // If he doesn't live here or if his current house is not as nice as this one, move him and his
            // family into the house.
            Household heirsHousehold = heir.getHousehold(onDate);
            if (heirsHousehold == null) {
                log.info(String.format("%d %s does not have a household yet; creating", heir.getId(),
                        heir.getName()));
                heirsHousehold = householdService.createHouseholdForHead(heir, onDate, true);
            }
            log.info(String.format("Moving household of %d %s into house", heir.getId(),
                    heir.getName()));

            householdDwellingPlaceService.addToDwellingPlace(heirsHousehold, dwelling, onDate, null);

            if (heir.isMarried(onDate)) {
                // If the family emigrated when they married, the father would have been set to null on the maternity.
                // Since they are moving back to one of the parishes, set it to the father again so that checking may
                // resume.
                Person woman = heir.isFemale() ? heir : heir.getSpouse(onDate);
                Person man = heir.isFemale() ? heir.getSpouse(onDate) : heir;
                if (woman != null && woman.getMaternity() != null && woman.getMaternity().getFather() == null) {
                    log.info(String.format("%d %s was married on %s but not having relations. " +
                                    "Updating maternity since the couple has moved to a parish.",
                            woman.getId(), woman.getName(), onDate));
                    woman.getMaternity().setFather(man);
                    personService.save(woman);
                }
            }

            Household oldHousehold = deceasedPerson.getHousehold(onDate.minusDays(1));
            if (oldHousehold == null) {
                return results;
            }
            if (dwelling.equals(oldHousehold.getDwellingPlace(onDate))) {
                return maybeMoveCurrentHouseholdOutOfHouse(oldHousehold, onDate, heir, dwelling);
            }
        }
        return results;
    }

    /**
     * Used when a new household moves into a house as owners. Will move out an existing household into a buyable house,
     * or creates a new house for the household. Does not move out the household if the new owner is a parent, child,
     * or sibling of the other household's head.
     *
     * @param oldHousehold the household that might need to move
     * @param onDate the date the new household moves in
     * @param newOwner the new owner of the house
     * @param dwelling the house
     * @return an event if the household had to move out
     */
    private List<CalendarDayEvent> maybeMoveCurrentHouseholdOutOfHouse(@NonNull Household oldHousehold,
                                                                       @NonNull LocalDate onDate,
                                                                       @NonNull Person newOwner,
                                                                       @NonNull Dwelling dwelling) {
        Person otherHead = oldHousehold.getHead(onDate);
        if (otherHead == null) {
            otherHead = householdService.resetHeadAsOf(oldHousehold, onDate);
            if (otherHead == null) {
                log.info("Could not evict existing household as it has no candidate to be head of household");
                return new ArrayList<>();
            }
        }
        Relationship relationship = ancestryService.calculateRelationship(newOwner, otherHead, false);
        if (relationship == null ||
                !(relationship.isParentChildRelationship() ||
                        relationship.isSiblingRelationship() || relationship.isSelf()
                        || relationship.isMarriageOrPartnership())) {
            // Another household is living here, and they are not closely related to the heir. They
            // have to move out.
            DwellingPlace parish = dwelling;
            do {
                parish = parish.getParent();
            } while (parish != null && parish.getType() != DwellingPlaceType.PARISH);
            if (parish instanceof Parish) {

                log.info(String.format("%d %s is evicting current household from house", newOwner.getId(),
                        newOwner.getName()));

                return householdDwellingPlaceService.buyOrCreateOrMoveIntoEmptyHouse((Parish) parish,
                        oldHousehold, onDate, DwellingPlaceOwnerPeriod.ReasonToPurchase.EVICTION, true);
            }
        }
        return new ArrayList<>();
    }

    @Nullable
    private Person getHeirForRealEstateEntailedToTitle(@NonNull Title title,
                                                       @NonNull DwellingPlace dwelling,
                                                       @NonNull LocalDate onDate) {
        Person titleHolder = title.getHolder(onDate);
        if (titleHolder != null) {
            return titleHolder;
        }

        // Gets 0 or more heirs for a title that is either extinct or in abeyance.
        Pair<LocalDate, List<Person>> titleHeirs = titleService.getTitleHeirs(title);
        if (titleHeirs == null || titleHeirs.getSecond().isEmpty()) {
            return null;
        }
        if (titleHeirs.getSecond().size() == 1) {
            // Should never happen because we only call this method when there are 0 or more than 1 heirs.
            return titleHeirs.getSecond().get(0);
        }

        // First see whether the current owner has a living child who is a possible heir. If so, this person inherits.
        Person currentOwner = dwelling.getOwner(onDate.minusDays(1));
        if (currentOwner != null) {
            List<Person> children = currentOwner.getLivingChildren(onDate).stream()
                    .filter(p -> titleHeirs.getSecond().contains(p))
                    .sorted(Comparator.comparing(Person::getBirthDate))
                    .collect(Collectors.toList());
            if (!children.isEmpty()) {
                return children.get(0);
            }
        }

        // Get the oldest living potential heir and make him the heir of the real estate.
        List<Person> allResidentsOfDwelling = dwelling.getAllResidents(onDate);
        Optional<Person> oldestHeirAlreadyLivingInPlace = titleHeirs.getSecond().stream()
                .filter(allResidentsOfDwelling::contains)
                .max(Comparator.comparing(Person::getBirthDate).reversed());
        // An heir that is already living in the place gets priority, for the sake of continuity.
        // Otherwise take the oldest potential heir (may return null)
        return oldestHeirAlreadyLivingInPlace.orElseGet(() -> titleHeirs.getSecond().stream()
                .filter(p -> p.isLiving(onDate))
                .max(Comparator.comparing(Person::getBirthDate).reversed())
                .orElse(null));
    }
}
