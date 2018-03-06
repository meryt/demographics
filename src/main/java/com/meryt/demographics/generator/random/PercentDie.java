package com.meryt.demographics.generator.random;

import java.util.Random;

/**
 * Die that returns values between [0.0, 1.0)
 */
public class PercentDie {

    private final Random random = new Random();

    public double roll() {
        return random.nextFloat();
    }
}
