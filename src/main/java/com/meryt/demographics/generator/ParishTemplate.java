package com.meryt.demographics.generator;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
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
}
