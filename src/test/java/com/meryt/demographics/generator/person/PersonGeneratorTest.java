package com.meryt.demographics.generator.person;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonGeneratorTest {

    @Test
    public void randomDomesticityIsBetween0And1() {
        for (int i = 0; i < 100; i++) {
            double randomDomesticity = PersonGenerator.randomDomesticity();
            assertTrue(randomDomesticity <= 1.0, randomDomesticity + " was not <= 1.0");
            assertTrue(randomDomesticity >= 0.0, randomDomesticity + " was not >= 0.0");
        }
    }
}
