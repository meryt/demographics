package com.meryt.demographics.response;

import java.time.LocalDate;
import javax.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;

/**
 * Response for a family of which the given person is the husband or wife. Presumes that we're showing the family as
 * a member of the person. Shows only the "spouse" field.
 */
@Getter
@JsonPropertyOrder({ "id", "weddingDate", "husbandAgeAtMarriage", "wifeAgeAtMarriage", "relationshipBetweenSpouses",
        "spouse", "children" })
public class PersonFamilyResponse extends AbstractFamilyResponse {

    private PersonResponse spouse;

    private String relationshipBetweenSpouses;

    public PersonFamilyResponse(@NonNull Person person,
                                @NonNull Family family,
                                @Nullable Relationship relationshipBetweenSpouses,
                                @Nullable LocalDate onDate) {
        super(family, onDate);

        if (person.isMale() && family.getWife() != null) {
            spouse = new PersonResponse(family.getWife(), onDate);
        } else if (person.isFemale() && family.getHusband() != null) {
            spouse = new PersonResponse(family.getHusband(), onDate);
        }
        this.relationshipBetweenSpouses = relationshipBetweenSpouses == null ? null : relationshipBetweenSpouses.getName();
    }

    public PersonFamilyResponse(@NonNull Person person, @NonNull Family family, @Nullable LocalDate onDate) {
        this(person, family, null, onDate);
    }

}
