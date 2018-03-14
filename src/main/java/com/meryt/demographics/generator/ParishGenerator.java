package com.meryt.demographics.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.place.Town;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.family.HouseholdGenerator;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.request.ParishParameters;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.HouseholdService;
import com.meryt.demographics.service.OccupationService;
import com.meryt.demographics.service.PersonService;

@Slf4j
public class ParishGenerator {

    private static final double ACRES_PER_SQUARE_MILE = 640;

    private final Die d4 = new Die(4);

    private final OccupationService occupationService;

    private final FamilyGenerator familyGenerator;

    private final FamilyService familyService;

    private final PersonService personService;

    private final HouseholdService householdService;

    public ParishGenerator(@NonNull OccupationService occupationService,
                           @NonNull FamilyGenerator familyGenerator,
                           @NonNull FamilyService familyService,
                           @NonNull PersonService personService,
                           @NonNull HouseholdService householdService) {
        this.occupationService = occupationService;
        this.familyGenerator = familyGenerator;
        this.householdService = householdService;
        this.familyService = familyService;
        this.personService = personService;
    }

    /**
     * Generates a parish with its towns, households, and inhabitants. Does not save.
     */
    public Parish generateParish(@NonNull ParishParameters parishParameters) {
        if (parishParameters.getFamilyParameters() == null) {
            throw new IllegalArgumentException("FamilyParameters are required when creating a parish");
        }
        if (parishParameters.getFamilyParameters().getReferenceDate() == null) {
            throw new IllegalArgumentException("Reference date is required when creating a parish");
        }
        // Ensure that the parish parameters and family parameters have same setting for persist
        parishParameters.getFamilyParameters().setPersist(parishParameters.isPersist());

        Parish parish = new Parish();
        parish.setAcres(parishParameters.getSquareMiles() * ACRES_PER_SQUARE_MILE);
        parish.setName("Parish 1");

        long totalPopulation = parishParameters.getPopulation();
        long currentPopulation = 0;

        log.info(String.format("Created the Parish %s with expected population %d", parish.getName(), totalPopulation));

        long lastPopulation = largestTownPopulation(totalPopulation);
        currentPopulation += lastPopulation;
        List<TownTemplate> towns = new ArrayList<>();
        TownTemplate town1 = createTown("Town 1", lastPopulation);
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

            TownTemplate town = createTown("Town " + townIndex++, lastPopulation);
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

        ParishPopulator populator = new ParishPopulator(new HouseholdGenerator(familyGenerator,
                personService, familyService, householdService));
        populator.populateParish(template);

        return parish;
    }

    private TownTemplate createTown(String name, long population) {
        Town town = new Town();
        town.setName(name);
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
     * @param parishParameters which includes the calcuated total population
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

}
