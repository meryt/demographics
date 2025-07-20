package com.meryt.demographics.domain.person.fertility;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "paternities")
public class Paternity extends Fertility {

    public double getAdjustedFertilityFactor(long ageInDays) {
        return getFertilityFactor() * getAgeAdjustedFertilityFactor(ageInDays);
    }

    private double getAgeAdjustedFertilityFactor(long ageInDays) {
        // Use approximate age in years since it doesn't have to be perfect
        // and getting true age in years is expensive.
        long ageInYears = ageInDays / 365;

        if (ageInYears < 40) {
            return 1;
        } else if (ageInYears >= 95) {
            return 0;
        } else {
            // factor varies linearly with age
            return (95 - ageInYears) / 55.0;
        }
    }

}
