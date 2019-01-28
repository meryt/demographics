package com.meryt.demographics.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.place.Region;

@Getter
@Setter
public class ParishParameters {

    private double squareMiles = 100.0;
    private long populationPerSquareMile = 40;
    private long minTownPopulation = 50;
    private Long locationId;
    private Region location;
    private String parishName;
    private PlaceNameParameters placeNames;
    private RandomFamilyParameters familyParameters;
    private int maxEstates = 2;
    @JsonIgnore
    private volatile int currentEstates = 0;

    public long getPopulation() {
        return Math.round(squareMiles * populationPerSquareMile);
    }

    @JsonIgnore
    public int getRemainingEstates() {
        return maxEstates - currentEstates;
    }
}
