package com.meryt.demographics.domain.person;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersonTest {

    @Test
    public void testGetAgeInYears() {
        Person person = new Person();
        person.setBirthDate(LocalDate.of(1700, 1, 1));

        assertEquals(1, person.getAgeInYears(LocalDate.of(1701, 1, 1)));
        assertEquals(0, person.getAgeInYears(LocalDate.of(1700, 12, 31)));
        assertEquals(10, person.getAgeInYears(LocalDate.of(1710, 10, 10)));
        assertEquals(-1, person.getAgeInYears(LocalDate.of(1699, 1, 1)));
    }
}
