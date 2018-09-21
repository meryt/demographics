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

@Getter
abstract class AbstractFamilyResponse {

    private final long id;

    private final LocalDate weddingDate;
    private String husbandAgeAtMarriage;
    private String wifeAgeAtMarriage;

    private final List<PersonResponse> children;

    AbstractFamilyResponse(@NonNull Family family, @Nullable LocalDate onDate) {
        id = family.getId();
        weddingDate = family.getWeddingDate();
        husbandAgeAtMarriage = family.getHusbandAgeAtMarriage();
        wifeAgeAtMarriage = family.getWifeAgeAtMarriage();

        List<Person> sortedChildren = family.getChildren().stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .collect(Collectors.toList());
        if (!sortedChildren.isEmpty()) {
            children = new ArrayList<>();
            for (Person child : sortedChildren) {
                children.add(new PersonResponse(child, onDate));
            }
        } else {
            children = null;
        }
    }
}
