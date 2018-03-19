package com.meryt.demographics.response;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Household;

@Getter
class DwellingPlaceSummaryResponse extends DwellingPlaceReference {
    private Long directPopulation;
    private Long totalPopulation;

    private List<HouseholdSummaryResponse> leadingHouseholds;

    DwellingPlaceSummaryResponse(@NonNull DwellingPlace dwellingPlace) {
        this(dwellingPlace, null);
    }

    DwellingPlaceSummaryResponse(@NonNull DwellingPlace dwellingPlace, @Nullable LocalDate onDate) {
        super(dwellingPlace);

        if (onDate != null) {
            long pop = dwellingPlace.getPopulation(onDate);
            long directPop = dwellingPlace.getDirectPopulation(onDate);
            totalPopulation = pop == 0 ? null : pop;
            directPopulation = directPop == 0 ? null : directPop;

            List<Household> leadingHouseholdList = dwellingPlace.getHouseholds(onDate).stream()
                    .filter(h -> h.getHead(onDate) != null
                            && h.getHead(onDate).getSocialClass().getRank() >= SocialClass.GENTLEMAN.getRank())
                    .collect(Collectors.toList());
            if (!leadingHouseholdList.isEmpty()) {
                leadingHouseholds = new ArrayList<>();
                for (Household household : leadingHouseholdList) {
                    leadingHouseholds.add(new HouseholdSummaryResponse(household, onDate));
                }
            }
        }
    }
}
