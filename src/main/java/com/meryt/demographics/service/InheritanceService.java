package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;
import com.meryt.demographics.time.LocalDateComparator;

@Slf4j
@Service
public class InheritanceService {

    /**
     * Gets the potential heirs for a person on a given date.
     *
     * @param person the person whose heirs we want to find
     * @param onDate the date (may be his death date, but if we're calling recursively for a grandchild where the
     *               parent predecesases the person, then it would be the grandparent's death date)
     * @param inheritanceStyle the inheritance style to use (used to determine whether women can inherit)
     * @return a list, possibly empty, of one or more potential heirs, sorted by death date
     */
    @NonNull
    public List<Person> findPotentialHeirsForPerson(@NonNull Person person,
                                                    @NonNull LocalDate onDate,
                                                    @NonNull TitleInheritanceStyle inheritanceStyle) {
        List<Person> results = new ArrayList<>();
        List<Person> childrenByBirthDate = person.getChildren().stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .collect(Collectors.toList());

        // Loop through sons in order of birth
        for (Person son : childrenByBirthDate.stream().filter(Person::isMale).collect(Collectors.toList())) {
            // A living son is immediately heir.
            if (son.isLiving(onDate) || son.getBirthDate().isAfter(onDate)) {
                results.add(son);
                return results;
            }
            // A dead son may himself have heirs. Find his heirs on the death date of his father.
            List<Person> sonsHeirs = findPotentialHeirsForPerson(son, onDate, inheritanceStyle);
            if (!sonsHeirs.isEmpty()) {
                return sonsHeirs;
            }
        }

        // If this is a male-only inheritance, return immediately if no sons are alive or have living heirs.
        if (inheritanceStyle.isMalesOnly()) {
            return results;
        }

        List<Person> daughtersByBirthDate = childrenByBirthDate.stream()
                .filter(Person::isFemale)
                .collect(Collectors.toList());

        for (Person daughter : daughtersByBirthDate) {
            // A living daughter is a possible heir. But unlike sons, we don't return immediately, but continue looping.
            if (daughter.isLiving(onDate) || daughter.getBirthDate().isAfter(onDate)) {
                results.add(daughter);
            }
            // A dead daughter may herself have heirs.
            List<Person> daughtersHeirs = findPotentialHeirsForPerson(daughter, onDate, inheritanceStyle);
            results.addAll(daughtersHeirs);
        }

        return results.stream().sorted(Comparator.comparing(Person::getDeathDate)).collect(Collectors.toList());
    }

    @Nullable
    public Pair<Person, LocalDate> findHeirForPerson(@NonNull Person person,
                                                     @NonNull LocalDate onDate,
                                                     @NonNull TitleInheritanceStyle inheritanceStyle) {
        List<Person> allHeirsOnDate = findPotentialHeirsForPerson(person, onDate, inheritanceStyle);
        if (allHeirsOnDate.isEmpty()) {
            return null;
        } else if (allHeirsOnDate.size() == 1) {
            return Pair.of(allHeirsOnDate.get(0), onDate);
        }

        // The heirs are sorted by increasing order of death date. Iterate over people as they die off, assuming
        // they have finished having children. When we reach a single person or an empty list, or when the next person
        // to die has not finished having children, return.
        Person nextToDie;
        LocalDate nextDeathDate;
        LocalDate lastDeathDate = null;
        do {
            nextToDie = allHeirsOnDate.get(0);
            log.info("Next person is " + nextToDie.toString());
            nextDeathDate = nextToDie.getDeathDate();
            if (!nextToDie.isFinishedGeneration()) {
                log.info("Stopping search; this person has not finished generation");
                return null;
            }
            if (nextDeathDate.equals(lastDeathDate)) {
                // There's something wrong with the loop. The next death date should always be greater than the last.
                log.warn("Invalid loop condition: nextDeathDate equalled lastDeathDate of " + nextDeathDate.toString());
                return null;
            } else {
                lastDeathDate = nextDeathDate;
            }

            allHeirsOnDate = findPotentialHeirsForPerson(person, nextDeathDate.plusDays(1), inheritanceStyle);
            if (allHeirsOnDate.isEmpty()) {
                return null;
            } else if (allHeirsOnDate.size() == 1) {
                return Pair.of(allHeirsOnDate.get(0), nextDeathDate);
            }

        } while (true);

    }

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
            } else if (mayHaveOrBeHeir(heiress, onDate)) {
                // Perhaps she dies before her parent and has no children, but she may still have an heir. Return empty
                // list for now.
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
            List<Person> descendantHeir = findHeirForPerson(child, parentsDeathDate, inheritanceStyle,
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
            LocalDate childOnDate = LocalDateComparator.max(onDate, person.getDeathDate());
            if (mayHaveOrBeHeir(child, childOnDate)) {
                return true;
            }
        }
        return false;
    }

}
