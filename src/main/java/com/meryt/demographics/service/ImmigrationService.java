package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.generator.family.HouseholdGenerator;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.response.calendar.EmploymentEvent;
import com.meryt.demographics.response.calendar.ImmigrationEvent;

/**
 * Helper service for handling immigration events.
 */
@Service
@Slf4j
public class ImmigrationService {


    private final HouseholdGenerator householdGenerator;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;
    private final PersonService personService;
    private final OccupationService occupationService;

    public ImmigrationService(@Autowired @NonNull HouseholdDwellingPlaceService householdDwellingPlaceService,
                              @Autowired @NonNull HouseholdGenerator householdGenerator,
                              @Autowired @NonNull PersonService personService,
                              @Autowired @NonNull OccupationService occupationService) {

        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.householdGenerator = householdGenerator;
        this.personService = personService;
        this.occupationService = occupationService;
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

        // Optionally add an occupation.
        Occupation occupation = occupationService.findAvailableOccupationForPerson(man, parish, date);
        if (occupation != null) {
            man.addOccupation(occupation, date);
            personService.save(man);
            dayResults.add(new EmploymentEvent(date, man, occupation));
        }

        List<CalendarDayEvent> results = householdDwellingPlaceService.buyOrCreateOrMoveIntoEmptyHouse(
                parish, household, date, DwellingPlaceOwnerPeriod.ReasonToPurchase.MOVE_TO_PARISH);

        dayResults.addAll(results);

        return dayResults;
    }

}
