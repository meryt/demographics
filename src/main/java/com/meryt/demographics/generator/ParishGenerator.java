package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Estate;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.place.Town;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.family.HouseholdGenerator;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.request.ParishParameters;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.HouseholdDwellingPlaceService;
import com.meryt.demographics.service.HouseholdService;
import com.meryt.demographics.service.OccupationService;
import com.meryt.demographics.service.PersonService;

import static com.meryt.demographics.domain.place.DwellingPlace.ACRES_PER_SQUARE_MILE;

@Service
@Slf4j
public class ParishGenerator {

    private final Die d4 = new Die(4);

    private final OccupationService occupationService;
    private final FamilyGenerator familyGenerator;
    private final FamilyService familyService;
    private final PersonService personService;
    private final HouseholdService householdService;
    private final DwellingPlaceService dwellingPlaceService;
    private final HouseholdGenerator householdGenerator;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;

    public ParishGenerator(@Autowired @NonNull OccupationService occupationService,
                           @Autowired @NonNull FamilyGenerator familyGenerator,
                           @Autowired @NonNull FamilyService familyService,
                           @Autowired @NonNull PersonService personService,
                           @Autowired @NonNull HouseholdService householdService,
                           @Autowired @NonNull DwellingPlaceService dwellingPlaceService,
                           @Autowired @NonNull HouseholdDwellingPlaceService householdDwellingPlaceService,
                           @Autowired @NonNull HouseholdGenerator householdGenerator) {
        this.occupationService = occupationService;
        this.familyGenerator = familyGenerator;
        this.householdService = householdService;
        this.familyService = familyService;
        this.personService = personService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.householdGenerator = householdGenerator;
    }

    /**
     * Generates a parish with its towns, households, and inhabitants. Does not save.
     */
    public Parish generateParish(@NonNull ParishParameters parishParameters) {
        if (parishParameters.getFamilyParameters() == null) {
            throw new IllegalArgumentException("RandomFamilyParameters are required when creating a parish");
        }
        if (parishParameters.getFamilyParameters().getReferenceDate() == null) {
            throw new IllegalArgumentException("Reference date is required when creating a parish");
        }
        DwellingPlace location = null;
        if (parishParameters.getLocationId() != null) {
            location = dwellingPlaceService.load(parishParameters.getLocationId());
            if (location == null) {
                throw new IllegalArgumentException(String.format(
                        "Cannot create parish: there is no location with ID %d to add it to.",
                        parishParameters.getLocationId()));
            }
            if (!location.getType().canContain(DwellingPlaceType.PARISH)) {
                throw new IllegalArgumentException(
                        String.format("Cannot create parish: the location %d %s of type %s cannot contain a parish.",
                        location.getId(), location.getName(), location.getType().name()));
            }
        } else if (parishParameters.getLocation() != null) {
            // Create the location if it does not exist
            location = parishParameters.getLocation();
            if (location.getId() <= 0 && parishParameters.isPersist()) {
                location = dwellingPlaceService.save(location);
            } else {
                throw new IllegalArgumentException("Cannot create parish: to create a new Region to contain this " +
                    "parish, use the locationId parameter rather than a location object with a non-null ID.");
            }
        }
        // Ensure that the parish parameters and family parameters have same setting for persist
        parishParameters.getFamilyParameters().setPersist(parishParameters.isPersist());

        Parish parish = new Parish();
        parish.setAcres(parishParameters.getSquareMiles() * ACRES_PER_SQUARE_MILE);
        if (parishParameters.getParishName() != null) {
            parish.setName(parishParameters.getParishName());
        } else {
            parish.setName("Parish");
        }

        long totalPopulation = parishParameters.getPopulation();
        long currentPopulation = 0;

        if (parishParameters.isPersist()) {
            parish = (Parish) dwellingPlaceService.save(parish);
        }

        if (location != null) {
            location.addDwellingPlace(parish);
            if (parishParameters.isPersist()) {
                dwellingPlaceService.save(location);
            }
        }

        log.info(String.format("Created the Parish %s with expected population %d", parish.getName(), totalPopulation));

        long lastPopulation = largestTownPopulation(totalPopulation);
        currentPopulation += lastPopulation;
        List<TownTemplate> towns = new ArrayList<>();
        String name = randomTownName(parishParameters, 1);
        TownTemplate town1 = createTown(name, lastPopulation, parishParameters.isPersist());
        towns.add(town1);
        parish.addDwellingPlace(town1.getTown());

        int townIndex = 2;
        while (canAddAnotherTown(parishParameters, currentPopulation, lastPopulation)) {
            if (townIndex == 2) {
                lastPopulation = secondTownPopulation(lastPopulation);
            } else {
                lastPopulation = furtherTownPopulation(lastPopulation);
            }

            // Don't add this town if it would take the remaining population below 0
            if (lastPopulation > remainingRealmPopulation(parishParameters, currentPopulation)) {
                break;
            }

            currentPopulation += lastPopulation;

            name = randomTownName(parishParameters, townIndex++);
            TownTemplate town = createTown(name, lastPopulation, parishParameters.isPersist());
            parish.addDwellingPlace(town.getTown());
            towns.add(town);
        }

        long remainingPopulation = totalPopulation - currentPopulation;
        log.info(String.format(
                "%d people remain outside of towns, and their households will be added directly to the parish",
                remainingPopulation));

        ParishTemplate template = new ParishTemplate();
        template.setParish(parish);
        template.setTowns(towns);
        template.setExpectedTotalPopulation(totalPopulation);
        template.setExpectedRuralPopulation(remainingPopulation);
        template.setFamilyParameters(parishParameters.getFamilyParameters());

        ParishPopulator populator = new ParishPopulator(parishParameters, householdGenerator,
                familyGenerator,
                familyService,
                householdService,
                dwellingPlaceService,
                personService,
                householdDwellingPlaceService);
        populator.populateParish(template);

        return parish;
    }

    public void populateEstateWithEmployees(@NonNull Estate estate, @NonNull LocalDate onDate) {

        ParishParameters parishParameters = new ParishParameters();
        ParishPopulator populator = new ParishPopulator(parishParameters, householdGenerator,
                familyGenerator,
                familyService,
                householdService,
                dwellingPlaceService,
                personService,
                householdDwellingPlaceService);

        RandomFamilyParameters parameters = new RandomFamilyParameters();
        parameters.setReferenceDate(onDate);

        List<Occupation> domesticServants = occupationService.findByIsDomesticServant();
        List<Occupation> farmLaborers = occupationService.findByIsFarmLaborer();

        Household household;
        for (int i = 0; i < 5; i++) {
            household = populator.createHouseholdToFillOccupation(parameters, estate,
                    domesticServants.get(i % domesticServants.size()));
            if (household != null) {
                DwellingPlace currentPlace = household.getDwellingPlace(onDate);
                if (currentPlace != null && !currentPlace.isHouse()) {
                    householdDwellingPlaceService.moveHomelessHouseholdIntoHouse(household, onDate, onDate);
                }
            }
            household = populator.createHouseholdToFillOccupation(parameters, estate,
                    farmLaborers.get(i % farmLaborers.size()));
            if (household != null) {
                DwellingPlace currentPlace = household.getDwellingPlace(onDate);
                if (currentPlace != null && !currentPlace.isHouse()) {
                    householdDwellingPlaceService.moveHomelessHouseholdIntoHouse(household, onDate, onDate);
                }
            }
        }
    }

    private TownTemplate createTown(String name, long population, boolean persist) {
        Town town = new Town();
        town.setName(name);

        if (persist) {
            town = (Town) dwellingPlaceService.save(town);
        }

        Map<Occupation, Integer> expectedOccupations = new HashMap<>();
        List<Occupation> occupationList = occupationService.occupationsForTownPopulation(population);
        for (Occupation occupation : occupationList) {
            expectedOccupations.putIfAbsent(occupation, 0);
            expectedOccupations.put(occupation, expectedOccupations.get(occupation) + 1);
        }
        TownTemplate townTemplate = new TownTemplate(town, population, expectedOccupations);
        log.info(String.format("Created the town %s with expected population %d", town.getName(),
                population));

        return townTemplate;
    }


    /**
     * Determines how many people are not yet assigned to towns in the realm
     *
     * @param parishParameters so we can get the total population
     * @param currentPopulation its current population
     * @return the people remaining to assign
     */
    private long remainingRealmPopulation(@NonNull ParishParameters parishParameters, long currentPopulation) {
        return parishParameters.getPopulation() - currentPopulation;
    }

    /**
     * The largest town is equal to (P times M), where P is equal to the square root of the country's population,
     * and M is equal to a random roll of 2d4+10
     */
    private long largestTownPopulation(long totalPopulation) {
        double p = Math.sqrt(totalPopulation);
        int m = d4.roll(2) + 10;
        return Math.round(p * m);
    }

    /**
     * The second largest town is a random fraction of the size of the first
     * @param largestTownPopulation population of the largest town
     * @return a population
     */
    private long secondTownPopulation(long largestTownPopulation) {
        double percent = d4.roll(2) * 0.1;
        return Math.round(largestTownPopulation * percent);
    }

    /**
     * The population of a next smallest town given the previous town's population
     * @param lastTownPopulation population of the previous town generated
     * @return population of the next town
     */
    private long furtherTownPopulation(long lastTownPopulation) {
        double percent = d4.roll(2) * 0.05;
        long pop = Math.round(lastTownPopulation * percent);
        return pop == 0 ? 1 : pop;
    }

    /**
     * Checks to see whether there is enough remaining population to add another town
     *
     * @param parishParameters which includes the calculated total population
     * @param currentTownPopulation current population across all towns
     * @param previousTownPopulation the population of the last generated town
     * @return boolean if there is enough population left to allocate it to another town
     */
    private boolean canAddAnotherTown(@NonNull ParishParameters parishParameters,
                                      long currentTownPopulation,
                                      long previousTownPopulation) {
        return ((parishParameters.getPopulation() - currentTownPopulation) > parishParameters.getMinTownPopulation())
                && previousTownPopulation > parishParameters.getMinTownPopulation();
    }

    /**
     * Get a random town name from the list, if there is a list, otherwise make a name from the parish name if any,
     * otherwise just a placeholder from the index
     *
     * @param parishParameters the parameters which may contain a list of names or at least a parish name. If a random
     *                         name is chosen from the list, it will also remove it from the list so it won't be reused
     * @param townIndex the number of the town we are creating, in case we need to create a name like "Town 3"
     * @return a string name
     */
    @NonNull
    private String randomTownName(@NonNull ParishParameters parishParameters, int townIndex) {
        String name = parishParameters.getAndRemoveRandomTownName();
        if (name != null) {
            return name;
        }
        name = parishParameters.getParishName() != null
                ? parishParameters.getParishName() + " Town " + townIndex
                : "Town " + townIndex;
        return name;
    }

}
