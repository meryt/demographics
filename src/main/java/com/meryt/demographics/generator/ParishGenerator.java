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
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.request.RealmParameters;
import com.meryt.demographics.service.OccupationService;

@Slf4j
public class ParishGenerator {

    private static final double ACRES_PER_SQUARE_MILE = 640;

    private final Die d4 = new Die(4);

    private final OccupationService occupationService;

    public ParishGenerator(@NonNull OccupationService occupationService) {
        this.occupationService = occupationService;
    }

    /**
     * Generates a parish with its towns, households, and inhabitants. Does not save.
     */
    public Parish generateParish(@NonNull RealmParameters realmParameters) {
        if (realmParameters.getReferenceDate() == null) {
            throw new IllegalArgumentException("Reference date is required when creating a parish");
        }

        Parish parish = new Parish();
        parish.setAcres(realmParameters.getSquareMiles() * ACRES_PER_SQUARE_MILE);
        parish.setName("Parish 1");

        long totalPopulation = realmParameters.getPopulation();
        long currentPopulation = 0;

        log.info(String.format("Created the Parish %s with expected population %d", parish.getName(), totalPopulation));

        long lastPopulation = largestTownPopulation(totalPopulation);
        currentPopulation += lastPopulation;
        List<TownTemplate> towns = new ArrayList<>();
        TownTemplate town1 = createTown("Town 1", lastPopulation);
        towns.add(town1);
        parish.addDwellingPlace(town1.getTown());

        int townIndex = 2;
        while (canAddAnotherTown(realmParameters, currentPopulation, lastPopulation)) {
            if (townIndex == 2) {
                lastPopulation = secondTownPopulation(lastPopulation);
            } else {
                lastPopulation = furtherTownPopulation(lastPopulation);
            }

            // Don't add this town if it would take the remaining population below 0
            if (lastPopulation > remainingRealmPopulation(realmParameters, currentPopulation)) {
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

        ParishPopulator populator = new ParishPopulator();
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
     * @param realmParameters so we can get the total population
     * @param currentPopulation its current population
     * @return the people remaining to assign
     */
    private long remainingRealmPopulation(@NonNull RealmParameters realmParameters, long currentPopulation) {
        return realmParameters.getPopulation() - currentPopulation;
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
     * @param realmParameters which includes the calcuated total population
     * @param currentTownPopulation current population across all towns
     * @param previousTownPopulation the population of the last generated town
     * @return boolean if there is enough population left to allocate it to another town
     */
    private boolean canAddAnotherTown(@NonNull RealmParameters realmParameters,
                                      long currentTownPopulation,
                                      long previousTownPopulation) {
        return ((realmParameters.getPopulation() - currentTownPopulation) > realmParameters.getMinTownPopulation())
                && previousTownPopulation > realmParameters.getMinTownPopulation();
    }

}
