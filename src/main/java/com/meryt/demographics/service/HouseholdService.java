package com.meryt.demographics.service;

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

}
