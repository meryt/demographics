package com.meryt.demographics.request;

import com.meryt.demographics.domain.person.Gender;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PersonParameters {

    private Gender gender;
    private LocalDate aliveOnDate;


}
