package com.meryt.demographics.response;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;

@Getter
public class DwellingPlaceOwnerResponse {

    private final PersonReference person;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final String reason;

    public DwellingPlaceOwnerResponse(@NonNull DwellingPlaceOwnerPeriod ownerPeriod) {
        person = new PersonReference(ownerPeriod.getOwner());
        fromDate = ownerPeriod.getFromDate();
        toDate = ownerPeriod.getToDate();
        reason = ownerPeriod.getReason();
    }
}
