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

    public Parish() {
        super();
        setType(DwellingPlaceType.PARISH);
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
