package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import com.meryt.demographics.domain.person.SocialClass;

@Getter
@Setter
public class FamilyParameters {

    public static final int DEFAULT_MIN_HUSBAND_AGE = 17;
    public static final int DEFAULT_MAX_HUSBAND_AGE = 50;
    public static final int DEFAULT_MIN_WIFE_AGE = 15;
    public static final int DEFAULT_MAX_WIFE_AGE = 50;
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

    // TODO make use of these
    private SocialClass minSocialClass;

    private SocialClass maxSocialClass;

    /**
     * If true, will save the family after generating.
     */
    private boolean persist;

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

    /**
     * Gets the max wife age set in the parameters; or if null, returns the husband's age in the reference year.
     */
    public int getMaxWifeAgeOrDefault(int husbandAge) {
        return maxWifeAge == null ? husbandAge : maxWifeAge;
    }

    public double getPercentMaleFoundersOrDefault() {
        return percentMaleFounders == null ? DEFAULT_PERCENT_MALE_FOUNDERS : percentMaleFounders;
    }

}
