package com.meryt.demographics.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitialGenerationPost extends OutputToFilePost {
    private List<String> lastNames;
    private List<String> scottishLastNames;
    private List<String> requiredLastNames;
    private List<String> requiredScottishLastNames;
    private Integer numFamilies;
    private RandomFamilyParameters familyParameters;
    private Double percentScottish;
    private Integer numNextGenerations;
    private PersonFamilyPost nextGenerationPost;

    public void validate() {
        if (numFamilies == null || numFamilies <= 0) {
            throw new IllegalArgumentException("a numFamilies > 0 must be specified");
        }
        familyParameters.validate();
    }
}
