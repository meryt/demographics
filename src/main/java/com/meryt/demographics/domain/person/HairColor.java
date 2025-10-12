package com.meryt.demographics.domain.person;

import com.meryt.demographics.generator.random.PercentDie;

public class HairColor {

    private HairColor() {
        // hide constructor for static member class
    }

    public static String getHairColorFromGenes(String genes) {
        if (genes == null) {
            return null;
        }
        switch (genes) {
            case "BBRR":
            case "BBRr":
                return "black";
            case "BbRR":
            case "BbRr":
                return "brown";
            case "bbRR":
            case "bbRr":
                return "blond";
            case "BBrr":
                return "auburn";
            case "Bbrr":
                return "red";
            case "bbrr":
                return "strawberry";
            default:
                throw new IllegalArgumentException("No hair color known for gene set " + genes);
        }
    }

    public static String getGenesFromHairColor(String hairColor) {
        double roll = PercentDie.roll();
        switch (hairColor) {
            case "black":
                if (roll < 0.5) {
                    return "BBRR";
                } else {
                    return "BBRr";
                }
            case "brown":
                if (roll < 0.5) {
                    return "BbRR";
                } else {
                    return "BbRr";
                }
            case "blond":
                if (roll < 0.5) {
                    return "bbRR";
                } else {
                    return "bbRr";
                }
            case "auburn":
                return "BBrr";
            case "red":
                return "Bbrr";
            case "strawberry":
                return "bbrr";
            default:
                throw new IllegalArgumentException("Invalid hair color  " + hairColor);
        }
    }

    public static boolean isBlond(String genes) {
        return genes.startsWith("bb");
    }

    public static boolean isReddish(String genes) {
        return genes.equals("Bbrr") || genes.equals("bbrr") || genes.equals("BBrr");
    }
}
