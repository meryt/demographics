package com.meryt.demographics.response;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;

@Getter
public class HouseholdResponse {

    private final long id;

    private final DwellingPlaceSummaryResponse location;
    private final List<DwellingPlaceSummaryResponse> locations;

    public HouseholdResponse(@NonNull Household household) {
        this(household, null);
    }

    public HouseholdResponse(@NonNull Household household, @Nullable LocalDate onDate) {
        id = household.getId();

        if (onDate == null) {
            location = null;
            List<HouseholdLocationPeriod> locationPeriods = household.getDwellingPlaces();
            if (locationPeriods.isEmpty()) {
                locations = null;
            } else {
                locations = new ArrayList<>();
                for (HouseholdLocationPeriod period : locationPeriods) {
                    locations.add(new DwellingPlaceSummaryResponse(period.getDwellingPlace()));
                }
            }
        } else {
            locations = null;
            DwellingPlace place = household.getDwellingPlace(onDate);
            location = place == null ? null : new DwellingPlaceSummaryResponse(place, onDate);
        }
    }

}
