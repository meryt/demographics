package com.meryt.demographics.response;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;

@Getter
public class RelatedPersonResponse extends PersonReference {

    private boolean finishedGeneration;
    private final Relationship relationship;

    public RelatedPersonResponse(@NonNull Person relatedPerson, @Nullable Relationship relationship) {
        super(relatedPerson);
        this.finishedGeneration = relatedPerson.isFinishedGeneration();
        this.relationship = relationship;
    }

}
