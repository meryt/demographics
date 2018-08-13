package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PersonHouseholdPost {

    private Long householdId;
    private boolean isHead;
    private String fromDate;
    private boolean includeHomelessFamilyMembers;
}
