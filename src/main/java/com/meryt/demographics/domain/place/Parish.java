package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "PARISH")
public class Parish extends DwellingPlace {
    private static final double SETTLED_SQUARE_MILE_POPULATION_SUPPORTED = 180.0;
    private static final double DESIRED_POPULATION_PER_SQUARE_MILE = 30.0;

    public Parish() {
        super();
        setType(DwellingPlaceType.PARISH);
    }

    public double getPopulationPerSquareMile(@NonNull LocalDate onDate) {
        if (getSquareMiles() == null || getSquareMiles() == 0) {
            return 0;
        }
        return getPopulation(onDate) / getSquareMiles();
    }

    /**
     * Calculate the chance of a newly married couple emigrating (depends on population density)
     *
     * @return a chance between about 0.0 and 1.0 (or greater than 1.0 for extremely high densities)
     */
    public double getChanceOfEmigrating(@NonNull LocalDate onDate) {
        double x = getPopulationPerSquareMile(onDate);

        // Tweak x so that we allow greater population density as time goes by
        int numYears = 1900 - onDate.getYear();
        if (numYears < 0) {
            numYears = 0;
        }
        double factor = numYears / (1900 - 1280);
        x = x - (30 * (1 - factor));

        // https://mycurvefit.com/
        /*
                  0                0
                 20                0
                 30                0
                 40                0.85
                 50                0.95
                 60                1
                 35                0.5
         */

        // y = 0.963058 + (-0.0160843 - 0.963058)/(1 + (x/34.94572)^18.18451)
        double value = 0.963058 + (-0.0160843 - 0.963058) / (1 + Math.pow(x /34.94572, 18.18451));
        return value;
    }

    /**
     * Gets the population of the parish that does not live in any town.
     */
    public long getRuralPopulation(@NonNull LocalDate onDate) {
        long recurisvePopWithoutTowns = getDwellingPlaces().stream()
                .filter(dp -> dp.getType() != DwellingPlaceType.TOWN)
                .mapToLong(d -> d.getPopulation(onDate))
                .sum();
        return recurisvePopWithoutTowns + getDirectPopulation(onDate);
    }

    public Double getSettledSquareMiles(@NonNull LocalDate onDate) {
        if (getSquareMiles() == null) {
            return null;
        }

        long totalPopulation = getPopulation(onDate);
        return totalPopulation / SETTLED_SQUARE_MILE_POPULATION_SUPPORTED;
    }

    public Double getSettledAcres(@NonNull LocalDate onDate) {
        Double settledSquareMiles = getSettledSquareMiles(onDate);
        return settledSquareMiles == null ? null : settledSquareMiles * ACRES_PER_SQUARE_MILE;
    }
}
