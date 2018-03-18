package com.meryt.demographics.response;

import javax.annotation.Nullable;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;

@Getter
public class PersonReference {
    private long id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private String age;

    public PersonReference(@NonNull Person person) {
        this(person, null);
    }

    public PersonReference(@NonNull Person person, @Nullable LocalDate onDate) {
        this.id = person.getId();
        this.firstName = person.getFirstName();
        this.lastName = person.getLastName();
        this.birthDate = person.getBirthDate();
        this.deathDate = person.getDeathDate();

        if (onDate != null && person.isLiving(onDate)) {
            age = person.getAge(onDate);
        } else {
            age = null;
        }
    }
}
