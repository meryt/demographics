package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;

@Service
public class InheritanceService {

    @Nullable
    Person findHeirForPerson(@NonNull Person person,
                                    @NonNull LocalDate onDate,
                                    @NonNull TitleInheritanceStyle inheritanceStyle,
                                    @Nullable Person inheritanceRoot) {

        List<Person> childrenByBirthDate = person.getChildren().stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .collect(Collectors.toList());
        for (Person child : childrenByBirthDate) {
            if (child.isMale()) {
                Person heir = getChildOrChildsHeirAsHeir(child, onDate, inheritanceStyle, inheritanceRoot);
                if (heir != null) {
                    return heir;
                }
            }
        }

        // We've gone through the males. We may now consider the females for some inheritance styles.
        if (!inheritanceStyle.isMalesOnly()) {
            List<Person> girls = childrenByBirthDate.stream()
                    .filter(Person::isFemale)
                    .collect(Collectors.toList());
            if (girls.size() == 1) {
                Person heiress = girls.get(0);
                Person heiressesHeir = getChildOrChildsHeirAsHeir(heiress, onDate, inheritanceStyle, inheritanceRoot);
                if (heiressesHeir != null) {
                    return heiressesHeir;
                }
            }
            // If there is more than 1 girl, the title may have gone into abeyance.
            // TODO this complicated calculation
            List<Person> possibleFemaleHeirs = girls.stream()
                    .filter(p -> mayHaveOrBeHeir(p, onDate))
                    .collect(Collectors.toList());
            if (possibleFemaleHeirs.size() == 1) {
                // TODO find the actual heir and the date, which may be well after the death date.
            }
        }

        // TODO consider brothers of the current title holder, cousins, heirs general, etc.

        return null;
    }

    private Person getChildOrChildsHeirAsHeir(@NonNull Person child,
                                              @NonNull LocalDate parentsDeathDate,
                                              @NonNull TitleInheritanceStyle inheritanceStyle,
                                              @Nullable Person inheritanceRoot) {
        if (child.isLiving(parentsDeathDate)) {
            return child;
        } else if (!child.getChildren().isEmpty()) {
            Person descendantHeir = findHeirForPerson(child, child.getDeathDate(), inheritanceStyle,
                    inheritanceRoot);
            if (descendantHeir != null) {
                return descendantHeir;
            }
        }
        return null;
    }

    private boolean mayHaveOrBeHeir(@NonNull Person person, @NonNull LocalDate onDate) {
        if (!person.isFinishedGeneration()) {
            return true;
        }
        if (person.isLiving(onDate)) {
            return true;
        }
        for (Person child : person.getChildren()) {
            if (mayHaveOrBeHeir(child, person.getDeathDate())) {
                return true;
            }
        }
        return false;
    }

}
