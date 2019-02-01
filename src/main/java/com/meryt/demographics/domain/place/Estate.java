package com.meryt.demographics.domain.place;

import java.util.Comparator;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "ESTATE")
public class Estate extends DwellingPlace {

    private static final int DEFAULT_MIN_NUM_SERVANTS = 3;

    public Estate() {
        super();
        setType(DwellingPlaceType.ESTATE);
    }

    /**
     * Gets the number of farm laborers this estate is expected to have. Returns the value of the estate divided by
     * 10_000, or DEFAULT_NUM_SERVANTS if there is no value
     */
    public int getExpectedNumFarmLaborerHouseholds() {
        return Math.max(DEFAULT_MIN_NUM_SERVANTS, (int) Math.round(getNullSafeValue() / 10_000));
    }

    /**
     * Gets the manor house for this estate. The manor house is the most expensive dwelling that is attached to the
     * estate.
     * @return a Dwelling or null if there is no house attached to this estate
     */
    public Dwelling getManorHouse() {
        return (Dwelling) getDwellingPlaces().stream()
                .filter(p -> p.isHouse() && p.isAttachedToParent())
                .max(Comparator.comparing(DwellingPlace::getNullSafeValue))
                .orElse(null);
    }

}
