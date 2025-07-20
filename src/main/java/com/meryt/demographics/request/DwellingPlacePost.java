package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DwellingPlacePost {
    private Long id;
    private String name;
    private Long parentId;
    private String type;
    private String foundedDate;
    private Long ownerId;
    private String ownerFromDate;
    private String ownerReason;
    private Double acres;
}
