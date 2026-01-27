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

    /**
     * This param can be used to perform these operations only on a single person. If a non-null reference date is
     * set on the PersonFamilyPost, will loop until then. Otherwise until his death.
     */
    private Long onlyForPerson;

    private Boolean skipTitleUpdate;

    private Boolean advanceToReferenceDate = true;

    public void validate() {
        if (personFamilyPost == null) {
            throw new IllegalArgumentException("personFamilyPost is required");
        }
    }

    public boolean isAdvanceToReferenceDate() {
        return advanceToReferenceDate != null && advanceToReferenceDate;
    }

}
