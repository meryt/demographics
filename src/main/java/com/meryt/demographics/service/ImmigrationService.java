package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.generator.family.HouseholdGenerator;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.response.calendar.EmploymentEvent;
import com.meryt.demographics.response.calendar.ImmigrationEvent;
import com.meryt.demographics.response.calendar.NewHouseEvent;
import com.meryt.demographics.response.calendar.PropertyTransferEvent;

/**
 * Helper service for handling immigration events.
 */
@Service
@Slf4j
public class ImmigrationService {


    private final HouseholdGenerator householdGenerator;
    private final HouseholdService householdService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;
    private final PersonService personService;
    private final OccupationService occupationService;
    private final DwellingPlaceService dwellingPlaceService;

    public ImmigrationService(@Autowired @NonNull HouseholdService householdService,
                              @Autowired @NonNull HouseholdDwellingPlaceService householdDwellingPlaceService,
                              @Autowired @NonNull HouseholdGenerator householdGenerator,
                              @Autowired @NonNull PersonService personService,
                              @Autowired @NonNull OccupationService occupationService,
                              @Autowired @NonNull DwellingPlaceService dwellingPlaceService) {

        this.householdService = householdService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.householdGenerator = householdGenerator;
        this.personService = personService;
        this.occupationService = occupationService;
        this.dwellingPlaceService = dwellingPlaceService;
    }

    /**
     * Process the arrival of an immigrant family from the calendar service.
     *
     * @param parish the parish where they are moving
     * @param requestFamilyParameters the family parameters that were part of the calendar request. Some values will
     *                                be copied, others will be customized for the purpose of this method. This object
     *                                will not be modified.
     * @param date the date they arrive
     * @return a list of events generated while processing this arrival
     */
    List<CalendarDayEvent> processImmigrantArrival(@NonNull Parish parish,
                                                   @NonNull final RandomFamilyParameters requestFamilyParameters,
                                                   @NonNull LocalDate date) {
        List<CalendarDayEvent> dayResults = new ArrayList<>();

        // Generate a new family.
        RandomFamilyParameters familyParameters = new RandomFamilyParameters(requestFamilyParameters);
        familyParameters.setChanceGeneratedSpouse(0.0);
        familyParameters.setCycleToDeath(false);
        familyParameters.setReferenceDate(date);
        familyParameters.setPersist(true);
        familyParameters.setAllowMaternalDeath(true);
        familyParameters.setPercentMaleFounders(1.0);

        Household household = householdGenerator.generateHousehold(familyParameters);
        Person man = household.getHead(date);
        if (man == null) {
            // Should never happen, since the chance of a male founder is 100% and the generateHousehold method
            // ensures the founder is alive on the specified date.
            log.warn(String.format("No male head of immigrant household as of %s; skipping", date));
            return dayResults;
        }
        dayResults.add(new ImmigrationEvent(date, household));

        personService.generateStartingCapitalForFounder(man, date);
        double capital = man.getCapital(date);

        // Optionally add an occupation.
        Occupation occupation = occupationService.findAvailableOccupationForPerson(man, parish, date);
        if (occupation != null) {
            man.addOccupation(occupation, date);
            personService.save(man);
            dayResults.add(new EmploymentEvent(date, man, occupation));
        }

        List<DwellingPlace> buyableHouses = parish.getRecursiveDwellingPlaces(DwellingPlaceType.DWELLING).stream()
                .filter(h -> h.getAllResidents(date).isEmpty() && h.getNullSafeValue() < capital)
                .sorted(Comparator.comparing(DwellingPlace::getNullSafeValue).reversed())
                .collect(Collectors.toList());
        if (buyableHouses.isEmpty()) {
            // Find a dwelling place in the parish. May purchase if there is an empty one, or build a new
            // house, or...?
            List<DwellingPlace> towns = new ArrayList<>(parish.getRecursiveDwellingPlaces(
                    DwellingPlaceType.TOWN));
            DwellingPlace townOrParish;
            if (towns.isEmpty()) {
                townOrParish = parish;
            } else {
                int whichTown = new BetweenDie().roll(1, towns.size() + 1);
                if (whichTown > towns.size()) {
                    // add ot parish
                    townOrParish = parish;
                } else {
                    Collections.shuffle(towns);
                    townOrParish = towns.get(0);
                }
            }
            // Either build a house or move into an existing household depending on the social class
            householdService.addToDwellingPlace(household, townOrParish, date, null);
            DwellingPlace newHouse = householdDwellingPlaceService.moveHomelessHouseholdIntoHouse(household,
                    date, date);
            if (newHouse != null) {
                List<Person> owners = newHouse.getOwners(date);
                if (newHouse instanceof Dwelling && owners != null && owners.contains(man)) {
                    dayResults.add(new NewHouseEvent(date, newHouse));
                }
            }
        } else {
            DwellingPlace house = buyableHouses.get(0);
            dwellingPlaceService.buyDwellingPlace(house, man, date);
            householdService.addToDwellingPlace(household, house, date, null);
            dayResults.add(new PropertyTransferEvent(date, house, house.getOwners(date.minusDays(1)),
                    house.getOwners(date)));
        }
        return dayResults;
    }

}
