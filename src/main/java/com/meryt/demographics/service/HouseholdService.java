package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.repository.HouseholdRepository;

@Service
public class HouseholdService {

    private final HouseholdRepository householdRepository;

    public HouseholdService(@Autowired @NonNull HouseholdRepository householdRepository) {
        this.householdRepository = householdRepository;
    }

    public Household save(@NonNull Household household) {
        return householdRepository.save(household);
    }

    /**
     * Finds a household by ID or returns null if none found
     */
    @Nullable
    public Household load(long householdId) {
        return householdRepository.findById(householdId).orElse(null);
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

}
