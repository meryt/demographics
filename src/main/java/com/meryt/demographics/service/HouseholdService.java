package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdInhabitantPeriod;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.repository.HouseholdInhabitantRepository;
import com.meryt.demographics.repository.HouseholdLocationRepository;
import com.meryt.demographics.repository.HouseholdRepository;
import com.meryt.demographics.time.LocalDateComparator;

@Service
public class HouseholdService {

    private static final int MIN_HEAD_OF_HOUSEHOLD_AGE = 16;

    private final HouseholdRepository householdRepository;
    private final HouseholdInhabitantRepository householdInhabitantRepository;
    private final HouseholdLocationRepository householdLocationRepository;

    public HouseholdService(@Autowired @NonNull HouseholdRepository householdRepository,
                            @Autowired @NonNull HouseholdInhabitantRepository householdInhabitantRepository,
                            @Autowired @NonNull HouseholdLocationRepository householdLocationRepository) {
        this.householdRepository = householdRepository;
        this.householdInhabitantRepository = householdInhabitantRepository;
        this.householdLocationRepository = householdLocationRepository;
    }

    public Household save(@NonNull Household household) {
        return householdRepository.save(household);
    }

    public HouseholdInhabitantPeriod save(@NonNull HouseholdInhabitantPeriod householdInhabitantPeriod) {
        return householdInhabitantRepository.save(householdInhabitantPeriod);
    }

    /**
     * Finds a household by ID or returns null if none found
     */
    @Nullable
    public Household load(long householdId) {
        return householdRepository.findById(householdId).orElse(null);
    }

    public void delete(@NonNull HouseholdInhabitantPeriod householdInhabitantPeriod) {
        householdInhabitantRepository.delete(householdInhabitantPeriod);
    }

    /**
     * Finds all households that are not in any location.
     *
     * @param onDate the date to use for the search
     * @return a list of 0 or more households that are not in any type of location
     */
    public List<Household> loadHouseholdsWithoutLocations(@NonNull LocalDate onDate) {
        return householdRepository.findHouseholdsWithoutLocations(onDate);
    }

    /**
     * Finds all households that are in a location, but are not in a location of type DWELLING. Does not find
     * "floating" households that are not in any location.
     *
     * @param onDate the date to use for the search
     * @return a list of 0 or more households that are not in a location of type DWELLING
     */
    public List<Household> loadHouseholdsWithoutHouses(@NonNull LocalDate onDate) {
        return householdRepository.findHouseholdsWithoutHouses(onDate);
    }

    /**
     * Use this method to find and set a new head of household as of the given date. Normally used when the current
     * head of the household dies. Uses the oldest male over 16, and failing that, the oldest female over 16. If no one
     * is eligible, does nothing.
     *
     * The method attempts to set the start of the headship from the end of the previous headship, if it does not
     * happen to equal to the onDate parameter, or from the start of the residency of the new head, if that date
     * falls after the headship of the previous head.
     *
     * @param household the household whose head needs to be reset
     * @param onDate the date the previous head died or left
     */
    public void resetHeadAsOf(@NonNull Household household, @NonNull LocalDate onDate) {
        List<Person> inhabitantsByAge = household.getInhabitants(onDate).stream()
                .filter(p -> p.getBirthDate() != null && p.getAgeInYears(onDate) >= MIN_HEAD_OF_HOUSEHOLD_AGE)
                .sorted(Comparator.comparing(Person::getGender).thenComparing(Person::getBirthDate))
                .collect(Collectors.toList());
        if (inhabitantsByAge.isEmpty()) {
            return;
        }

        if (household.getHead(onDate) != null) {
            // should never happen...
            endPersonResidence(household, household.getHead(onDate), onDate);
        }

        HouseholdInhabitantPeriod lastHeadsPeriod = household.getInhabitantPeriods().stream()
                .filter(HouseholdInhabitantPeriod::isHouseholdHead)
                .filter(hip -> hip.getToDate() != null && hip.getToDate().isBefore(onDate))
                .max(Comparator.comparing(HouseholdInhabitantPeriod::getToDate).reversed())
                .orElse(null);

        Person newHead = inhabitantsByAge.get(0);

        HouseholdInhabitantPeriod newHeadsPeriod = household.getInhabitantPeriods().stream()
                .filter(hip -> hip.getPerson().equals(newHead) && hip.contains(onDate))
                .findFirst().orElse(null);

        LocalDate startOfHeadship;
        if (newHeadsPeriod == null) {
            // Should never happen, but...
            if (lastHeadsPeriod == null || lastHeadsPeriod.getToDate() == null) {
                startOfHeadship = onDate;
            } else {
                startOfHeadship = LocalDateComparator.min(lastHeadsPeriod.getToDate(), onDate);
            }
        } else {
            if (lastHeadsPeriod == null || lastHeadsPeriod.getToDate() == null) {
                startOfHeadship = onDate;
            } else {
                startOfHeadship = LocalDateComparator.max(newHeadsPeriod.getFromDate(), lastHeadsPeriod.getToDate());
            }
        }

        endPersonResidence(household, newHead, startOfHeadship);

        addPersonToHousehold(newHead, household, startOfHeadship, true);
        save(household);
    }

    public void addPersonToHousehold(@NonNull Person person,
                                     @NonNull Household household,
                                     @NonNull LocalDate fromDate,
                                     boolean isHead) {

        HouseholdInhabitantPeriod newPeriod = new HouseholdInhabitantPeriod();
        newPeriod.setFromDate(fromDate);
        newPeriod.setToDate(person.getDeathDate());

        for (HouseholdInhabitantPeriod period : person.getHouseholds()) {
            if (period.rangeEquals(newPeriod)) {
                endPersonResidence(period.getHousehold(), person, fromDate);
                // There is an existing exact match for this new open-ended range. Just move the household.
                period.setHousehold(household);
                period.setHouseholdHead(isHead);
                return;
            } else if (period.getFromDate().isBefore(fromDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(fromDate))) {
                period.setToDate(fromDate);
                endPersonResidence(period.getHousehold(), person, fromDate);
            }
        }

        newPeriod.setHousehold(household);

        household.addInhabitantPeriod(newPeriod);

        newPeriod.setPerson(person);
        newPeriod.setPersonId(person.getId());
        newPeriod.setHouseholdHead(isHead);
        person.getHouseholds().add(newPeriod);
    }

    /**
     * Tell this household that the person has moved. Cap his current residency period (if any)
     * @param person   the person
     * @param asOfDate the date upon which the person moved to another household
     */
    public void endPersonResidence(@NonNull Household household,
                                   @NonNull Person person,
                                   @NonNull LocalDate asOfDate) {
        for (HouseholdInhabitantPeriod period : household.getInhabitantPeriods()) {
            if (period.getPersonId() == person.getId() && period.getFromDate().equals(asOfDate)) {
                household.getInhabitantPeriods().remove(period);
                person.getHouseholds().remove(period);
                delete(period);
                return;
            }
            if (period.getPersonId() == person.getId() &&
                    period.getFromDate().isBefore(asOfDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(asOfDate))) {

                period.setToDate(asOfDate);
                save(period);
            }
        }
    }

    public void addToDwellingPlace(@NonNull Household household,
                                   @NonNull DwellingPlace dwellingPlace,
                                   @NonNull LocalDate fromDate,
                                   LocalDate toDate) {
        List<HouseholdLocationPeriod> periodsToDelete = new ArrayList<>();
        for (HouseholdLocationPeriod period : household.getDwellingPlaces()) {
            if (period.getFromDate().isBefore(fromDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(fromDate))) {
                period.setToDate(fromDate);
                householdLocationRepository.save(period);
            }  else if (period.getFromDate().equals(fromDate) && (
                    (period.getToDate() == null && toDate == null)
                            || (period.getToDate().equals(toDate)))) {
                // If the periods are identical, just change the dwelling place.
                period.setDwellingPlace(dwellingPlace);
                householdLocationRepository.save(period);
                return;
            } else if (toDate == null && period.getFromDate().isAfter(fromDate)) {
                // If this is an open-ended date range, we should delete any future locations for this household
                periodsToDelete.add(period);
            }
        }

        for (HouseholdLocationPeriod periodToDelete : periodsToDelete) {
            household.getDwellingPlaces().remove(periodToDelete);
            dwellingPlace.getHouseholdPeriods().remove(periodToDelete);
            householdLocationRepository.delete(periodToDelete);
        }

        HouseholdLocationPeriod newPeriod = new HouseholdLocationPeriod();
        newPeriod.setHouseholdId(household.getId());
        newPeriod.setHousehold(household);
        newPeriod.setDwellingPlace(dwellingPlace);
        newPeriod.setFromDate(fromDate);
        newPeriod.setToDate(toDate);
        household.getDwellingPlaces().add(newPeriod);

        newPeriod = householdLocationRepository.save(newPeriod);

        dwellingPlace.getHouseholdPeriods().add(newPeriod);
    }

}
