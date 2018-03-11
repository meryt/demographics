package com.meryt.demographics.generator;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
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

}
