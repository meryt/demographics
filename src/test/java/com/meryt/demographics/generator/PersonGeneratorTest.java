package com.meryt.demographics.generator;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PersonGeneratorTest {

    @Test
    public void randomDomesticityIsBetween0And1() {
        PersonGenerator personGenerator = new PersonGenerator(null, null);
        for (int i = 0; i < 100; i++) {
            double randomDomesticity = personGenerator.randomDomesticity();
            assertTrue(randomDomesticity + " was not <= 1.0", randomDomesticity <= 1.0);
            assertTrue(randomDomesticity + " was not >= 0.0", randomDomesticity >= 0.0);
        }
    }
}
