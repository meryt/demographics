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
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.place.Region;
import com.meryt.demographics.domain.place.Town;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.request.ParishParameters;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.OccupationService;
import com.meryt.demographics.service.TownTemplateService;

import static com.meryt.demographics.domain.place.DwellingPlace.ACRES_PER_SQUARE_MILE;

@Service
@Slf4j
public class ParishGenerator {

    private final Die d4 = new Die(4);

    private final OccupationService occupationService;
    private final DwellingPlaceService dwellingPlaceService;
    private final TownTemplateService townTemplateService;
    private final ParishPopulator parishPopulator;

    public ParishGenerator(@Autowired @NonNull OccupationService occupationService,
                           @Autowired @NonNull DwellingPlaceService dwellingPlaceService,
                           @Autowired @NonNull TownTemplateService townTemplateService,
                           @Autowired @NonNull ParishPopulator parishPopulator) {
        this.occupationService = occupationService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.townTemplateService = townTemplateService;
        this.parishPopulator = parishPopulator;
    }

    /**
     * Generates a parish with its towns, households, and inhabitants.
     */
    public Parish generateParish(@NonNull ParishParameters parishParameters) {
        validateParameters(parishParameters);

        DwellingPlace location = loadOrCreateLocation(parishParameters);

        Parish parish = createParish(parishParameters, location);

        long totalPopulation = parishParameters.getPopulation();
        long currentPopulation = 0;

        log.info(String.format("Created the Parish %s with expected population %d", parish.getName(), totalPopulation));

        long lastPopulation = largestTownPopulation(totalPopulation);
        currentPopulation += lastPopulation;
        List<TownTemplate> towns = new ArrayList<>();
        String name = randomTownName(parishParameters, 1);
        TownTemplate town1 = createTown(name, lastPopulation, parishParameters.getFamilyParameters().getReferenceDate(),
                location);
        towns.add(town1);
        parish.addDwellingPlace(town1.getTown());

        int townIndex = 2;
        while (canAddAnotherTown(parishParameters, currentPopulation, lastPopulation)) {
            if (townIndex == 2) {
                lastPopulation = secondTownPopulation(lastPopulation);
            } else {
                lastPopulation = furtherTownPopulation(lastPopulation);
            }

            // Don't add this town if it would take the remaining population below 0, or if it's too small to be a town
            if (lastPopulation > remainingRealmPopulation(parishParameters, currentPopulation) ||
                    lastPopulation < parishParameters.getMinTownPopulation()) {
                break;
            }

            currentPopulation += lastPopulation;

            name = randomTownName(parishParameters, townIndex++);
            TownTemplate town = createTown(name, lastPopulation,
                    parishParameters.getFamilyParameters().getReferenceDate(), location);
            parish.addDwellingPlace(town.getTown());
            towns.add(town);
        }

        long remainingPopulation = totalPopulation - currentPopulation;
        log.info(String.format(
                "%d people remain outside of towns, and their households will be added directly to the parish",
                remainingPopulation));

        // By this point an empty parish and some empty towns have been created. Set up the ParishTemplate object
        // so that population can now be generated.
        ParishTemplate template = new ParishTemplate(parishParameters);
        template.setParish(parish);
        template.setTowns(towns);
        template.setExpectedTotalPopulation(totalPopulation);
        template.setExpectedRuralPopulation(remainingPopulation);
        template.setFamilyParameters(parishParameters.getFamilyParameters());

        parishPopulator.populateParish(template);

        return parish;
    }

    /**
     * Create and save the parish. At this point no towns or inhabitants have been created.
     */
    private Parish createParish(@NonNull ParishParameters parishParameters, @NonNull DwellingPlace location) {
        Parish parish = new Parish();
        parish.setFoundedDate(parishParameters.getFamilyParameters().getReferenceDate());
        parish.setAcres(parishParameters.getSquareMiles() * ACRES_PER_SQUARE_MILE);
        parish.setName(parishParameters.getParishName());

        parish = (Parish) dwellingPlaceService.save(parish);

        if (location != null) {
            location.addDwellingPlace(parish);
            dwellingPlaceService.save(location);
        }
        return parish;
    }

    /**
     * Creates a Town according to the given parameters. Does not create any dwellings or inhabitants, but returns the
     * Town embedded in a TownTemplate object that includes info about population and expected occupations.
     *
     * @param name desired name for the town
     * @param population desired population for the town
     * @param foundedDate the date the town was founded
     * @param location the location of the town (normally a Parish)
     * @return the TownTemplate including the town and the desired population and occupations
     */
    private TownTemplate createTown(String name,
                                    long population,
                                    @NonNull LocalDate foundedDate,
                                    @NonNull DwellingPlace location) {
        Town town = new Town();
        town.setName(name);
        town.setFoundedDate(foundedDate);
        if (location instanceof Region) {
            town.setMapId(townTemplateService.getUnusedMapId((Region) location));
        }

        town = (Town) dwellingPlaceService.save(town);

        Map<Occupation, Integer> expectedOccupations = new HashMap<>();
        List<Occupation> occupationList = occupationService.occupationsForTownPopulation(population);
        for (Occupation occupation : occupationList) {
            expectedOccupations.putIfAbsent(occupation, 0);
            expectedOccupations.put(occupation, expectedOccupations.get(occupation) + 1);
        }
        TownTemplate townTemplate = new TownTemplate(town, population, expectedOccupations);
        log.info(String.format("Created the town %s with map ID %s and expected population %d", town.getName(),
                town.getMapId(), population));

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
        if (totalPopulation > 1000) {
            double p = Math.sqrt(totalPopulation);
            int m = d4.roll(2) + 10;
            return Math.round(p * m);
        } else {
            return Math.round(0.5 * totalPopulation);
        }
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
        String name = parishParameters.getPlaceNames().getAndRemoveRandomTownName();
        if (name != null) {
            return name;
        }
        name = parishParameters.getParishName() != null
                ? parishParameters.getParishName() + " Town " + townIndex
                : "Town " + townIndex;
        return name;
    }

    /**
     * Given the parish parameters with the location specified, either load or create the location (e.g. a Region
     * like Scotland or England)
     */
    private DwellingPlace loadOrCreateLocation(@NonNull ParishParameters parishParameters) {
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
            if (location.getId() <= 0) {
                location = dwellingPlaceService.save(location);
            } else {
                throw new IllegalArgumentException("Cannot create parish: to create a new Region to contain this " +
                        "parish, use the locationId parameter rather than a location object with a non-null ID.");
            }
        }
        return location;
    }

    private void validateParameters(@NonNull ParishParameters parishParameters) {
        if (parishParameters.getFamilyParameters() == null) {
            throw new IllegalArgumentException("RandomFamilyParameters are required when creating a parish");
        }
        if (parishParameters.getFamilyParameters().getReferenceDate() == null) {
            throw new IllegalArgumentException("Reference date is required when creating a parish");
        }

        // Ensure that the parish parameters and family parameters have same setting for persist
        parishParameters.getFamilyParameters().setPersist(true);

        if (parishParameters.getParishName() == null) {
            parishParameters.setParishName("Parish");
        }
    }

}
