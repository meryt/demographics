package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

@Getter
@Setter
public class FamilyParameters {

    public static final int DEFAULT_MIN_HUSBAND_AGE = 17;
    public static final int DEFAULT_MAX_HUSBAND_AGE = 50;
    public static final int DEFAULT_MIN_WIFE_AGE = 15;
    public static final int DEFAULT_MAX_WIFE_AGE = 50;
    public static final int DEFAULT_MAX_OLDER_WIFE_AGE_DIFF = 3;
    public static final int DEFAULT_MAX_MARRIAGEABLE_WIFE_AGE = 35;
    public static final int DEFAULT_MIN_DEGREES_SEPARATION = 4;
    private static final double DEFAULT_PERCENT_MALE_FOUNDERS = 0.8;

    /**
     * Reference year used for generating a family. The founders will be of an age to have been married before this
     * year, and may have had children by then.
     */
    private LocalDate referenceDate;

    /**
     * The generated husband should have an age no less than this in the reference year
     */
    private Integer minHusbandAge;

    /**
     * The generated husband should have an age no greater than this in the reference year
     */
    private Integer maxHusbandAge;

    /**
     * The generated wife should have an age no less than this in the reference year
     */
    private Integer minWifeAge;

    /**
     * The generated wife should have an age no greater than this in the reference year
     */
    private Integer maxWifeAge;

    /**
     * Determines how likely the founder of a random family is to be a man. Defaults to 0.8.
     */
    private Double percentMaleFounders;

    private SocialClass minSocialClass;

    private SocialClass maxSocialClass;

    /**
     * If set, this last name will be used for the founder.
     */
    private String founderLastName;

    /**
     * If set, this last name will be used for the spouse.
     */
    private String spouseLastName;

    /**
     * If set, we will attempt to use this spouse (assuming dates are valid, etc.). The person may still choose not to
     * marry or the people may not be compatible, so there is no guarantee the family will be created for these people.
     */
    private Person spouse;

    /**
     * If true, will attempt to find a spouse among existing persons in the database.
     */
    private boolean allowExistingSpouse;

    /**
     * Only used if allowExistingSpouse is true. If there are fewer than this many existing eligible spouses on the
     * search date, random potential spouses will be created to fill out the selection to this many people. For example
     * if this is set to 5 and there are only 2 potential spouses in the database, there is a 3/5 chance a random
     * spouse will be used instead of one from the database.
     */
    private Integer minSpouseSelection;

    /**
     * If true, will save the family after generating.
     */
    private boolean persist;

    /**
     * If true, will continue checking for children until the death of the father or mother, rather than stopping
     * at the reference date
     */
    private boolean cycleToDeath;

    /**
     * For generated families, the wife will be no more than this many years older than the husband.
     */
    private Integer maxOlderWifeAgeDiff;

    /**
     * For generated families, a woman stops being eligible to marry at this age.
     */
    private Integer maxMarriageableWifeAge;

    /**
     * For relatives to marry there must be at least this many degrees of separation
     */
    private Integer minDegreesSeparation;

    public int getMinHusbandAgeOrDefault() {
        return minHusbandAge == null ? DEFAULT_MIN_HUSBAND_AGE : minHusbandAge;
    }

    public int getMinHusbandAgeOrDefault(int wifeAge) {
        return minHusbandAge == null ? wifeAge : minHusbandAge;
    }

    public int getMaxHusbandAgeOrDefault() {
        return maxHusbandAge == null ? DEFAULT_MAX_HUSBAND_AGE : maxHusbandAge;
    }

    public int getMinWifeAgeOrDefault() {
        return minWifeAge == null ? DEFAULT_MIN_WIFE_AGE : minWifeAge;
    }

    public int getMaxWifeAgeOrDefault() {
        return maxWifeAge == null ? DEFAULT_MAX_WIFE_AGE : maxWifeAge;
    }

    public int getMaxOlderWifeAgeDiffOrDefault() {
        return maxOlderWifeAgeDiff == null ? DEFAULT_MAX_OLDER_WIFE_AGE_DIFF : maxOlderWifeAgeDiff;
    }

    public int getMaxMarriageableWifeAgeOrDefault() {
        return maxMarriageableWifeAge == null ? DEFAULT_MAX_MARRIAGEABLE_WIFE_AGE : maxMarriageableWifeAge;
    }

    public double getPercentMaleFoundersOrDefault() {
        return percentMaleFounders == null ? DEFAULT_PERCENT_MALE_FOUNDERS : percentMaleFounders;
    }

    public int getMinDegreesSeparationOrDefault() {
        return minDegreesSeparation == null ? DEFAULT_MIN_DEGREES_SEPARATION : minDegreesSeparation;
    }

    public void validate() {
        if (referenceDate == null) {
            throw new IllegalArgumentException("referenceDate is required");
        }
    }

}
