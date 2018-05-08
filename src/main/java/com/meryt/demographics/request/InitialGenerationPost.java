package com.meryt.demographics.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitialGenerationPost extends OutputToFilePost {
    List<String> lastNames;
    Integer numFamilies;
    FamilyParameters familyParameters;

    public void validate() {
        if (numFamilies == null || numFamilies <= 0) {
            throw new IllegalArgumentException("a numFamilies > 0 must be specified");
        }
        familyParameters.validate();
    }
}
