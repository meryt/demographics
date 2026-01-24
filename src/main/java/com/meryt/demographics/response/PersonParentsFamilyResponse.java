package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;

/**
 * Response for the family of which the given person is a child. Shows the father and mother and excludes the person
 * himself from the siblings list.
 */
@Getter
class PersonParentsFamilyResponse {

    private static final int NUM_GENERATIONS = 2;

    private final long id;
    private final PersonReference father;
    private final PersonReference mother;
    private final LocalDate weddingDate;
    private final List<PersonReference> siblings;

    PersonParentsFamilyResponse(@NonNull Family family, @NonNull Person person, @Nullable LocalDate onDate) {
        id = family.getId();
        father = family.getHusband() == null ? null : new PersonParentsResponse(family.getHusband(), NUM_GENERATIONS, onDate);
        mother = family.getWife() == null ? null : new PersonParentsResponse(family.getWife(), NUM_GENERATIONS, onDate);
        weddingDate = family.getWeddingDate();

        List<Person> sortedChildren = family.getChildren().stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .filter(p -> p.getId() != person.getId())
                .collect(Collectors.toList());
        if (!sortedChildren.isEmpty()) {
            siblings = new ArrayList<>();
            for (Person child : sortedChildren) {
                siblings.add(new PersonReference(child, onDate));
            }
        } else {
            siblings = null;
        }
    }
}
