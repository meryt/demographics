package com.meryt.demographics.domain.place;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.thymeleaf.util.StringUtils;

public enum DwellingPlaceType {
    DWELLING(null),
    FARM(setOf(DWELLING)),
    STREET(setOf(DWELLING, FARM)),
    ESTATE(setOf(DWELLING, STREET, FARM)),
    TOWN(setOf(DWELLING, FARM, STREET, ESTATE)),
    TOWNSHIP(setOf(DWELLING, FARM, STREET, ESTATE, TOWN)),
    PARISH(setOf(DWELLING, FARM, STREET, ESTATE, TOWN)),
    REGION(setOf(DWELLING, FARM, STREET, ESTATE, TOWN, TOWNSHIP, PARISH));

    private Set<DwellingPlaceType> canContain;

    DwellingPlaceType(Set<DwellingPlaceType> canContain) {
        this.canContain = canContain;
    }

    private static Set<DwellingPlaceType> setOf(DwellingPlaceType... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    static {
        for (DwellingPlaceType t : values()) {
            if (t.canContain == null) {
                t.canContain = EnumSet.noneOf(DwellingPlaceType.class);
            } else {
                t.canContain = EnumSet.copyOf(t.canContain);
            }
        }
    }

    /**
     * Determines whether a dwelling place of this type can contain this other type of dwelling place. For example,
     * a TOWN may contain a STREET but a STREET may not contain a TOWN.
     * 
     * A REGION can contain other REGIONs. (It's not possible to enable this in the enum constructor.)
     */
    public boolean canContain(DwellingPlaceType other) {
        return canContain.contains(other) || (other == REGION && this == REGION);
    }

    public String getFriendlyName() {
        return StringUtils.capitalize(name().toLowerCase());
    }
}
