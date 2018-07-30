package com.meryt.demographics.response;

import java.time.LocalDate;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;

@Getter
public class RelatedPersonResponse extends PersonReference {

    private boolean finishedGeneration;
    private final Relationship relationship;

    public RelatedPersonResponse(@NonNull Person relatedPerson,
                                 @Nullable Relationship relationship,
                                 @Nullable LocalDate onDate) {
        super(relatedPerson, onDate);
        this.finishedGeneration = relatedPerson.isFinishedGeneration();
        this.relationship = relationship;
    }

    public RelatedPersonResponse(@NonNull Person relatedPerson, @Nullable Relationship relationship) {
        super(relatedPerson);
        this.finishedGeneration = relatedPerson.isFinishedGeneration();
        this.relationship = relationship;
    }

    @JsonIgnore
    public Integer getDegreeOfSeparation() {
        return relationship == null ? null : relationship.getDegreeOfSeparation();
    }

}
