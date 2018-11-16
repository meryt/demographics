package com.meryt.demographics.response;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.HouseholdLocationPeriod;

@Getter
class ResidencePeriodResponse {

    private final long householdId;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final DwellingPlaceSummaryResponse location;


    ResidencePeriodResponse(@NonNull HouseholdLocationPeriod locationPeriod) {
        this.householdId = locationPeriod.getHouseholdId();
        this.fromDate = locationPeriod.getFromDate();
        this.toDate = locationPeriod.getToDate();
        this.location = new DwellingPlaceSummaryResponse(locationPeriod.getDwellingPlace(), null);
    }

}
