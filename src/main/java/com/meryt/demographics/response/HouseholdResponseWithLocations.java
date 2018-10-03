package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.service.AncestryService;

public class HouseholdResponseWithLocations extends HouseholdResponse {

    @Getter
    private DwellingPlaceReference location;
    @Getter
    private List<DwellingPlaceReference> locations;

    public HouseholdResponseWithLocations(@NonNull Household household,
                                          @Nullable LocalDate onDate,
                                          @Nullable AncestryService ancestryService) {
        super(household, onDate, ancestryService);

        if (onDate == null) {
            populateDatelessData(household);
        } else {
            populateDatedData(household, onDate);
        }
    }

    private void populateDatelessData(@NonNull Household household) {
        List<HouseholdLocationPeriod> locationPeriods = household.getDwellingPlaces();
        if (!locationPeriods.isEmpty()) {
            locations = new ArrayList<>();
            for (HouseholdLocationPeriod period : locationPeriods) {
                locations.add(new DwellingPlaceReference(period.getDwellingPlace()));
            }
        }
    }

    private void populateDatedData(@NonNull Household household, @NonNull LocalDate onDate) {
        DwellingPlace place = household.getDwellingPlace(onDate);
        location = place == null ? null : new DwellingPlaceReference(place);
    }

}
