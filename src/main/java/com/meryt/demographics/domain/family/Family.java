package com.meryt.demographics.domain.family;

import com.meryt.demographics.domain.person.Person;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * A Family consists of a male and female and optionally any biological children. The two people may or may not be
 * married.
 */
@Getter
@Setter
public class Family {

    private Person husband;
    private Person wife;
    private List<Person> children;
    private LocalDate weddingDate;
}
