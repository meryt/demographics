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
}
