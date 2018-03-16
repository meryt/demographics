package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.request.FamilyParameters;

@Getter
@Setter
class ParishTemplate {

    private Parish parish;
    private List<TownTemplate> towns = new ArrayList<>();
    private long expectedTotalPopulation;
    private long expectedRuralPopulation;
    private FamilyParameters familyParameters;

    /**
     * Determines whether the town population on the given date is less than the expected population
     * @param onDate the date to check the town's population
     * @return true if actual population is less than expected population
     */
    public boolean hasRuralPopulationRemaining(@NonNull LocalDate onDate) {
        return getParish().getDirectPopulation(onDate) < getExpectedRuralPopulation();
    }

}
