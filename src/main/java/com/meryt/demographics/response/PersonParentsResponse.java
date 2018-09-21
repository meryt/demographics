package com.meryt.demographics.response;

import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;

@Getter
class PersonParentsResponse extends PersonReference {

    private final PersonReference father;
    private final PersonReference mother;

    PersonParentsResponse(@NonNull Person person, int numLevelsRemaining) {
        super(person);
        if (person.getFamily() == null) {
            father = null;
            mother = null;
        } else {
            Family family = person.getFamily();
            father = getResponseForParent(family.getHusband(), numLevelsRemaining);
            mother = getResponseForParent(family.getWife(), numLevelsRemaining);
        }
    }

    private PersonReference getResponseForParent(@Nullable Person parent, int numLevelsRemaining) {
        if (parent == null) {
            return null;
        }

        if (numLevelsRemaining == 0) {
            return new PersonReference(parent);
        } else {
            return new PersonParentsResponse(parent, numLevelsRemaining - 1);
        }
    }

}
