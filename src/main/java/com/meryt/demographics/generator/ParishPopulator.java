package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Estate;
import com.meryt.demographics.domain.place.Farm;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.place.Town;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.family.HouseholdGenerator;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.request.ParishParameters;
import com.meryt.demographics.request.PlaceNameParameters;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.HouseholdDwellingPlaceService;
import com.meryt.demographics.service.HouseholdService;
import com.meryt.demographics.service.PersonService;

@Service
@Slf4j
public class ParishPopulator {

    private final HouseholdGenerator householdGenerator;
    private final FamilyGenerator familyGenerator;
    private final FamilyService familyService;
    private final HouseholdService householdService;
    private final DwellingPlaceService dwellingPlaceService;
    private final PersonService personService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;

    ParishPopulator(@NonNull @Autowired HouseholdGenerator householdGenerator,
                    @NonNull @Autowired FamilyGenerator familyGenerator,
                    @NonNull @Autowired FamilyService familyService,
                    @NonNull @Autowired HouseholdService householdService,
                    @NonNull @Autowired DwellingPlaceService dwellingPlaceService,
                    @NonNull @Autowired PersonService personService,
                    @NonNull @Autowired HouseholdDwellingPlaceService householdDwellingPlaceService) {
        this.householdGenerator = householdGenerator;
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.householdService = householdService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.personService = personService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
    }

    /**
     * Populate an entire parish. Generates households and adds them to the parish or towns in the parish, until the
     * expected population is at least met.
     *
     * @param template the template containing the parameters for the parish
     */
    void populateParish(@NonNull ParishTemplate template) {
        log.info("Beginning population of Parish " + template.getParish().getName());

        long currentPopulation = 0;
        while (currentPopulation < template.getExpectedTotalPopulation()) {
            currentPopulation += addHousehold(template);
        }

        for (TownTemplate town : template.getTowns()) {
            log.info("Filling town jobs from existing town population for town " + town.getTown().getName());
            fillTownJobsFromPopulation(town, template.getFamilyParameters().getReferenceDate());

            if (!town.getExpectedOccupations().isEmpty()) {
                List<String> unfilledJobs = town.getExpectedOccupations().entrySet().stream()
                        .map(e -> e.getKey().getName() + ": " + e.getValue())
                        .collect(Collectors.toList());
                log.info(String.format("Town '%s' still has unfilled occupations: %s", town.getTown().getName(),
                        String.join(", ", unfilledJobs)));

                log.info("Creating new households in this town to fill jobs.");
                for (Map.Entry<Occupation, Integer> occupationSlot : town.getExpectedOccupations().entrySet()) {
                    for (int i = 0; i < occupationSlot.getValue(); i++) {
                        createHouseholdToFillOccupation(template.getFamilyParameters(),
                                town.getTown(), occupationSlot.getKey(),
                                mayCreateNewEstate(town.getTown(), template.getParishParameters()),
                                template.getParishParameters().getPlaceNames());
                    }
                }
            }
        }

        moveHomelessHouseholdsIntoHouses(template);
        householdDwellingPlaceService.hireDomesticServants(template.getFamilyParameters().getReferenceDate());
    }

    /**
     * Generates a household with living people in it, as of the reference date. Adds the household to one of the towns
     * or to the parish.
     *
     * @return the number of living people in the household on the reference date
     */
    private int addHousehold(@NonNull ParishTemplate parishTemplate) {

        RandomFamilyParameters familyParameters = parishTemplate.getFamilyParameters();

        Household household = householdGenerator.generateHousehold(familyParameters);
        Person head = household.getHead(familyParameters.getReferenceDate());

        DwellingPlace currentDwellingPlace = household.getDwellingPlace(familyParameters.getReferenceDate());
        if (currentDwellingPlace == null || !currentDwellingPlace.isHouse()) {
            moveHouseholdToTownOrParish(household, parishTemplate);
        }

        // Generate the capital after he has moved into a town. We need to know whether he has an employment or whether
        // he lives off his rents.
        if (head != null) {
            personService.generateStartingCapitalForFounder(head, familyParameters.getReferenceDate());
        }

        // After a household is created, there might be adult sons in it. Try to move them out and create their own
        // households too (based on their ability to find a wife).
        int childHouseholdPopulation = tryMoveOutSons(household, parishTemplate);

        return childHouseholdPopulation + household.getPopulation(familyParameters.getReferenceDate());
    }

    public Household createHouseholdToFillOccupation(@NonNull RandomFamilyParameters familyParameters,
                                              @NonNull DwellingPlace town,
                                              @NonNull Occupation occupation,
                                              boolean mayCreateNewEstate,
                                              @Nullable PlaceNameParameters placeNameParameters) {
        familyParameters.setMinSocialClass(occupation.getMinClass());
        familyParameters.setMaxSocialClass(occupation.getMaxClass());
        if (occupation.isAllowMale() && occupation.isAllowFemale()) {
            familyParameters.setPercentMaleFounders(0.5);
        } else if (occupation.isAllowMale()) {
            familyParameters.setPercentMaleFounders(1.0);
        } else {
            familyParameters.setPercentMaleFounders(0.0);
        }

        // If persist is true on the family template, the household and its inhabitants will be saved by this method
        // call.
        Household household = householdGenerator.generateHousehold(familyParameters);

        List<Person> inhabitants = household.getInhabitants(familyParameters.getReferenceDate()).stream()
                .filter(p -> occupation.isAllowMale() ? p.isMale() : p.isFemale())
                .sorted(Comparator.comparing(Person::getBirthDate))
                .collect(Collectors.toList());
        if (inhabitants.isEmpty()) {
            log.warn("Unable to find a person of the appropriate gender to be a " + occupation.getName());
            return null;
        }

        Person person = inhabitants.get(0);

        log.info(String.format("%d %s (%s) was created to take a job in %s as a %s", person.getId(), person.getName(),
                person.getSocialClass().getFriendlyName(), town.getName(),
                occupation.getName()));

        person.addOccupation(occupation, getJobStartDate(person));
        personService.generateStartingCapitalForFounder(person, familyParameters.getReferenceDate());

        DwellingPlace newHouse = addHouseholdToDwellingPlaceOnDate(town, household,
                person, getMoveInDate(person, familyParameters.getReferenceDate()), mayCreateNewEstate);
        maybeRenameNewEstateOrFarm(placeNameParameters, newHouse, familyParameters.getReferenceDate());

        dwellingPlaceService.save(town);
        householdService.save(household);
        return household;
    }

    /**
     * Adds the household to a class- and occupation-appropriate dwelling place on the given date. The dwelling place
     * given is used as-is if it is a house, but if it is a town or parish, a house or farm is found or created as
     * appropriate.
     *
     * @param dwellingPlace The place where the household should move. If it is a house, they move into the house; if
     *                      it is any other type of dwelling place, a house, estate, or farm may be found, purchased,
     *                      or created, as appropriate.
     * @param household The household to move.
     * @param headOrOccupationHolder If non-null, this person will be used as the effective head of the household in
     *                               terms of using their occupation or social class to find an appropriate place to
     *                               live
     * @param moveInDate The date to move them in.
     * @return the dwelling place they have moved into. If it is a Farm or Estate, this will be returned; otherwise
     * it will be a Dwelling.
     */
    private DwellingPlace addHouseholdToDwellingPlaceOnDate(@NonNull DwellingPlace dwellingPlace,
                                                            @NonNull Household household,
                                                            @Nullable Person headOrOccupationHolder,
                                                            @NonNull LocalDate moveInDate,
                                                            boolean mayCreateNewEstate) {
        if (dwellingPlace.getId() <= 0) {
            throw new IllegalStateException(String.format("%s %s should be saved before calling this method",
                    dwellingPlace.getType().name(), dwellingPlace.getName()));
        }

        // No further checks need to be done if we are adding directly to a dwelling
        if (dwellingPlace instanceof Dwelling) {
            return householdDwellingPlaceService.addToDwellingPlace(household, dwellingPlace, moveInDate, null);
        }

        Person headOfHousehold = headOrOccupationHolder == null
                ? household.getHead(moveInDate)
                : headOrOccupationHolder;

        if (headOfHousehold == null) {
            // They will be added directly to the Town, Parish, etc.  Without a household head they are
            // made "homeless".
            return householdDwellingPlaceService.addToDwellingPlace(household, dwellingPlace, moveInDate, null);
        }

        // Determine the head's occupation before deciding what to do. If he's a gentleman or better it's not used,
        // but in most other cases it may be regarded.
        Occupation occupationOnDate = headOfHousehold.getOccupation(moveInDate);

        if (headOfHousehold.getSocialClass().isAtLeast(SocialClass.GENTLEMAN)
                && headOfHousehold.getOccupations().isEmpty() && mayCreateNewEstate) {
            // An unemployed gentleman or better moves into an estate rather than directly into the town or parish.
            return householdDwellingPlaceService.moveGentlemanIntoEstate(dwellingPlace, headOfHousehold, household,
                    moveInDate, DwellingPlaceOwnerPeriod.ReasonToPurchase.MOVE_TO_PARISH.getMessage());

        } else if (headOfHousehold.getSocialClass().isAtLeast(SocialClass.YEOMAN_OR_MERCHANT)
                || (occupationOnDate != null &&
                occupationOnDate.getMinClass().isAtLeast(SocialClass.YEOMAN_OR_MERCHANT))) {
            // An employed gentleman or a yeoman/merchant moves into a house
            return householdDwellingPlaceService.moveFamilyIntoNewHouse(dwellingPlace, household, moveInDate, null,
                    DwellingPlaceOwnerPeriod.ReasonToPurchase.MOVE_TO_PARISH.getMessage());

        } else if (occupationOnDate != null && occupationOnDate.isFarmOwner()) {
            log.info("Moving farmer onto farm");
            // Farm-owners move into a house on a farm
            return moveFarmerOntoFarm(dwellingPlace, headOfHousehold, household, moveInDate);
        } else if (occupationOnDate != null && occupationOnDate.isRural()) {
            // Rural non-farm-owners get a house on an existing farm or estate, if possible, otherwise just a house in
            // the area.
            return moveRuralLaborerOntoEstateOrFarm(dwellingPlace, headOfHousehold, household, moveInDate);
        } else {
            return householdDwellingPlaceService.addToDwellingPlace(household, dwellingPlace, moveInDate, null);
        }
    }

    private DwellingPlace moveFarmerOntoFarm(@NonNull DwellingPlace dwellingPlace,
                                             @NonNull Person headOfHousehold,
                                             @NonNull Household household,
                                             @NonNull LocalDate moveInDate) {
        Farm farm = new Farm();
        farm.setFoundedDate(moveInDate);
        farm.setName(headOfHousehold.getLastName() + " Farm");
        farm.setValue(WealthGenerator.getRandomLandValue(headOfHousehold.getSocialClass()));
        dwellingPlaceService.save(farm);
        farm.addOwner(headOfHousehold, moveInDate, headOfHousehold.getDeathDate(),
                DwellingPlaceOwnerPeriod.ReasonToPurchase.MOVE_TO_PARISH.getMessage());
        dwellingPlace.addDwellingPlace(farm);
        dwellingPlaceService.save(dwellingPlace);
        Dwelling farmHouse = new Dwelling();
        farmHouse.setFoundedDate(moveInDate);
        farmHouse.setAttachedToParent(true);
        farmHouse.setValue(WealthGenerator.getRandomHouseValue(headOfHousehold.getSocialClass()));
        farm.addDwellingPlace(farmHouse);
        dwellingPlaceService.save(farmHouse);
        dwellingPlaceService.save(farm);
        farmHouse.addOwner(headOfHousehold, moveInDate, headOfHousehold.getDeathDate(),
                DwellingPlaceOwnerPeriod.ReasonToPurchase.MOVE_TO_PARISH.getMessage());
        farmHouse = (Dwelling) householdDwellingPlaceService.addToDwellingPlace(household, farmHouse, moveInDate, null);
        householdService.save(household);
        log.info(String.format("Created %s in %s", farm.getName(), dwellingPlace.getFriendlyName()));
        return farmHouse;
    }

    private boolean mayCreateNewEstate(@NonNull DwellingPlace town, @NonNull ParishParameters parishParameters) {
        boolean mayCreateNewEstate = parishParameters.getRemainingEstates() > 0;
        // If there appears to be room to create a new estate, we should count the current estates and update the
        // settings on the parish params. (It's a somewhat expensive operation so only do it if necessary.)
        if (mayCreateNewEstate) {
            Parish parish = town.getParish();
            if (parish != null) {
                parishParameters.setCurrentEstates(parish.getRecursiveDwellingPlaces(DwellingPlaceType.ESTATE).size());
            }
            mayCreateNewEstate = parishParameters.getRemainingEstates() > 0;
        }
        return mayCreateNewEstate;
    }

    /**
     * Puts the household into a town or the rural areas of the parish. The head of the household will look for a job
     * in the towns, and if a suitable one is found, the household moves to that town. Otherwise may move to a town
     * with population to spare, or to the parish itself, in rural areas.
     */
    private void moveHouseholdToTownOrParish(@NonNull Household household, @NonNull ParishTemplate parishTemplate) {
        LocalDate onDate = parishTemplate.getFamilyParameters().getReferenceDate();
        Person person = household.getHead(onDate);
        ParishParameters parishParameters = parishTemplate.getParishParameters();
        boolean placedInTown = findTownWithOccupationForHouseholdHead(parishTemplate, household, person);

        if (!placedInTown) {
            if (parishTemplate.hasRuralPopulationRemaining(onDate)) {
                maybeRenameNewEstateOrFarm(parishParameters.getPlaceNames(), addHouseholdToDwellingPlaceOnDate(
                        parishTemplate.getParish(), household, person, getMoveInDate(person, onDate),
                        mayCreateNewEstate(parishTemplate.getParish(), parishParameters)), onDate);
            } else {
                // Add to a random town even without a job. First try to fill out any towns that have room left but
                // no more jobs.
                List<TownTemplate> townsWithNoOccupationsLeft = parishTemplate.getTowns().stream()
                        .filter(t -> t.getExpectedOccupations().isEmpty())
                        .collect(Collectors.toList());
                int size = townsWithNoOccupationsLeft.size();
                if (size > 0) {
                    TownTemplate townTemplate = townsWithNoOccupationsLeft.get(new Die(size).roll() - 1);
                    maybeRenameNewEstateOrFarm(parishParameters.getPlaceNames(), addHouseholdToDwellingPlaceOnDate(
                            townTemplate.getTown(), household, person, getMoveInDate(person, onDate),
                            mayCreateNewEstate(parishTemplate.getParish(), parishParameters)), onDate);
                    return;
                }

                // Otherwise look for any town that has room
                List<TownTemplate> townsWithPopulationLeft = parishTemplate.getTowns().stream()
                        .filter(t -> t.getExpectedPopulation() > t.getTown().getPopulation(onDate))
                        .collect(Collectors.toList());

                size = townsWithPopulationLeft.size();
                if (size > 0) {
                    TownTemplate townTemplate = townsWithPopulationLeft.get(new Die(size).roll() - 1);
                    maybeRenameNewEstateOrFarm(parishParameters.getPlaceNames(), addHouseholdToDwellingPlaceOnDate(
                            townTemplate.getTown(), household, person, getMoveInDate(person, onDate),
                            mayCreateNewEstate(parishTemplate.getParish(), parishParameters)), onDate);
                    return;
                }

                log.info("There was no room in the parish nor in any town. Adding household to parish");
                maybeRenameNewEstateOrFarm(parishParameters.getPlaceNames(), addHouseholdToDwellingPlaceOnDate(
                        parishTemplate.getParish(), household, person, getMoveInDate(person, onDate),
                        mayCreateNewEstate(parishTemplate.getParish(), parishParameters)), onDate);
            }
        }
    }

    /**
     * The household head will try to find a job opening in a town whose upper social class range corresponds to
     * his social class. If no town has such an opening, null is returned. If an opening is found, the person's
     * occupation will be set and the town will be returned so his household can be added to it.
     *
     * @param household the household of which the head is looking for a job
     * @return true if a job was found and the household was added to a town
     */
    private boolean findTownWithOccupationForHouseholdHead(@NonNull ParishTemplate template,
                                                           @NonNull Household household,
                                                           Person person) {

        if (person == null || person.getSocialClass() == null || !person.getOccupations().isEmpty()) {
            return false;
        }

        LocalDate onDate = template.getFamilyParameters().getReferenceDate();

        for (TownTemplate townTemplate : template.getTowns()) {
            if (moveIntoTownIfNoJobsInTown(template.getParishParameters().getPlaceNames(), townTemplate, household,
                    person, onDate)) {
                return true;
            }

            if (findOccupationInTownForHouseholdHead(template.getParishParameters(), template.getParish(),
                    townTemplate, household, person, onDate)) {
                return true;
            }
        }

        log.info(String.format("%d %s (%s) could not find a job or home in any town", person.getId(), person.getName(),
                person.getSocialClass().getFriendlyName()));

        return false;
    }


    /**
     * If the town has no jobs left but has population remaining, move the household into this town without taking a
     * job.
     *
     * @param townTemplate the town
     * @param household the household to move in if a job is found
     * @param person the household head looking for a job
     * @param onDate the reference date (in case the person has no wedding date)
     * @return true if the household moved into the town, else false
     */
    private boolean moveIntoTownIfNoJobsInTown(@NonNull PlaceNameParameters placeNameParameters,
                                               @NonNull TownTemplate townTemplate,
                                               @NonNull Household household,
                                               @NonNull Person person,
                                               @NonNull LocalDate onDate) {
        if (townTemplate.getExpectedOccupations().size() == 0 && townTemplate.hasSpaceRemaining(onDate)) {
            log.info(String.format(
                    "There are no more jobs in %s but there is still population space remaining. " +
                            "%d %s (%s) will move in but not take a job.", townTemplate.getTown().getName(),
                    person.getId(), person.getName(), person.getSocialClass().getFriendlyName()));
            maybeRenameNewEstateOrFarm(placeNameParameters, addHouseholdToDwellingPlaceOnDate(
                    townTemplate.getTown(), household, person, getMoveInDate(person, onDate), false), onDate);
            return true;
        }
        return false;
    }

    /**
     * Try to find an occupation for this person in this town, and if so, move the household into the town and give him
     * the occupation.
     *
     * @param townTemplate the town
     * @param household the household to move in if a job is found
     * @param person the household head looking for a job
     * @param onDate the reference date (in case the person has no wedding date)
     * @return true if a job was found and the household moved in, else false
     */
    private boolean findOccupationInTownForHouseholdHead(@NonNull ParishParameters parishParameters,
                                                         @NonNull Parish parish,
                                                         @NonNull TownTemplate townTemplate,
                                                         @NonNull Household household,
                                                         @NonNull Person person,
                                                         @NonNull LocalDate onDate) {
        for (Map.Entry<Occupation, Integer> occupationSlot : townTemplate.getExpectedOccupations().entrySet()) {
            Occupation occupation = occupationSlot.getKey();
            if (personWillAcceptOccupation(person, occupation) && occupationSlot.getValue() > 0) {
                log.info(String.format("%d %s (%s) will take a job in %s as a %s", person.getId(), person.getName(),
                        person.getSocialClass().getFriendlyName(), townTemplate.getTown().getName(),
                        occupationSlot.getKey().getName()));

                person.addOccupation(occupation, getJobStartDate(person));

                if (occupationSlot.getValue() == 1) {
                    townTemplate.getExpectedOccupations().remove(occupation);
                } else {
                    occupationSlot.setValue(occupationSlot.getValue() - 1);
                }
                // If it's a rural occupation, move to the parish rather than the town.
                if (occupation.isRural()) {
                    maybeRenameNewEstateOrFarm(parishParameters.getPlaceNames(),
                            addHouseholdToDwellingPlaceOnDate(parish, household, person, getMoveInDate(person, onDate),
                                    mayCreateNewEstate(parish, parishParameters)), onDate);
                } else {
                    maybeRenameNewEstateOrFarm(parishParameters.getPlaceNames(), addHouseholdToDwellingPlaceOnDate(
                            townTemplate.getTown(), household, person, getMoveInDate(person, onDate),
                            mayCreateNewEstate(parish, parishParameters)), onDate);
                }

                return true;
            }
        }
        return false;
    }

    private void fillTownJobsFromPopulation(@NonNull TownTemplate townTemplate, @NonNull LocalDate onDate) {
        Map<Occupation, Integer> jobsStillUnfilled = new HashMap<>();
        for (Map.Entry<Occupation, Integer> occupationEntry : townTemplate.getExpectedOccupations().entrySet()) {
            Occupation occupation = occupationEntry.getKey();
            int expected = occupationEntry.getValue();
            int filled = 0;
            for (int i = 0; i < expected; i++) {
                if (fillJobFromTown(occupation, townTemplate.getTown(), onDate)) {
                    filled++;
                }
            }
            jobsStillUnfilled.put(occupation, expected - filled);
        }

        for (Map.Entry<Occupation, Integer> occupationEntry : jobsStillUnfilled.entrySet()) {
            if (occupationEntry.getValue() == 0) {
                townTemplate.getExpectedOccupations().remove(occupationEntry.getKey());
            } else {
                townTemplate.getExpectedOccupations().put(occupationEntry.getKey(), occupationEntry.getValue());
            }
        }
    }

    private boolean fillJobFromTown(@NonNull Occupation occupation, @NonNull Town town, @NonNull LocalDate onDate) {
        List<Household> possibleHouseholds = town.getHouseholds(onDate).stream()
                .filter(h -> headIsAliveButHasNeverBeenEmployed(h, onDate))
                .collect(Collectors.toList());

        for (Household household : possibleHouseholds) {
            Person head = household.getHead(onDate);
            if (head != null && personWillAcceptBetterOccupation(head, occupation)) {
                log.info(String.format("%d %s (%s) already lives in %s so will will take a job as a %s",
                        head.getId(),
                        head.getName(),
                        head.getSocialClass().getFriendlyName(),
                        town.getName(),
                        occupation.getName()));

                head.addOccupation(occupation, getJobStartDate(head));
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether a person will accept an occupation on offer. The max social class for the occupation must
     * be the same as his occupation, and the gender must be allowed.
     */
    private boolean personWillAcceptOccupation(@NonNull Person person, @NonNull Occupation occupation) {
        return person.getSocialClass() == occupation.getMaxClass()
                && (!person.isMale() || occupation.isAllowMale())
                && (!person.isFemale() || occupation.isAllowFemale());
    }

    /**
     * Usually the occupation is assigned such that the person's class equals the max for the job. Now we allow the
     * person to take the job if his class is anywhere between min and max for the job.
     */
    private boolean personWillAcceptBetterOccupation(@NonNull Person person, @NonNull Occupation occupation) {
        return person.getSocialClass().getRank() >= occupation.getMinClass().getRank()
                && person.getSocialClass().getRank() <= occupation.getMaxClass().getRank()
                && (!person.isMale() || occupation.isAllowMale())
                && (!person.isFemale() || occupation.isAllowFemale());
    }

    /**
     * Gets a move-in date for the person. If the person has one or more families, gets the first one and takes the
     * wedding date (if any). Otherwise takes the reference date.
     */
    private LocalDate getMoveInDate(Person person, @NonNull LocalDate referenceDate) {
        if (person == null || person.getFamilies().isEmpty()) {
            return referenceDate;
        } else {
            Family family = person.getFamilies().iterator().next();
            return family.getWeddingDate() == null ? referenceDate : family.getWeddingDate();
        }
    }

    /**
     * Gets a job start date for the person. If he lived to at least 16, then use his 16th birthday. Otherwise take
     * his wedding date, if any. Otherwise take his death date.
     */
    private LocalDate getJobStartDate(@NonNull Person person) {

        if (person.isLiving(person.getBirthDate().plusYears(16))) {
            return person.getBirthDate().plusYears(16);
        }

        if (!person.getFamilies().isEmpty()) {
            Family family = person.getFamilies().iterator().next();
            if (family.getWeddingDate() != null) {
                return family.getWeddingDate();
            }
        }

        return person.getDeathDate();
    }

    /**
     * Finds any adult sons in the household and tries to find wives for them. If they marry, then they will move to
     * their own household.
     *
     * @param household the household that may contain sons
     * @param parishTemplate template used for family generation parameters
     * @return the number of residents in the household or households founded by sons, as of the reference date
     */
    private int tryMoveOutSons(@NonNull Household household, @NonNull ParishTemplate parishTemplate) {
        LocalDate onDate = parishTemplate.getFamilyParameters().getReferenceDate();

        // Ensure that if the head has died and the household records not been cleaned up, that we find a new head.
        // We don't move out a son if he is the head of his household.
        Person head = household.getHead(onDate);
        if (head == null) {
            head = householdService.resetHeadAsOf(household, onDate);
            if (head == null) {
                return 0;
            } else {
                personService.save(head);
            }
        }
        long headId = head.getId();

        // We don't want to move out a son who is the head of the household due to the death of his father
        // Get all males who are at least 18 and are not head of household
        List<Person> adultSons = household.getInhabitants(onDate).stream()
                .filter(Person::isMale)
                .filter(p -> p.getId() != headId)
                .filter(p -> p.getAgeInYears(onDate) >= 18)
                .collect(Collectors.toList());

        // Loop over sons and attempt to create families and households for them.
        int sonHouseholdPopulation = 0;
        for (Person adultSon : adultSons) {
            Family family = familyGenerator.generate(adultSon, parishTemplate.getFamilyParameters());
            if (family != null) {
                log.info(String.format("Adult son %d %s married on %s and moved out", adultSon.getId(),
                        adultSon.getName(), family.getWeddingDate()));
                personService.generateStartingCapitalForFounder(adultSon, onDate);
                Household sonsHousehold = moveOutSon(household, family, parishTemplate);

                moveHouseholdToTownOrParish(sonsHousehold, parishTemplate);

                sonHouseholdPopulation += sonsHousehold.getPopulation(onDate);

            } else {
                log.info(String.format("Adult son %d %s could not find a wife and stayed home", adultSon.getId(),
                        adultSon.getName()));
            }
        }
        return sonHouseholdPopulation;
    }

    /**
     * Perform the manipulations on the old household and the new household so that the son will be moved out.
     *
     * @param oldHousehold the son's former household
     * @param family the son's new family
     * @param parishTemplate the template used for parameters
     * @return the son's new household, containing his new family members
     */
    private Household moveOutSon(@NonNull Household oldHousehold,
                                 @NonNull Family family,
                                 @NonNull ParishTemplate parishTemplate) {

        family = familyService.save(family);

        LocalDate onDate = parishTemplate.getFamilyParameters().getReferenceDate();

        Household newHousehold = new Household();
        householdGenerator.addFamilyToHousehold(newHousehold, family, onDate);
        householdService.save(oldHousehold);

        if (!family.getHusband().isLiving(onDate)) {
            householdService.resetHeadAsOf(newHousehold, family.getHusband().getDeathDate());
        }

        householdService.save(newHousehold);

        return newHousehold;
    }

    /**
     * Process all existing households that are in a dwelling place type that is not DWELLING. These need to be moved
     * to a house somewhere.
     */
    private void moveHomelessHouseholdsIntoHouses(@NonNull ParishTemplate parishTemplate) {
        LocalDate onDate = parishTemplate.getFamilyParameters().getReferenceDate();

        List<Household> households = householdService.loadHouseholdsWithoutHouses(onDate);
        for (Household household : households) {
            householdDwellingPlaceService.moveHomelessHouseholdIntoHouse(household, onDate,
                    householdDwellingPlaceService.getMoveInDate(household, onDate),
                    DwellingPlaceOwnerPeriod.ReasonToPurchase.MOVE_TO_PARISH);
        }
    }

    /**
     * Given a dwelling place, which is almost certainly a house, check to see if the parent is an estate or farm. If
     * the parent is an estate or farm, and it does not have a valid name, give it a name and save it.
     *
     * If the owner's last name is null, sets it and his children's name to "of [EstateName]".
     *
     * @param house a newly created house (or something like a parish if the household was homeless)
     */
    private void maybeRenameNewEstateOrFarm(@Nullable PlaceNameParameters placeNameParameters,
                                            @NonNull DwellingPlace house,
                                            @NonNull LocalDate onDate) {
        if (house.getParent() == null || placeNameParameters == null) {
            return;
        }
        if (house.getParent() instanceof Farm) {
            maybeRenameNewFarm(placeNameParameters, (Farm) house.getParent());
        } else if (house.getParent() instanceof Estate) {
            String dwellingName = maybeRenameNewEstate(placeNameParameters, (Estate) house.getParent());
            if (dwellingName != null) {
                house.setName(dwellingName);
                dwellingPlaceService.save(house);
                Person owner = house.getOwner(onDate);
                if (owner != null && owner.getLastName() == null) {
                    personService.updatePersonLastName(owner, "of " + house.getParent().getName(), true, false);
                }
            }
        }
    }

    /**
     * Maybe rename the estate if we have some names available.
     * @return a name for the estate house
     */
    @Nullable
    private String maybeRenameNewEstate(@NonNull PlaceNameParameters placeNameParameters, @NonNull Estate estate) {
        if (!StringUtils.isBlank(estate.getName())) {
            return null;
        }
        Pair<String, String> estateName = placeNameParameters.getAndRemoveRandomEstateName();

        if (estateName != null) {
            estate.setName(estateName.getLeft());
            dwellingPlaceService.save(estate);
            if (estateName.getLeft() != null && estateName.getRight() != null) {
                return estateName.getLeft() + " " + estateName.getRight();
            } else if (estateName.getLeft() != null) {
                return estateName.getLeft() + " House";
            }
        }

        return null;
    }

    private void maybeRenameNewFarm(@NonNull PlaceNameParameters placeNameParameters, @NonNull Farm farm) {
        if (!StringUtils.isBlank(farm.getName()) && !("null farm").equalsIgnoreCase(farm.getName())) {
            return;
        }

        farm.setName(placeNameParameters.getAndRemoveRandomFarmName());
        dwellingPlaceService.save(farm);
    }

    private DwellingPlace moveRuralLaborerOntoEstateOrFarm(@NonNull DwellingPlace dwellingPlace,
                                                          @NonNull Person headOfHousehold,
                                                          @NonNull Household household,
                                                          @NonNull LocalDate moveInDate) {
        List<DwellingPlace> farmsInPlace = new ArrayList<>(dwellingPlace.getRecursiveDwellingPlaces(
                DwellingPlaceType.FARM));
        farmsInPlace.addAll(dwellingPlace.getRecursiveDwellingPlaces(DwellingPlaceType.ESTATE));

        Dwelling house = new Dwelling();
        house.setFoundedDate(moveInDate);
        house.setValue(WealthGenerator.getRandomHouseValue(headOfHousehold.getSocialClass()));
        house = (Dwelling) dwellingPlaceService.save(house);
        house = (Dwelling) householdDwellingPlaceService.addToDwellingPlace(household, house, moveInDate, null);
        house.addOwner(headOfHousehold, moveInDate, headOfHousehold.getDeathDate(),
                DwellingPlaceOwnerPeriod.ReasonToPurchase.MOVE_TO_PARISH.getMessage());

        if (farmsInPlace.isEmpty()) {
            dwellingPlace.addDwellingPlace(house);
            dwellingPlaceService.save(dwellingPlace);
            house = (Dwelling) dwellingPlaceService.save(house);
            log.info(String.format("%s could not find a farm so moved into a new house in %s",
                    household.getFriendlyName(moveInDate), dwellingPlace.getFriendlyName()));
        } else {
            Collections.shuffle(farmsInPlace);
            DwellingPlace farmOrEstate = farmsInPlace.get(0);
            farmOrEstate.addDwellingPlace(house);
            dwellingPlaceService.save(farmOrEstate);
            house = (Dwelling) dwellingPlaceService.save(house);
            log.info(String.format("Moved %s into new house on %s", household.getFriendlyName(moveInDate),
                    farmOrEstate.getFriendlyName()));
        }
        return house;
    }

    /**
     * Determines whether the head of the household as of this date has ever been or ever will be employed.
     *
     * @return false if there is no head on this date, or if the head has been employed at some point.
     */
    private boolean headIsAliveButHasNeverBeenEmployed(@NonNull Household household, @NonNull LocalDate onDate) {
        Person head = household.getHead(onDate);
        if (head == null) {
            return false;
        } else {
            return head.getOccupations().isEmpty();
        }
    }

}
