package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParishParameters {

    private double squareMiles = 100.0;
    private long populationPerSquareMile = 40;
    private long minTownPopulation = 50;
    private boolean persist;
    private FamilyParameters familyParameters;

    public long getPopulation() {
        return Math.round(squareMiles * populationPerSquareMile);
    }
}
