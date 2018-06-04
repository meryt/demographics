package com.meryt.demographics.response;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.math3.util.Precision;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Parish;

@Getter
class DwellingPlaceSummaryResponse extends DwellingPlaceReference {
    private Long directPopulation;
    private Long totalPopulation;

    private Double settledAcres;
    private Double settledSquareMiles;

    DwellingPlaceSummaryResponse(@NonNull DwellingPlace dwellingPlace, @Nullable LocalDate onDate) {
        super(dwellingPlace);

        if (onDate != null) {
            long pop = dwellingPlace.getPopulation(onDate);
            long directPop = dwellingPlace.getDirectPopulation(onDate);
            totalPopulation = pop == 0 ? null : pop;
            directPopulation = directPop == 0 ? null : directPop;

            if (dwellingPlace instanceof Parish) {
                settledAcres = Precision.round(((Parish) dwellingPlace).getSettledAcres(onDate), 1);
                settledSquareMiles = Precision.round(((Parish) dwellingPlace).getSettledSquareMiles(onDate), 1);
            }
         }
    }
}
