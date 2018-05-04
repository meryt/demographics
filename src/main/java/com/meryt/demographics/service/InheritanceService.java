package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
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

    @NonNull
    List<Person> findHeirForPerson(@NonNull Person person,
                                    @NonNull LocalDate onDate,
                                    @NonNull TitleInheritanceStyle inheritanceStyle,
                                    @Nullable Person inheritanceRoot) {

        List<Person> childrenByBirthDate = person.getChildren().stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .collect(Collectors.toList());

        // First check sons. A first born son and his line inherit before younger sons and their lines.
        for (Person child : childrenByBirthDate) {
            // Loop through the sons in order of birth
            if (child.isMale()) {
                // If the son or one (or some, if he has only daughters) of his line is alive, return them.
                List<Person> heirs = getChildOrChildsHeirAsHeir(child, onDate, inheritanceStyle, inheritanceRoot);
                if (!heirs.isEmpty()) {
                    return heirs;
                }
                // If the child is not an heir, and does not have children, he may yet be on the line of descent.
                // Return empty heirs for now, if so.
                if (mayHaveOrBeHeir(child, onDate)) {
                    return heirs;
                }
            }
        }

        // We've gone through the sons. For male-only inheritance, we should check brothers, nephews, etc.
        // For now return none.
        if (inheritanceStyle.isMalesOnly()) {
            return new ArrayList<>();
        }


        // We've gone through the sons. We may now consider the daughters for some inheritance styles.
        List<Person> girls = childrenByBirthDate.stream()
                .filter(Person::isFemale)
                .collect(Collectors.toList());

        // If there is only one daughter, she or her heirs are heir.
        if (girls.size() == 1) {
            Person heiress = girls.get(0);
            List<Person> heiressesHeirs = getChildOrChildsHeirAsHeir(heiress, onDate, inheritanceStyle, inheritanceRoot);
            if (!heiressesHeirs.isEmpty()) {
                return heiressesHeirs;
            }
        }

        // If there is more than 1 girl, the title may have gone into abeyance.
        List<Person> possibleFemaleHeirs = girls.stream()
                .filter(p -> mayHaveOrBeHeir(p, onDate))
                .collect(Collectors.toList());

        // Any girl who is finished generation and has no children may be eliminated as of her death date.
        // If there are any
        List<Person> possibleHeirsInOrderOfDeath = possibleFemaleHeirs.stream()
                .sorted(Comparator.comparing(Person::getDeathDate))
                .collect(Collectors.toList());

        do {
            if (possibleHeirsInOrderOfDeath.isEmpty() || !possibleHeirsInOrderOfDeath.get(0).isFinishedGeneration()) {
                break;
            }
            Person nextToDie = possibleHeirsInOrderOfDeath.remove(0);
            List<Person> heirsOfNextToDie = findHeirForPerson(nextToDie, nextToDie.getDeathDate(), inheritanceStyle,
                    inheritanceRoot);
            if (!heirsOfNextToDie.isEmpty()) {
                possibleHeirsInOrderOfDeath.addAll(heirsOfNextToDie);
                possibleHeirsInOrderOfDeath = possibleHeirsInOrderOfDeath.stream()
                        .sorted(Comparator.comparing(Person::getDeathDate))
                        .collect(Collectors.toList());
            }
        } while (possibleHeirsInOrderOfDeath.size() > 1);

        return possibleHeirsInOrderOfDeath;
    }

    private List<Person> getChildOrChildsHeirAsHeir(@NonNull Person child,
                                                    @NonNull LocalDate parentsDeathDate,
                                                    @NonNull TitleInheritanceStyle inheritanceStyle,
                                                    @Nullable Person inheritanceRoot) {
        List<Person> heirs = new ArrayList<>();
        // If the child is alive when the parent dies, or if he is a posthumous child, he is himself the heir.
        if (child.isLiving(parentsDeathDate) || child.getBirthDate().isAfter(parentsDeathDate)) {
            heirs.add(child);
            return heirs;
        } else if (!child.getChildren().isEmpty()) {
            List<Person> descendantHeir = findHeirForPerson(child, child.getDeathDate(), inheritanceStyle,
                    inheritanceRoot);
            if (!descendantHeir.isEmpty()) {
                return descendantHeir;
            }
        }
        return heirs;
    }

    private boolean mayHaveOrBeHeir(@NonNull Person person, @NonNull LocalDate onDate) {
        if (!person.isFinishedGeneration()) {
            // She may yet have children, so she may have or be an heir
            return true;
        }
        if (person.isLiving(onDate)) {
            // If she's still alive she may herself be an heir, even if she never had children
            return true;
        }
        // If she's dead but had children, they may be heirs if they were alive on her death date and/or had heirs of
        // their own.
        for (Person child : person.getChildren()) {
            if (mayHaveOrBeHeir(child, person.getDeathDate())) {
                return true;
            }
        }
        return false;
    }

}
