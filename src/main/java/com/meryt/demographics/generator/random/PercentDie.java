package com.meryt.demographics.generator.random;

import java.util.Random;

/**
 * Die that returns values between [0.0, 1.0)
 */
public class PercentDie {

    private static final Random random = new Random();

    public static double roll() {
        return random.nextFloat();
    }
}
