package com.meryt.demographics.domain.person;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SocialClassTest {

    @Test
    public void randomBetweenWorksForMinAndMax() {
        SocialClass between = SocialClass.randomBetween(SocialClass.GENTLEMAN, SocialClass.BARON);
        assertTrue(between.getRank() >= SocialClass.GENTLEMAN.getRank(), String.format("%s is not >= GENTLEMAN", between.name()));
        assertTrue(between.getRank() <= SocialClass.BARON.getRank(), String.format("%s is not <= BARON", between.name()));
    }

}
