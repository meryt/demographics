package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;

@Getter
abstract class AbstractFamilyResponse {

    private final long id;

    private final LocalDate weddingDate;

    private final List<PersonReference> children;

    AbstractFamilyResponse(@NonNull Family family) {
        id = family.getId();
        weddingDate = family.getWeddingDate();

        List<Person> sortedChildren = family.getChildren().stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .collect(Collectors.toList());
        if (!sortedChildren.isEmpty()) {
            children = new ArrayList<>();
            for (Person child : sortedChildren) {
                children.add(new PersonReference(child));
            }
        } else {
            children = null;
        }
    }
}
