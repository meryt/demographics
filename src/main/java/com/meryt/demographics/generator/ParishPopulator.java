package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Estate;
import com.meryt.demographics.domain.place.Farm;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.place.Town;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.family.HouseholdGenerator;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.request.ParishParameters;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.HouseholdService;

@Slf4j
class ParishPopulator {

    private final ParishParameters parishParameters;

    private final HouseholdGenerator householdGenerator;

    private final FamilyGenerator familyGenerator;

    private final FamilyService familyService;

    private final HouseholdService householdService;

    private final DwellingPlaceService dwellingPlaceService;

    ParishPopulator(@NonNull ParishParameters parishParameters,
                    @NonNull HouseholdGenerator householdGenerator,
                    @NonNull FamilyGenerator familyGenerator,
                    @NonNull FamilyService familyService,
                    @NonNull HouseholdService householdService,
                    @NonNull DwellingPlaceService dwellingPlaceService) {
        this.parishParameters = parishParameters;
        this.householdGenerator = householdGenerator;
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.householdService = householdService;
        this.dwellingPlaceService = dwellingPlaceService;
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
                        createHouseholdToFillOccupation(template, town, occupationSlot.getKey());
                    }
                }
            }
        }

        moveHomelessHouseholdsIntoHouses(template);
    }

    private boolean shouldPersist() {
        return parishParameters.isPersist();
    }

    /**
     * Generates a household with living people in it, as of the reference date. Adds the household to one of the towns
     * or to the parish.
     *
     * @return the number of living people in the household on the reference date
     */
    private int addHousehold(@NonNull ParishTemplate parishTemplate) {

        FamilyParameters familyParameters = parishTemplate.getFamilyParameters();

        // If persist is true on the family template, the household and its inhabitants will be saved by this method
        // call.
        Household household = householdGenerator.generateHousehold(familyParameters);

        moveHouseholdToTownOrParish(household, parishTemplate);

        // After a household is created, there might be adult sons in it. Try to move them out and create their own
        // households too (based on their ability to find a wife).
        int childHouseholdPopulation = tryMoveOutSons(household, parishTemplate);

        return childHouseholdPopulation + household.getPopulation(familyParameters.getReferenceDate());
    }

    private void createHouseholdToFillOccupation(@NonNull ParishTemplate template,
                                                 @NonNull TownTemplate townTemplate,
                                                 @NonNull Occupation occupation) {
        FamilyParameters familyParameters = template.getFamilyParameters();
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
            return;
        }

        Person person = inhabitants.get(0);

        log.info(String.format("%s (%s) was created to take a job in %s as a %s", person.getName(),
                person.getSocialClass().getFriendlyName(), townTemplate.getTown().getName(),
                occupation.getName()));

        person.addOccupation(occupation, getJobStartDate(person));
        addHouseholdToDwellingPlaceOnWeddingDate(townTemplate.getTown(), household,
                getMoveInDate(person, familyParameters.getReferenceDate()));
        maybePersist(townTemplate.getTown());
        maybePersist(household);
    }

    /**
     * Puts the household into a town or the rural areas of the parish. The head of the household will look for a job
     * in the towns, and if a suitable one is found, the household moves to that town. Otherwise may move to a town
     * with population to spare, or to the parish itself, in rural areas.
     */
    private void moveHouseholdToTownOrParish(@NonNull Household household, @NonNull ParishTemplate parishTemplate) {
        LocalDate onDate = parishTemplate.getFamilyParameters().getReferenceDate();
        Person person = household.getHead(onDate);
        boolean placedInTown = findTownWithOccupationForHouseholdHead(parishTemplate, household, person);

        if (!placedInTown) {
            if (parishTemplate.hasRuralPopulationRemaining(onDate)) {
                addHouseholdToDwellingPlaceOnWeddingDate(parishTemplate.getParish(), household, getMoveInDate(person,
                        onDate));
            } else {
                // Add to a random town even without a job. First try to fill out any towns that have room left but
                // no more jobs.
                List<TownTemplate> townsWithNoOccupationsLeft = parishTemplate.getTowns().stream()
                        .filter(t -> t.getExpectedOccupations().isEmpty())
                        .collect(Collectors.toList());
                int size = townsWithNoOccupationsLeft.size();
                if (size > 0) {
                    TownTemplate townTemplate = townsWithNoOccupationsLeft.get(new Die(size).roll() - 1);
                    addHouseholdToDwellingPlaceOnWeddingDate(townTemplate.getTown(), household, getMoveInDate(person,
                            onDate));
                    return;
                }

                // Otherwise look for any town that has room
                List<TownTemplate> townsWithPopulationLeft = parishTemplate.getTowns().stream()
                        .filter(t -> t.getExpectedPopulation() > t.getTown().getPopulation(onDate))
                        .collect(Collectors.toList());

                size = townsWithPopulationLeft.size();
                if (size > 0) {
                    TownTemplate townTemplate = townsWithPopulationLeft.get(new Die(size).roll() - 1);
                    addHouseholdToDwellingPlaceOnWeddingDate(townTemplate.getTown(), household, getMoveInDate(person,
                            onDate));
                    return;
                }

                log.info("There was no room in the parish nor in any town. Adding household to parish");
                addHouseholdToDwellingPlaceOnWeddingDate(parishTemplate.getParish(), household, getMoveInDate(person,
                        onDate));
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

        if (person == null || person.getSocialClass() == null) {
            return false;
        }

        LocalDate onDate = template.getFamilyParameters().getReferenceDate();

        for (TownTemplate townTemplate : template.getTowns()) {
            if (moveIntoTownIfNoJobsInTown(townTemplate, household, person, onDate)) {
                return true;
            }

            if (findOccupationInTownForHouseholdHead(template.getParish(), townTemplate, household, person, onDate)) {
                return true;
            }
        }

        log.info(String.format("%s (%s) could not find a job or home in any town", person.getName(),
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
    private boolean moveIntoTownIfNoJobsInTown(@NonNull TownTemplate townTemplate,
                                               @NonNull Household household,
                                               @NonNull Person person,
                                               @NonNull LocalDate onDate) {
        if (townTemplate.getExpectedOccupations().size() == 0 && townTemplate.hasSpaceRemaining(onDate)) {
            log.info(String.format(
                    "There are no more jobs in %s but there is still population space remaining. " +
                            "%s (%s) will move in but not take a job.", townTemplate.getTown().getName(),
                    person.getName(), person.getSocialClass().getFriendlyName()));
            addHouseholdToDwellingPlaceOnWeddingDate(townTemplate.getTown(), household,
                    getMoveInDate(person, onDate));
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
    private boolean findOccupationInTownForHouseholdHead(@NonNull Parish parish,
                                                         @NonNull TownTemplate townTemplate,
                                                         @NonNull Household household,
                                                         @NonNull Person person,
                                                         @NonNull LocalDate onDate) {
        for (Map.Entry<Occupation, Integer> occupationSlot : townTemplate.getExpectedOccupations().entrySet()) {
            Occupation occupation = occupationSlot.getKey();
            if (personWillAcceptOccupation(person, occupation) && occupationSlot.getValue() > 0) {
                log.info(String.format("%s (%s) will take a job in %s as a %s", person.getName(),
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
                    addHouseholdToDwellingPlaceOnWeddingDate(parish, household,
                            getMoveInDate(person, onDate));
                } else {
                    addHouseholdToDwellingPlaceOnWeddingDate(townTemplate.getTown(), household,
                            getMoveInDate(person, onDate));
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
                .filter(h -> h.getPopulation(onDate) > 0
                        && h.getHead(onDate) != null
                        && h.getHead(onDate).getOccupations().isEmpty())
                .collect(Collectors.toList());

        for (Household household : possibleHouseholds) {
            Person head = household.getHead(onDate);
            if (head != null && personWillAcceptBetterOccupation(head, occupation)) {
                log.info(String.format("%s (%s) already lives in %s so will will take a job as a %s",
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

    private void addHouseholdToDwellingPlaceOnWeddingDate(@NonNull DwellingPlace dwellingPlace,
                                                          @NonNull Household household,
                                                          @NonNull LocalDate moveInDate) {
        if (shouldPersist() && dwellingPlace.getId() <= 0) {
            throw new IllegalStateException(String.format("%s %s should be saved before calling this method",
                    dwellingPlace.getType().name(), dwellingPlace.getName()));
        }
        Person headOfHousehold = household.getHead(moveInDate);
        Occupation occupationOnDate = headOfHousehold == null ? null : headOfHousehold.getOccupation(moveInDate);
        if (headOfHousehold != null && headOfHousehold.getSocialClass().getRank() >= SocialClass.GENTLEMAN.getRank()
                && headOfHousehold.getOccupations().isEmpty()) {
            // An unemployed gentleman or better moves into an estate rather than directly into the town or parish.
            moveGentlemanIntoEstate(dwellingPlace, headOfHousehold, household, moveInDate);
        } else if (headOfHousehold != null &&
                (headOfHousehold.getSocialClass().getRank() >= SocialClass.YEOMAN_OR_MERCHANT.getRank()
                || (occupationOnDate != null &&
                        occupationOnDate.getMinClass().getRank() >= SocialClass.YEOMAN_OR_MERCHANT.getRank()))) {
            // An employed gentleman or a yeoman/merchant moves into a house
            moveFamilyIntoNewHouse(dwellingPlace, household, moveInDate);
        } else if (headOfHousehold != null && occupationOnDate != null && occupationOnDate.isFarmOwner()) {
            log.info("Moving farmer onto farm");
            // Farm-owners move into a house on a farm
            moveFarmerOntoFarm(dwellingPlace, headOfHousehold, household, moveInDate);
        } else if (headOfHousehold != null & occupationOnDate != null && occupationOnDate.isRural()) {
            // Rural non-farm-owners get a house on an existing farm or estate, if possible, otherwise just a house in
            // the area.
            moveRuralLaborerOntoEstateOrFarm(dwellingPlace, headOfHousehold, household, moveInDate);
        } else {
            householdService.addToDwellingPlace(household, dwellingPlace, moveInDate, null);
        }
    }

    /**
     * Create a house on an estate, put the estate into the provided dwelling place, and add the household to the house.
     *
     * @param dwellingPlace the place (such as a parish or town) where the estate should be created
     * @param headOfHousehold the head of the household
     * @param household the household
     * @param moveInDate the date on which the inhabitants should move in
     */
    private void moveGentlemanIntoEstate(@NonNull DwellingPlace dwellingPlace,
                                         @NonNull Person headOfHousehold,
                                         @NonNull Household household,
                                         @NonNull LocalDate moveInDate) {
        Estate estate = new Estate();
        estate.setName(parishParameters.getAndRemoveRandomEstateName());
        estate = (Estate) maybePersist(estate);
        dwellingPlace.addDwellingPlace(estate);
        maybePersist(dwellingPlace);
        estate.addOwner(headOfHousehold, moveInDate, null);
        Dwelling manorHouse = new Dwelling();
        manorHouse.setName(estate.getName());
        estate.addDwellingPlace(manorHouse);
        manorHouse = (Dwelling) maybePersist(manorHouse);
        estate = (Estate) maybePersist(estate);
        manorHouse.addOwner(headOfHousehold, moveInDate, null);
        log.info(String.format("Generated estate '%s' for household of %s, %s", estate.getName(),
                headOfHousehold.getName(), headOfHousehold.getSocialClass().getFriendlyName()));
        householdService.addToDwellingPlace(household, manorHouse, moveInDate, null);
        maybePersist(household);
    }

    /**
     * Creates a house in a dwelling place and moves a household into it
     *
     * @param dwellingPlace the place, such as a town, parish, or estate
     * @param household the household
     * @param moveInDate the date they begin to inhabit the place
     */
    private void moveFamilyIntoNewHouse(@NonNull DwellingPlace dwellingPlace,
                                        @NonNull Household household,
                                        @NonNull LocalDate moveInDate) {
        Dwelling house = new Dwelling();
        maybePersist(house);
        dwellingPlace.addDwellingPlace(house);
        householdService.addToDwellingPlace(household, house, moveInDate, null);
        maybePersist(dwellingPlace);
        if (household.getHead(moveInDate) != null) {
            house.addOwner(household.getHead(moveInDate), moveInDate, null);
        }
        log.info(String.format("Moved %s into new house in %s", household.getFriendlyName(moveInDate),
                (dwellingPlace.getName() == null ? "a " + dwellingPlace.getType() : dwellingPlace.getName())));
    }

    private void moveFarmerOntoFarm(@NonNull DwellingPlace dwellingPlace,
                                    @NonNull Person headOfHousehold,
                                    @NonNull Household household,
                                    @NonNull LocalDate moveInDate) {
        Farm farm = new Farm();
        farm.setName(headOfHousehold.getLastName() + " Farm");
        maybePersist(farm);
        farm.addOwner(headOfHousehold, moveInDate, null);
        dwellingPlace.addDwellingPlace(farm);
        maybePersist(dwellingPlace);
        Dwelling farmHouse = new Dwelling();
        farm.addDwellingPlace(farmHouse);
        maybePersist(farmHouse);
        maybePersist(farm);
        farmHouse.addOwner(headOfHousehold, moveInDate, null);
        householdService.addToDwellingPlace(household, farmHouse, moveInDate, null);
        maybePersist(household);
        log.info(String.format("Created %s in %s", farm.getName(), dwellingPlace.getFriendlyName()));
    }

    private void moveRuralLaborerOntoEstateOrFarm(@NonNull DwellingPlace dwellingPlace,
                                                  @NonNull Person headOfHousehold,
                                                  @NonNull Household household,
                                                  @NonNull LocalDate moveInDate) {
        List<DwellingPlace> farmsInPlace = new ArrayList<>(dwellingPlace.getRecursiveDwellingPlaces(
                DwellingPlaceType.FARM));
        farmsInPlace.addAll(dwellingPlace.getRecursiveDwellingPlaces(DwellingPlaceType.ESTATE));

        Dwelling house = new Dwelling();
        maybePersist(house);
        householdService.addToDwellingPlace(household, house, moveInDate, null);
        house.addOwner(headOfHousehold, moveInDate, null);

        if (farmsInPlace.isEmpty()) {
            dwellingPlace.addDwellingPlace(house);
            maybePersist(dwellingPlace);
            log.info(String.format("%s could not find a farm so moved into a new house in %s",
                    household.getFriendlyName(moveInDate), dwellingPlace.getFriendlyName()));
        } else {
            Collections.shuffle(farmsInPlace);
            DwellingPlace farmOrEstate = farmsInPlace.get(0);
            farmOrEstate.addDwellingPlace(house);
            maybePersist(farmOrEstate);
            log.info(String.format("Moved %s into new house on %s", household.getFriendlyName(moveInDate),
                    farmOrEstate.getFriendlyName()));
        }
    }

    private DwellingPlace maybePersist(@NonNull DwellingPlace dwellingPlace) {
        if (shouldPersist()) {
            return dwellingPlaceService.save(dwellingPlace);
        } else {
            return dwellingPlace;
        }
    }

    private Household maybePersist(@NonNull Household household) {
        if (shouldPersist()) {
            return householdService.save(household);
        } else {
            return household;
        }
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
        if (household.getHead(onDate) == null) {
            householdService.resetHeadAsOf(household, onDate);
        }
        // We don't want to move out a son who is the head of the household due to the death of his father
        Person head = household.getHead(onDate);

        // Get all males who are at least 18 and are not head of household
        List<Person> adultSons = household.getInhabitants(onDate).stream()
                .filter(Person::isMale)
                .filter(p -> !p.equals(head))
                .filter(p -> p.getAgeInYears(onDate) >= 18)
                .collect(Collectors.toList());

        // Loop over sons and attempt to create families and households for them.
        int sonHouseholdPopulation = 0;
        for (Person adultSon : adultSons) {
            Family family = familyGenerator.generate(adultSon, parishTemplate.getFamilyParameters());
            if (family != null) {
                log.info(String.format("Adult son %s married on %s and moved out", adultSon.getName(),
                        family.getWeddingDate()));
                Household sonsHousehold = moveOutSon(household, family, parishTemplate);

                moveHouseholdToTownOrParish(sonsHousehold, parishTemplate);

                sonHouseholdPopulation += sonsHousehold.getPopulation(onDate);

            } else {
                log.info(String.format("Adult son %s could not find a wife and stayed home", adultSon.getName()));
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
        householdGenerator.addFamilyToHousehold(newHousehold, family, family.getWeddingDate());
        maybePersist(oldHousehold);

        if (!family.getHusband().isLiving(onDate)) {
            householdService.resetHeadAsOf(newHousehold, family.getHusband().getDeathDate());
        }

        maybePersist(newHousehold);

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
            moveHomelessHouseholdIntoHouse(household, onDate);
        }
    }

    /**
     * Move the household into a house, depending on their occupation and/or social class
     */
    private void moveHomelessHouseholdIntoHouse(@NonNull Household household, @NonNull LocalDate onDate) {
        Person head = household.getHead(onDate);
        if (head == null) {
            householdService.resetHeadAsOf(household, onDate);
            maybePersist(household);
            head = household.getHead(onDate);
            if (head == null) {
                log.warn(String.format("Unable to create a house for %s; the household had no viable head on the date",
                        household.getFriendlyName(onDate)));
                return;
            }
        }
        Occupation occupation = head.getOccupation(onDate);

        log.info(String.format("Looking for house for homeless family %s%s (%s)", household.getFriendlyName(onDate),
                occupation == null ? "" : " (" + occupation.getName() + ")",
                head.getSocialClass().name()));

        // Get the move in date so that it's not the reference date necessarily, but the marriage date if any.
        LocalDate moveInDate = getMoveInDate(head, onDate);
        DwellingPlace currentLocation = household.getDwellingPlace(onDate);
        if (currentLocation == null) {
            // Should never happen, but we can't create a house in the middle of literal nowhere
            log.warn(String.format("Could not create house for %s: household was not in any location",
                    household.getFriendlyName(moveInDate)));
            return;
        }

        if (occupation != null) {
            if (occupation.isDomesticServant()) {
                log.info("Moving domestic servant into house");
                moveDomesticServantIntoHouse(currentLocation, head, household, moveInDate);
            } else if (occupation.isFarmLaborer()) {
                log.info("Moving farm laborer onto farm");
                moveFarmLaborerOntoFarm(currentLocation, household, moveInDate);
            } else {
                // All other occupations just get a house
                moveFamilyIntoNewHouse(currentLocation, household, moveInDate);
            }
            return;
        }

        if (head.getSocialClass() == SocialClass.PAUPER) {
            // Move into a random house
            log.info("Moving pauper into random house");
            movePauperIntoHouse(currentLocation, household, moveInDate);
        } else {
            // Anyone not a pauper and not employed gets a house
            moveFamilyIntoNewHouse(currentLocation, household, moveInDate);
        }

    }

    /**
     * Find a gentleman's (or higher) house in the same area for this household to move into. If the gentleman lives on
     * an estate or farm, the family may move into its own house. Otherwise may move into the main house.
     */
    private void moveDomesticServantIntoHouse(@NonNull DwellingPlace currentLocation,
                                              @NonNull Person head,
                                              @NonNull Household household,
                                              @NonNull LocalDate moveInDate) {
        // Find a gentleman or higher household
        DwellingPlace parent = currentLocation;

        List<Household> leadingHouseholds;
        do {
            leadingHouseholds = parent.getLeadingHouseholds(moveInDate, SocialClass.GENTLEMAN, true).stream()
                    .filter(h -> h.getDwellingPlace(moveInDate) != null
                            && h.getDwellingPlace(moveInDate).getType() == DwellingPlaceType.DWELLING)
                    .collect(Collectors.toList());
        } while (leadingHouseholds.isEmpty() && (parent = parent.getParent()) != null);

        if (leadingHouseholds.isEmpty()) {
            // There were no leading households anywhere. Just create a house.
            moveFamilyIntoNewHouse(currentLocation, household, moveInDate);
            return;
        }
        Collections.shuffle(leadingHouseholds);
        Household employerHousehold = leadingHouseholds.get(0);
        // The employer household is definitely living in a DWELLING. But if he lives on an ESTATE or FARM, we might
        // want to create a separate house for the household on the estate, rather than moving them into the employer's
        // house. 50% chance he lives in a separate house, unless he is a pauper, in which case he always moves into
        // the main house.
        DwellingPlace employeeHouseholdParentPlace = employerHousehold.getDwellingPlace(moveInDate).getParent();
        if (head.getSocialClass().getRank() > SocialClass.PAUPER.getRank() && new Die(2).roll() == 1
                && (employeeHouseholdParentPlace.getType() == DwellingPlaceType.ESTATE
                || employeeHouseholdParentPlace.getType() == DwellingPlaceType.FARM)) {
                moveFamilyIntoNewHouse(employeeHouseholdParentPlace, household, moveInDate);
        } else {
            // Otherwise add the household directly to the employer's house.
            householdService.addToDwellingPlace(household, employerHousehold.getDwellingPlace(moveInDate), moveInDate,
                    null);
            log.info(String.format("%s moved into the house of their employer, %s",
                    household.getFriendlyName(moveInDate), employerHousehold.getFriendlyName(moveInDate)));
        }
    }

    /**
     * Find a random farm in this area for the family to move into. They will get their own house on the farm.
     */
    private void moveFarmLaborerOntoFarm(@NonNull DwellingPlace currentLocation,
                                         @NonNull Household household,
                                         @NonNull LocalDate moveInDate) {
        moveHouseholdIntoRandomDwellingPlaceOfType(currentLocation, DwellingPlaceType.FARM, household, moveInDate);
    }

    /**
     * Find a random house is this area to move into. The household will move into the house.
     */
    private void movePauperIntoHouse(@NonNull DwellingPlace currentLocation,
                                         @NonNull Household household,
                                         @NonNull LocalDate moveInDate) {
        moveHouseholdIntoRandomDwellingPlaceOfType(currentLocation, DwellingPlaceType.DWELLING, household, moveInDate);
    }

    /**
     * Finds a random dwelling place of the specified type for this household to move into. Starts at the current
     * location of the household and broadens the search until at least one random dwelling is found.
     */
    private void moveHouseholdIntoRandomDwellingPlaceOfType(@NonNull DwellingPlace currentLocation,
                                                            @NonNull DwellingPlaceType type,
                                                            @NonNull Household household,
                                                            @NonNull LocalDate moveInDate) {
        DwellingPlace parent = currentLocation;

        List<DwellingPlace> placesOfType;
        do {
            placesOfType = new ArrayList<>(parent.getRecursiveDwellingPlaces(type));
        } while (placesOfType.isEmpty() && (parent = parent.getParent()) != null);

        if (placesOfType.isEmpty()) {
            // There were no places of this type anywhere, just move the household into a house.
            moveFamilyIntoNewHouse(currentLocation, household, moveInDate);
            return;
        }

        Collections.shuffle(placesOfType);
        if (type == DwellingPlaceType.DWELLING) {
            // Just move the household into the house (you can't put a house inside a house)
            DwellingPlace house = placesOfType.get(0);
            List<Person> houseOwner = house.getOwners(moveInDate);
            log.info(String.format("Moved pauper %s into house of %s", household.getFriendlyName(moveInDate),
                    houseOwner.isEmpty() ? "id " + house.getId() : houseOwner.get(0).getName()));
            householdService.addToDwellingPlace(household, house, moveInDate, null);
        } else {
            // Create a house for the family and place it in the random dwelling place
            moveFamilyIntoNewHouse(placesOfType.get(0), household, moveInDate);
        }
    }

}
