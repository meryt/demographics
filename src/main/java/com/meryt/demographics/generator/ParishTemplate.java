package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.place.Parish;

@Getter
@Setter
class ParishTemplate {

    private Parish parish;
    private List<TownTemplate> towns = new ArrayList<>();
    private long expectedTotalPopulation;
    private long expectedRuralPopulation;
    private LocalDate referenceDate;
}
