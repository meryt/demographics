package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.time.LocalDateComparator;

@Slf4j
@Service
public class InheritanceService {

    private final FamilyGenerator familyGenerator;
    private final FamilyService familyService;
    private final PersonService personService;
    private final AncestryService ancestryService;

    public InheritanceService(@NonNull FamilyGenerator familyGenerator,
                              @NonNull FamilyService familyService,
                              @NonNull PersonService personService,
                              @NonNull AncestryService ancestryService) {
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.personService = personService;
        this.ancestryService = ancestryService;
    }

    /**
     * Gets the potential heirs for a person on a given date.
     *
     * @param person the person whose heirs we want to find
     * @param onDate the date (may be his death date, but if we're calling recursively for a grandchild where the
     *               parent predeceases the person, then it would be the grandparent's death date)
     * @param inheritanceStyle the inheritance style to use (used to determine whether women can inherit)
     * @param mayLookInFuture if false, does not consider people not yet born, or people who are dead but for whom
     *                        generation is not finished.
     * @return a list, possibly empty, of one or more potential heirs, sorted by death date
     */
    @NonNull
    public List<Person> findPotentialHeirsForPerson(@NonNull Person person,
                                                    @NonNull LocalDate onDate,
                                                    @NonNull TitleInheritanceStyle inheritanceStyle,
                                                    boolean mayLookInFuture) {
        List<Person> results = new ArrayList<>();
        List<Person> childrenByBirthDate = person.getChildren().stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .filter(p -> mayLookInFuture || p.getBirthDate().isBefore(onDate) || p.getBirthDate().isEqual(onDate))
                .collect(Collectors.toList());

        // Loop through sons in order of birth
        for (Person son : childrenByBirthDate.stream().filter(Person::isMale).collect(Collectors.toList())) {
            // A living son is immediately heir.
            if (son.isLiving(onDate) || (mayLookInFuture && son.getBirthDate().isAfter(onDate))) {
                results.add(son);
                return results;
            }
            // A dead son may himself have heirs. Find his heirs on the death date of his father.
            List<Person> sonsHeirs = findPotentialHeirsForPerson(son, onDate, inheritanceStyle, mayLookInFuture);
            if (!sonsHeirs.isEmpty()) {
                return sonsHeirs;
            } else if (mayLookInFuture && mayHaveOrBeHeir(son, onDate, inheritanceStyle.isMalesOnly())) {
                // A dead son may not have finished generation or may have children that may be heirs once finished
                // generation. Return the son for now since we don't know what will happen in the future.
                sonsHeirs.add(son);
                return sonsHeirs;
            }
        }

        // If this is a male-only inheritance, return immediately if no sons are alive or have living heirs.
        if (inheritanceStyle.isMalesOnly()) {
            return results;
        }

        List<Person> daughtersByBirthDate = childrenByBirthDate.stream()
                .filter(Person::isFemale)
                .collect(Collectors.toList());

        for (Person daughter : daughtersByBirthDate) {
            // A living daughter is a possible heir. But unlike sons, we don't return immediately, but continue looping
            // through all daughters, since daughters split an inheritance.
            if (daughter.isLiving(onDate) || (mayLookInFuture && daughter.getBirthDate().isAfter(onDate))) {
                results.add(daughter);
            } else {
                // A dead daughter may herself have heirs.
                List<Person> daughtersHeirs = findPotentialHeirsForPerson(daughter, onDate, inheritanceStyle,
                        mayLookInFuture);
                if (daughtersHeirs.isEmpty() && (mayLookInFuture && !daughter.isFinishedGeneration())) {
                    // if she does not have heirs but is not finished generation, add her anyway
                    results.add(daughter);
                } else {
                    results.addAll(daughtersHeirs);
                }
            }
        }

        return results.stream().sorted(Comparator.comparing(Person::getDeathDate)).distinct().collect(Collectors.toList());
    }

    @Nullable
    public Pair<Person, LocalDate> findHeirForPerson(@NonNull Person person,
                                                     @NonNull LocalDate onDate,
                                                     @NonNull TitleInheritanceStyle inheritanceStyle,
                                                     boolean mayLookInFuture) {
        List<Person> allHeirsOnDate = findPotentialHeirsForPerson(person, onDate, inheritanceStyle, mayLookInFuture)
                .stream()
                .filter(p -> mayLookInFuture || (p.isLiving(onDate)))
                .collect(Collectors.toList());
        if (allHeirsOnDate.isEmpty()) {
            return null;
        } else if (allHeirsOnDate.size() == 1) {
            return Pair.of(allHeirsOnDate.get(0), onDate);
        }

        // The heirs are sorted by increasing order of death date. Iterate over people as they die off, assuming
        // they have finished having children. When we reach a single person or an empty list, or when the next person
        // to die has not finished having children, return.
        Person nextToDie;
        LocalDate nextDeathDate;
        LocalDate lastDeathDate = null;
        do {
            nextToDie = allHeirsOnDate.get(0);
            log.info("Next person is " + nextToDie.toString());
            nextDeathDate = nextToDie.getDeathDate();
            if (mayLookInFuture && !nextToDie.isFinishedGeneration()) {
                log.info("Stopping search; this person has not finished generation");
                return null;
            }
            if (nextDeathDate.equals(lastDeathDate)) {
                // There's something wrong with the loop. The next death date should always be greater than the last.
                log.warn("Invalid loop condition: nextDeathDate equalled lastDeathDate of " + nextDeathDate.toString());
                return null;
            } else {
                lastDeathDate = nextDeathDate;
            }

            allHeirsOnDate = findPotentialHeirsForPerson(person, nextDeathDate.plusDays(1), inheritanceStyle, mayLookInFuture)
                    .stream()
                    .filter(p -> mayLookInFuture || (p.isLiving(onDate)))
                    .collect(Collectors.toList());
            if (allHeirsOnDate.isEmpty()) {
                return null;
            } else if (allHeirsOnDate.size() == 1) {
                return Pair.of(allHeirsOnDate.get(0), nextDeathDate);
            }

        } while (true);

    }

    void processDeath(@NonNull Person person) {
        LocalDate date = person.getDeathDate();
        distributeCashToHeirs(person, date);
        distributeRealEstateToHeirs(person, date);
    }

    @NonNull
    private List<Person> findHeirsForCashInheritance(@NonNull Person person, @NonNull LocalDate onDate) {
        final List<Person> heirs = new ArrayList<>();
        if (person.isMarried(onDate)) {
            heirs.add(person.getSpouse(onDate));
        }
        heirs.addAll(person.getLivingChildren(onDate));

        if (heirs.isEmpty() && person.getFather() != null && person.getFather().isLiving(onDate)) {
            heirs.add(person.getFather());
        }

        if (heirs.isEmpty() && person.getMother() != null && person.getMother().isLiving(onDate)) {
            heirs.add(person.getMother());
        }

        if (heirs.isEmpty()) {
            if (person.getFather() != null) {
                heirs.addAll(person.getFather().getLivingChildren(onDate).stream()
                        .filter(p -> p.getId() != person.getId())
                        .collect(Collectors.toList()));
            }
            if (person.getMother() != null) {
                heirs.addAll(person.getMother().getLivingChildren(onDate).stream()
                        .filter(p -> p.getId() != person.getId() && !heirs.contains(p))
                        .collect(Collectors.toList()));
            }
        }

        if (heirs.isEmpty()) {
            heirs.addAll(findPotentialHeirsForPerson(person, onDate, TitleInheritanceStyle.HEIRS_GENERAL, false));
        }

        if (heirs.isEmpty()) {
            log.info(String.format("Unable to find any living natural heirs for %d %s on %s. " +
                    "Will try to find a random neighbor.", person.getId(), person.getName(), onDate));

            DwellingPlace dwelling = person.getResidence(onDate.minusDays(1));
            if (dwelling != null) {
                List<Person> dwellingResidents = dwelling.getAllResidents(onDate);
                if (!dwellingResidents.isEmpty()) {
                    Collections.shuffle(dwellingResidents);
                    heirs.add(dwellingResidents.get(0));
                } else {
                    DwellingPlace parentDwelling = dwelling.getParent();
                    if (parentDwelling != null) {
                        dwellingResidents = parentDwelling.getAllResidents(onDate);
                        if (!dwellingResidents.isEmpty()) {
                            Collections.shuffle(dwellingResidents);
                            heirs.add(dwellingResidents.get(0));
                        }
                    }
                }
            }
        }
        return heirs;
    }

    @NonNull
    private List<Person> findHeirsForRealEstate(@NonNull Person person, @NonNull LocalDate onDate) {
        // Spouse is first
        final List<Person> heirs = new ArrayList<>();
        if (person.isMarried(onDate)) {
            heirs.add(person.getSpouse(onDate));
        }
        // Living children are second, in order of birth
        heirs.addAll(person.getLivingChildren(onDate).stream().sorted(Comparator.comparing(Person::getBirthDate))
                .collect(Collectors.toList()));
        if (!heirs.isEmpty()) {
            return heirs;
        }

        // If there is neither spouse nor living children, look for other heirs.
        heirs.addAll(findHeirsForCashInheritance(person, onDate));
        return heirs;
    }

    private Person findMaleHeirForEntailments(@NonNull Person person, @NonNull LocalDate onDate) {
        Person maleHeirForEntailments = findPotentialHeirsForPerson(person, onDate,
                TitleInheritanceStyle.HEIRS_MALE_GENERAL, false).stream()
                .filter(p -> p.isLiving(onDate))
                .min(Comparator.comparing(Person::getBirthDate)).orElse(null);
        Person father;
        while (maleHeirForEntailments == null && (father = person.getFather()) != null) {
            maleHeirForEntailments = findPotentialHeirsForPerson(father, onDate.plusDays(1),
                    TitleInheritanceStyle.HEIRS_MALE_GENERAL, false).stream()
                    .filter(p -> p.isLiving(onDate))
                    .min(Comparator.comparing(Person::getBirthDate)).orElse(null);
        }
        return maleHeirForEntailments;
    }

    private boolean mayHaveOrBeHeir(@NonNull Person person, @NonNull LocalDate onDate, boolean malesOnly) {
        if (malesOnly && person.isFemale()) {
            return false;
        }
        if (!person.isFinishedGeneration()) {
            // She may yet have children, so she may have or be an heir
            return true;
        }
        if (person.isLiving(onDate)) {
            // If she's still alive she may herself be an heir, even if she never had children
            return true;
        }
        // If she's dead but had children, they may be heirs if they were alive on her death date and/or had heirs of
        // their own.
        for (Person child : person.getChildren()) {
            LocalDate childOnDate = LocalDateComparator.max(onDate, person.getDeathDate());
            if (mayHaveOrBeHeir(child, childOnDate, malesOnly)) {
                return true;
            }
        }
        return false;
    }

    private void distributeCashToHeirs(@NonNull Person person, @NonNull LocalDate onDate) {

        PersonCapitalPeriod period = person.getCapitalPeriod(onDate.minusDays(1));
        if (period == null) {
            return;
        }
        period.setToDate(onDate);
        personService.save(person);

        double cash = period.getCapital();
        List<Person> heirs = findHeirsForCashInheritance(person, onDate);

        if (heirs.isEmpty()) {
            log.info("Failed to find any heirs. Inheritance will be lost.");
            return;
        }

        double cashPerPerson = cash / heirs.size();
        for (Person heir : heirs) {
            Relationship relationship = ancestryService.calculateRelationship(heir, person);
            log.info(String.format("%d %s (%s) received %.2f on %s", heir.getId(), heir.getName(),
                    (relationship == null ? "no relation" : relationship.getName()), cashPerPerson, onDate));
            heir.addCapital(cashPerPerson, onDate);
            personService.save(heir);
        }
    }

    private void distributeRealEstateToHeirs(@NonNull Person person, @NonNull LocalDate onDate) {
        List<DwellingPlace> realEstate = person.getOwnedDwellingPlaces(onDate.minusDays(1));
        if (realEstate.isEmpty()) {
            return;
        }

        boolean ownsEntailedProperty = realEstate.stream().anyMatch(DwellingPlace::isEntailed);
        Person maleHeirForEntailments = null;
        if (ownsEntailedProperty) {
            maleHeirForEntailments = findMaleHeirForEntailments(person, onDate);
            if (maleHeirForEntailments == null) {
                log.info(String.format(
                        "No male heir found for %s. Entailed dwelling place will go to random new person.",
                        person.getName()));
                maleHeirForEntailments = generateNewOwnerForEntailedDwelling(person, onDate);
            }
        }

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

        for (DwellingPlace dwelling : entailedPlaces) {
            if (dwelling.isEntailed() && maleHeirForEntailments != null) {
                log.info(String.format("%s is entailed. Giving to male heir %d %s.", dwelling.getFriendlyName(),
                        maleHeirForEntailments.getId(), maleHeirForEntailments.getName()));
                dwelling.addOwner(maleHeirForEntailments, onDate, maleHeirForEntailments.getDeathDate());
                log.info(String.format("%d %s inherits %s %s on %s", maleHeirForEntailments.getId(),
                        maleHeirForEntailments.getName(), dwelling.getType().getFriendlyName(),
                        dwelling.getLocationString(), onDate));
                personService.save(maleHeirForEntailments);
            }
        }

        List<Person> heirs = findHeirsForRealEstate(person, onDate);
        int i = 0;
        for (DwellingPlace estateOrFarm : unentailedEstates) {
            List<DwellingPlace> places = estateOrFarm.getDwellingPlaces().stream()
                    .filter(dp -> dp.getOwners(onDate.minusDays(1)).contains(person))
                    .collect(Collectors.toList());
            unentailedHouses.removeAll(places);
            Person heir;
            if (heirs.isEmpty()) {
                heir = findPossibleHeirForDwellingPlace(estateOrFarm, onDate.plusDays(1));
                if (heir == null) {
                    heir = generateNewOwnerForEntailedDwelling(person, onDate);
                }
            } else {
                if (i == heirs.size()) {
                    i = 0;
                }
                heir = heirs.get(i++);
            }
            // Make him the owner of the estate as well as the given places.
            estateOrFarm.addOwner(heir, onDate, heir.getDeathDate());
            log.info(String.format("%d %s inherits %s %s on %s", heir.getId(), heir.getName(),
                    estateOrFarm.getType().getFriendlyName(),
                    estateOrFarm.getLocationString(), onDate));
            for (DwellingPlace estateBuilding : places) {
                log.info(String.format("%d %s inherits %s %s on %s", heir.getId(), heir.getName(),
                        estateBuilding.getType().getFriendlyName(),
                        estateBuilding.getLocationString(), onDate));
                estateBuilding.addOwner(heir, onDate, heir.getDeathDate());
            }
            personService.save(heir);
        }

        for (DwellingPlace house : unentailedHouses) {
            Person heir;
            if (heirs.isEmpty()) {
                heir = findPossibleHeirForDwellingPlace(house, onDate.plusDays(1));
                if (heir == null) {
                    heir = generateNewOwnerForEntailedDwelling(person, onDate);
                }
            } else {
                if (i == heirs.size()) {
                    i = 0;
                }
                heir = heirs.get(i++);
            }
            log.info(String.format("%d %s inherits %s in %s on %s", heir.getId(), heir.getName(),
                    house.getType().getFriendlyName(),
                    house.getLocationString(), onDate));
            house.addOwner(heir, onDate, heir.getDeathDate());
            personService.save(heir);
        }
    }

    @Nullable
    private Person findPossibleHeirForDwellingPlace(@NonNull DwellingPlace dwellingPlace, @NonNull LocalDate onDate) {
        List<Person> residents = dwellingPlace.getAllResidents(onDate).stream()
                .filter(p -> p.isLiving(onDate))
                .collect(Collectors.toList());
        if (residents.isEmpty()) {
            residents = dwellingPlace.getParent().getAllResidents(onDate).stream()
                    .filter(p -> p.isLiving(onDate))
                    .collect(Collectors.toList());
            if (residents.isEmpty()) {
                return null;
            }
        }
        Collections.shuffle(residents);
        return residents.get(0);
    }

    @NonNull
    private Person generateNewOwnerForEntailedDwelling(@NonNull Person formerOwner, @NonNull LocalDate onDate) {
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
}
