package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerationPost {
    private String outputToFile;
    private PersonFamilyPost personFamilyPost;

    public void validate() {
        if (personFamilyPost == null) {
            throw new IllegalArgumentException("personFamilyPost is required");
        }
    }
}
