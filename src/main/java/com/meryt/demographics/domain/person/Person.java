package com.meryt.demographics.domain.person;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.Null;

@Getter
@Setter
public class Person {

    private Gender gender;
    private String firstName;
    private String middleNames;
    private String lastName;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private long lifespanInDays;
    private SocialClass socialClass;

    public int getAgeInYears(@NonNull LocalDate onDate) {
        if (getBirthDate() == null) {
            throw new IllegalStateException("Cannot determine age for a person with a null birth date");
        }

        return (int) getBirthDate().until(onDate, ChronoUnit.YEARS);
    }

}
