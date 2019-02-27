package com.meryt.demographics.response;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;

@Getter
public class PersonReference {
    private long id;
    private final String firstName;
    private final String lastName;
    private final LocalDate birthDate;
    private final LocalDate deathDate;
    private final String age;
    private final List<PersonTitleResponse> titles;

    public PersonReference(@NonNull Person person) {
        this(person, null);
    }

    public PersonReference(@NonNull Person person, @Nullable LocalDate onDate) {
        this.id = person.getId();
        this.firstName = person.getFirstName();
        this.lastName = person.getLastName();
        this.birthDate = person.getBirthDate();
        this.deathDate = person.getDeathDate();

        if (onDate != null && person.isLiving(onDate) && !person.getTitles(onDate).isEmpty()) {
            titles = person.getTitles(onDate).stream()
                    .map(PersonTitleResponse::new)
                    .collect(Collectors.toList());
        } else if (!person.getTitles().isEmpty()){
            titles = person.getTitles().stream()
                    .map(PersonTitleResponse::new)
                    .collect(Collectors.toList());
        } else {
            titles = null;
        }

        if (onDate != null && person.isLiving(onDate)) {
            age = person.getAge(onDate);
        } else {
            age = null;
        }
    }
}
