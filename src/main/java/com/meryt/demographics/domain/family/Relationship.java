package com.meryt.demographics.domain.family;

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

    private final String name;
    private final int degreeOfSeparation;
    private final String personVia;
    private final String relatedPersonVia;

    /**
     * Returns true if the people are a parent and a child
     */
    public boolean isParentChildRelationship() {
        return name.equals(SON) || name.equals(DAUGHTER) || name.equals(FATHER) || name.equals(MOTHER);
    }

    /**
     * Returns true if the people are siblings or half-siblings
     */
    public boolean isSiblingRelationship() {
        return name.equals(BROTHER) || name.equals(SISTER) || name.equals(HALF_BROTHER) || name.equals(HALF_SISTER);
    }
}
