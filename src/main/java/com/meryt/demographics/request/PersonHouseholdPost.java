package com.meryt.demographics.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PersonHouseholdPost {

    private Long householdId;
    private boolean isHead;
    private LocalDate fromDate;
    private boolean includeHomelessFamilyMembers;
}
