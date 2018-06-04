package com.meryt.demographics.response;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.Household;

@Getter
public class HouseholdSummaryResponse {

    private final long id;

    private final PersonSummaryResponse head;

    private final DwellingPlaceReference location;

    public HouseholdSummaryResponse(@NonNull Household household, @NonNull LocalDate onDate) {
        id = household.getId();
        Person headOfHousehold = household.getHead(onDate);
        if (headOfHousehold != null) {
            head = new PersonSummaryResponse(headOfHousehold, onDate);
        } else {
            head = null;
        }
        if (household.getDwellingPlace(onDate) != null) {
            location = new DwellingPlaceReference(household.getDwellingPlace(onDate));
        } else {
            location = null;
        }
    }
}
