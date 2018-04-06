package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;

/**
 * Response for a family of which the given person is the husband or wife. Presumes that we're showing the family as
 * a member of the person. Shows only the "spouse" field.
 */
@Getter
class PersonFamilyResponse extends AbstractFamilyResponse {

    private PersonReference spouse;

    PersonFamilyResponse(@NonNull Person person, @NonNull Family family) {
        super(family);

        if (person.isMale() && family.getWife() != null) {
            spouse = new PersonReference(family.getWife());
        } else if (person.isFemale() && family.getHusband() != null) {
            spouse = new PersonReference(family.getHusband());
        }
    }

}
