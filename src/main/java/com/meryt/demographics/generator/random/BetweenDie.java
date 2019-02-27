package com.meryt.demographics.generator.random;

import java.util.Random;
import lombok.experimental.UtilityClass;

/**
 * A die that rolls values between 2 ints, inclusive
 */
@UtilityClass
public class BetweenDie {

    private static final Random random = new Random();

    public static int roll(int lowerBound, int upperBound) {
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Lower bound cannot be greater than upper bound");
        }
        if (upperBound == lowerBound) {
            return lowerBound;
        }

        return random.nextInt(upperBound - lowerBound ) + lowerBound;
    }
}
