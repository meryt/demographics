package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstatePost {

    private Long parentDwellingPlaceId;
    private String name;
    private String dwellingName;
    private Long ownerId;
    private String ownerFromDate;
    private Long entailedTitleId;
    private Boolean mustPurchase;
    /**
     * If parentDwellingPlaceId is null, and existingHouseId is non-null and corresponds to a Dwelling, the existing
     * house will have an estate created around it.
     */
    private Long existingHouseId;
}
