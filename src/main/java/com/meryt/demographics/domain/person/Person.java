package com.meryt.demographics.domain.person;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class Person {

    private static final double BASE_PER_DAY_MARRY_DESIRE_FACTOR = 0.0019;

    private Gender gender;
    private String firstName;
    private String middleNames;
    private String lastName;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private long lifespanInDays;
    private SocialClass socialClass;
    private double domesticity;

    public int getAgeInYears(@NonNull LocalDate onDate) {
        if (getBirthDate() == null) {
            throw new IllegalStateException("Cannot determine age for a person with a null birth date");
        }

        return (int) getBirthDate().until(onDate, ChronoUnit.YEARS);
    }

    /**
     * Determines whether this person is male. If gender is null, defaults to true.
     */
    @JsonIgnore
    public boolean isMale() {
        return gender == null || gender.equals(Gender.MALE);
    }

    /**
     * Determines whether the person is female. If gender is null, defaults to false.
     */
    @JsonIgnore
    public boolean isFemale() {
        return !isMale();
    }

    public double getDailyDesireToMarryProbability(LocalDate onDate) {
        return getDomesticity() * BASE_PER_DAY_MARRY_DESIRE_FACTOR;
    }

}
