package com.meryt.demographics.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Can be used to create a new family for a person by finding a spouse and generating children.
 */
@Getter
@Setter
public class PersonFamilyPost {

    private Integer minHusbandAge;
    private Integer minWifeAge;

    /**
     * Check for spouse/children until this date, if specified. Otherwise use the person's death date.
     */
    private LocalDate untilDate;

    /**
     * If present, a randomly-generated spouse will use this last name
     */
    private String spouseLastName;

    private boolean persist;

    /**
     * If non-null this person will be used as the spouse, rather than generating a random one.
     */
    private Long spouseId;

    public void validate() {
        // nothing to validate yet
    }

}

