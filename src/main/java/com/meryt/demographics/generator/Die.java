package com.meryt.demographics.generator;

import java.util.Random;

/**
 * A die with a certain number of faces that can be rolled.
 */
public class Die {

    private final Random random = new Random();
    private final int faces;

    Die(int faces) {
        this.faces = faces;
    }

    /**
     * Roll die once
     */
    int roll() {
        return random.nextInt(faces) + 1;
    }

    /**
     * Roll die n times and return the sum
     */
    public int roll(int times) {
        int result = 0;
        for (int i = 0; i < times; i++) {
            result += roll();
        }
        return result;
    }

}
