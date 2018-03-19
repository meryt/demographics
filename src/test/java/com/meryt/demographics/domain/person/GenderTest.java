package com.meryt.demographics.domain.person;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GenderTest {

    @Test
    public void maleSortsBeforeFemale() {
        Person man = new Person();
        man.setGender(Gender.MALE);

        Person woman = new Person();
        woman.setGender(Gender.FEMALE);

        List<Person> people = Arrays.asList(woman, man);

        Person first = people.stream()
                .sorted(Comparator.comparing(Person::getGender)).collect(Collectors.toList()).get(0);
        assertEquals(Gender.MALE, first.getGender());
    }
}
