package com.meryt.demographics.domain.place;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * An administrative unit of some sort
 */
public class Realm implements DwellingPlace {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private double areaSquareMiles;

    @Getter
    private final List<DwellingPlace> dwellingPlaces = new ArrayList<>();

    public long getPopulation() {
        return dwellingPlaces.stream().mapToLong(DwellingPlace::getPopulation).sum();
    }
}
