package com.meryt.demographics.generator;

import java.util.Random;

/**
 * A die that rolls values between 2 ints, inclusive
 */
public class BetweenDie {

    private final Random random = new Random();

    public int roll(int lowerBound, int upperBound) {
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Lower bound cannot be greater than upper bound");
        }
        if (upperBound == lowerBound) {
            return lowerBound;
        }

        return random.nextInt(upperBound - lowerBound ) + lowerBound;
    }
}
