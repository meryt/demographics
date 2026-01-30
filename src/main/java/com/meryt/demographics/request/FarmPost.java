package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FarmPost {

    private Long parentDwellingPlaceId;
    private String farmName;
    private String houseName;
    private Long ownerId;
    private String ownerFromDate;
    private Boolean includeHomelessFamilyMembers;
    private Boolean includeSiblings;
    private Double acres;

    public boolean getIncludeHomelessFamilyMembersOrDefault() {
        return includeHomelessFamilyMembers == null ? false : includeHomelessFamilyMembers;
    }

    public boolean getIncludeSiblingsOrDefault() {
        return includeSiblings == null ? false : includeSiblings;
    }
}
