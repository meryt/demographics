package com.meryt.demographics.domain.person;

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

}
