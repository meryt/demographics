package com.meryt.demographics.domain.person;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Person {

    private Gender gender;
    private String firstName;
    private String middleNames;
    private String lastName;
    private LocalDateTime birthDate;
    private LocalDateTime deathDate;

}
