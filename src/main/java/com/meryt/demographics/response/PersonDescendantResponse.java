package com.meryt.demographics.response;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

@Getter
public class PersonDescendantResponse extends PersonReference {

    private final String ageAtDeath;
    private final SocialClass socialClass;
    private final String relation;
    private List<PersonDescendantResponse> children;

    public PersonDescendantResponse(@NonNull Person person, int distanceFromRoot, int remainingDepth) {
        super(person);
        relation = calculateRelation(person.getGender(), distanceFromRoot);
        ageAtDeath = person.getAgeAtDeath();
        socialClass = person.getSocialClass();

        if (remainingDepth == 0) {
            children = null;
        } else {
            children = person.getFamilies().stream()
                    .flatMap(f -> f.getChildren().stream())
                    .map(p -> new PersonDescendantResponse(p, distanceFromRoot + 1, remainingDepth - 1))
                    .sorted(Comparator.comparing(PersonReference::getBirthDate))
                    .collect(Collectors.toList());
            if (children.isEmpty()) {
                children = null;
            }
        }
    }

    private String calculateRelation(@NonNull Gender gender, int distanceFromRoot) {
        if (distanceFromRoot == 0) {
            return "self";
        }

        String sonOrDaughter = gender == Gender.MALE ? "son" : "daughter";
        if (distanceFromRoot == 1) {
            return sonOrDaughter;
        } else if (distanceFromRoot == 2) {
            return "grand" + sonOrDaughter;
        } else if (distanceFromRoot == 3) {
            return "great-grand" + sonOrDaughter;
        } else if (distanceFromRoot == 4) {
            return "great-great-grand" + sonOrDaughter;
        } else {
            return (distanceFromRoot - 2) + " x great-grand" + sonOrDaughter;
        }
    }
}
