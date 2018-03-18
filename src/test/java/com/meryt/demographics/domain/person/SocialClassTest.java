package com.meryt.demographics.domain.person;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class SocialClassTest {

    @Test
    public void randomBetweenWorksForMinAndMax() {
        SocialClass between = SocialClass.randomBetween(SocialClass.GENTLEMAN, SocialClass.BARON);
        assertTrue(String.format("%s is not >= GENTLEMAN", between.name()), between.getRank() >= SocialClass.GENTLEMAN.getRank());
        assertTrue(String.format("%s is not <= BARON", between.name()), between.getRank() <= SocialClass.BARON.getRank());
    }

}
