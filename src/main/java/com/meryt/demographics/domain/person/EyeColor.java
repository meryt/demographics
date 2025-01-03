package com.meryt.demographics.domain.person;

import lombok.Getter;

import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.generator.random.PercentDie;

public enum EyeColor {
    BLUE(true, false),  // blue
    GRAY(true, false),  // blue
    GREEN(false, false), // green
    HAZEL(false, false), // green
    AMBER(false, true), // brown
    BROWN(false, true), // brown
    BLACK(false, true); // brown

    @Getter
    final boolean isBlue;

    @Getter
    final boolean isBrown;

    EyeColor(boolean isBlue, boolean isBrown) {
        this.isBlue = isBlue;
        this.isBrown = isBrown;
    }

    /**
     * Given a set of genes, determine a random eye color
     * @param genes one of "TT", "CC", "TC", or "CT"
     * @return an eye color
     */
    public static EyeColor randomFromGenes(String genes) {
        double roll = PercentDie.roll();
        switch (genes) {
            case "CC":
                if (roll <= 0.72) {
                    return randomBlue();
                } else if (roll <= 0.99) {
                    return randomGreen();
                } else {
                    return randomBrown();
                }
            case "TC":
            case "CT":
                if (roll <= 0.56) {
                    return randomBrown();
                } else if (roll <= 0.93) {
                    return randomGreen();
                } else {
                    return randomBlue();
                }
            case "TT":
                if (roll <= 0.85) {
                    return randomBrown();
                } else if (roll <= 0.99) {
                    return randomGreen();
                } else {
                    return randomBlue();
                }
            default:
                throw new IllegalArgumentException("Unrecognized eye gene type " + genes + "; must be CC, TC, or TT");
        }
    }

    private static EyeColor randomBlue() {
        int d6Roll = new Die(6).roll();
        if (d6Roll <= 4) {
            return EyeColor.BLUE;
        } else {
            return EyeColor.GRAY;
        }
    }

    private static EyeColor randomGreen() {
        int d6Roll = new Die(6).roll();
        if (d6Roll <= 3 ) {
            return EyeColor.GREEN;
        } else {
            return EyeColor.HAZEL;
        }
    }

    private static EyeColor randomBrown() {
        int d6Roll = new Die(6).roll();
        if (d6Roll <= 2) {
            return EyeColor.AMBER;
        } else if (d6Roll <= 4) {
            return EyeColor.BROWN;
        } else {
            return EyeColor.BLACK;
        }
    }
}
