package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.place.Town;

@Getter
@Setter
@AllArgsConstructor
class TownTemplate {

    private final Town town;
    private final long expectedPopulation;
    private final Map<Occupation, Integer> expectedOccupations;

    /**
     * Determines whether the town population on the given date is less than the expected population
     * @param onDate the date to check the town's population
     * @return true if actual population is less than expected population
     */
    boolean hasSpaceRemaining(@NonNull LocalDate onDate) {
        return getTown().getPopulation(onDate) < getExpectedPopulation();
    }

}
