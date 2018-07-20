package com.meryt.demographics.domain.family;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Relationship {
    public static String SON = "son";
    public static String DAUGHTER = "daughter";
    public static String FATHER = "father";
    public static String MOTHER = "mother";
    public static String BROTHER = "brother";
    public static String SISTER = "sister";
    public static String HALF_BROTHER = "half-brother";
    public static String HALF_SISTER = "half-sister";
    public static String SELF = "self";
    public static String SPOUSE = "spouse";
    public static String PARTNER = "partner";

    private final String name;
    private final int degreeOfSeparation;
    private final String personVia;
    private final String relatedPersonVia;

    /**
     * Returns true if the people are a parent and a child
     */
    @JsonIgnore
    public boolean isParentChildRelationship() {
        return name.equals(SON) || name.equals(DAUGHTER) || name.equals(FATHER) || name.equals(MOTHER);
    }

    /**
     * Returns true if the people are siblings or half-siblings
     */
    @JsonIgnore
    public boolean isSiblingRelationship() {
        return name.equals(BROTHER) || name.equals(SISTER) || name.equals(HALF_BROTHER) || name.equals(HALF_SISTER);
    }

    @JsonIgnore
    public boolean isSelf() {
        return name.equals(SELF);
    }

    @JsonIgnore
    public boolean isMarriage() {
        return name.equals(SPOUSE);
    }

    @JsonIgnore
    public boolean isMarriageOrPartnership() {
        return isMarriage() || name.equals(PARTNER);
    }
}
