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
     * @return a chance between about 0.0 and 1.0
     */
    public double getChanceOfEmigrating(@NonNull LocalDate onDate) {
        return 0.000003 * Math.pow((getPopulationPerSquareMile(onDate) - DESIRED_POPULATION_PER_SQUARE_MILE), 3);
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
