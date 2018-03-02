package com.meryt.demographics.domain.person;

import lombok.Getter;

public enum Gender {
    MALE("M"),
    FEMALE("F");

    @Getter
    private final String abbreviation;

    Gender(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public static Gender from(String value) {
        if ("M".equalsIgnoreCase(value) || "MALE".equalsIgnoreCase(value)) {
            return Gender.MALE;
        } else if ("F".equalsIgnoreCase(value) || "FEMALE".equalsIgnoreCase(value)) {
            return Gender.FEMALE;
        } else {
            throw new IllegalArgumentException("Unknown gender \"" + value + "\"");
        }
    }
}
