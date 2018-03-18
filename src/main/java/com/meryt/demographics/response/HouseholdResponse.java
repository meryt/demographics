package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.Household;

@Getter
public class HouseholdResponse {

    private final long id;

    public HouseholdResponse(@NonNull Household household) {
        id = household.getId();
    }

}
