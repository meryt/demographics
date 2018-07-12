package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Used to move a household to a dwelling place
 */
@Getter
@Setter
public class HouseholdPlacePost {

    private String onDate;
    private Long dwellingPlaceId;
    private Boolean evictCurrentResidents;
}
