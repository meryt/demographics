package com.meryt.demographics.request;

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
    private String untilDate;

    /**
     * If present, a randomly-generated spouse will use this last name
     */
    private String spouseLastName;

    private boolean persist;

    /**
     * If non-null this person will be used as the spouse, rather than generating a random one.
     */
    private Long spouseId;

    /**
     * If true, and if spouseId is null, then we will load potential spouses from the database and possibly pick one
     * at random.
     */
    private boolean allowExistingSpouse;

    /**
     * If non-null, and allowExistingSpouse is true and is used, then if there is less than this many potential
     * spouses, there's a possibility a random spouse will be generated instead.
     */
    private Integer minSpouseSelection;

    /**
     * Try up to this many times, if no spouse is found on the first try. If null, only tries once.
     */
    private Integer triesUntilGiveUp;

    private Boolean skipGenerateChildren;

    public void validate() {
        // nothing to validate yet
    }

}

