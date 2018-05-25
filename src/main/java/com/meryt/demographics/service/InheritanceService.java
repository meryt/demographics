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
     *               parent predeceases the person, then it would be the grandparent's death date)
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
            } else if (mayHaveOrBeHeir(son, onDate, inheritanceStyle.isMalesOnly())) {
                // A dead son may not have finished generation or may have children that may be heirs once finished
                // generation. Return the son for now since we don't know what will happen in the future.
                sonsHeirs.add(son);
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
            // A living daughter is a possible heir. But unlike sons, we don't return immediately, but continue looping
            // through all daughters, since daughters split an inheritance.
            if (daughter.isLiving(onDate) || daughter.getBirthDate().isAfter(onDate)) {
                results.add(daughter);
            } else {
                // A dead daughter may herself have heirs.
                List<Person> daughtersHeirs = findPotentialHeirsForPerson(daughter, onDate, inheritanceStyle);
                if (daughtersHeirs.isEmpty() && !daughter.isFinishedGeneration()) {
                    // if she does not have heirs but is not finished generation, add her anyway
                    results.add(daughter);
                } else {
                    results.addAll(daughtersHeirs);
                }
            }
        }

        return results.stream().sorted(Comparator.comparing(Person::getDeathDate)).distinct().collect(Collectors.toList());
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

    private boolean mayHaveOrBeHeir(@NonNull Person person, @NonNull LocalDate onDate, boolean malesOnly) {
        if (malesOnly && person.isFemale()) {
            return false;
        }
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
            if (mayHaveOrBeHeir(child, childOnDate, malesOnly)) {
                return true;
            }
        }
        return false;
    }

}
