package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import org.junit.Test;

import com.meryt.demographics.domain.person.Person;

import static org.junit.Assert.assertTrue;

public class MatchMakerTest {

    @Test
    public void getDesireToMarryProbabilityIsBetweenZeroAndOne() {
        Person person = new Person();
        person.setDomesticity(1.0);
        person.setBirthDate(LocalDate.of(1700, 1, 1));
        for (int year = 1701; year < 1800; year++) {
            double percent = MatchMaker.getDesireToMarryProbability(person, LocalDate.of(year, 1, 1), 0, null, null);
            assertTrue(String.format("Percent %4f is not between 0.0 and 1.0", percent), percent > 0.0 && percent <= 1.0);
        }
    }
}
