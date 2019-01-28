package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.request.ParishParameters;
import com.meryt.demographics.request.RandomFamilyParameters;

@Getter
@Setter
class ParishTemplate {

    private final ParishParameters parishParameters;
    private Parish parish;
    private List<TownTemplate> towns = new ArrayList<>();
    private long expectedTotalPopulation;
    private long expectedRuralPopulation;
    private RandomFamilyParameters familyParameters;

    ParishTemplate(@NonNull ParishParameters parishParameters) {
        this.parishParameters = parishParameters;
    }

    /**
     * Determines whether the town population on the given date is less than the expected population
     * @param onDate the date to check the town's population
     * @return true if actual population is less than expected population
     */
    boolean hasRuralPopulationRemaining(@NonNull LocalDate onDate) {
        return getParish().getDirectPopulation(onDate) < getExpectedRuralPopulation();
    }

}
