package com.meryt.demographics.request;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RealmParameters {

    private double squareMiles = 100.0;
    private long populationPerSquareMile = 40;
    private long minTownPopulation = 50;
    private LocalDate referenceDate;
    private boolean persist;

    public long getPopulation() {
        return Math.round(squareMiles * populationPerSquareMile);
    }
}
