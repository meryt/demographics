package com.meryt.demographics.domain;

import java.time.LocalDate;
import org.junit.Test;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlagueTest {

    @Test
    public void testCalculation() {
        LocalDate testDate = LocalDate.of(1350, 6, 1);

        Plague blackDeath = Plague.getPlagueForDate(testDate);
        assertNotNull(blackDeath);

        Person testPerson = new Person();
        testPerson.setBirthDate(LocalDate.of(1320, 6, 1));
        testPerson.setDeathDate(LocalDate.of(1360, 1, 1));
        testPerson.setGender(Gender.MALE);


        double overallChanceOfDying = blackDeath.getOverallChanceOfDying(testPerson, testDate);
        assertEquals(0.3, overallChanceOfDying, 0.0001);
        double delta = 0.02;

        int populationSize = 10_000;
        long expectedDeaths = Math.round(overallChanceOfDying * populationSize);
        long numDays = blackDeath.getDaysLength();

        int numDeaths = 0;
        for (int i = 0; i < numDays; i++) {
            for (int j = 0; j < populationSize; j++) {
                if (blackDeath.didPersonDieOnDate(testPerson, testDate)) {
                    numDeaths++;
                    populationSize--;
                }
            }
        }

        assertTrue(String.format(
                "Expected around %d deaths but had %d deaths", expectedDeaths, numDeaths),
                Math.abs(expectedDeaths - numDeaths) <= delta * expectedDeaths);

    }

}
