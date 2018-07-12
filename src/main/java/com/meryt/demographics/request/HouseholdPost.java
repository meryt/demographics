package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class HouseholdPost {
    private Long headId;
    private String asOfDate;
    private Boolean includeHomelessFamilyMembers;
}
