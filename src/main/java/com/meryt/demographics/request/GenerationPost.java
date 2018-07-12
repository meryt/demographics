package com.meryt.demographics.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerationPost extends OutputToFilePost {
    private PersonFamilyPost personFamilyPost;
    /**
     * This param can be used to perform these operations only on non-residents. Only applies if a non-null reference
     * date is set on the PersonFamilyPost.
     */
    private Boolean onlyNonResidents;

    public void validate() {
        if (personFamilyPost == null) {
            throw new IllegalArgumentException("personFamilyPost is required");
        }
    }
}
