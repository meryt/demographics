package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;
import com.meryt.demographics.time.LocalDateComparator;

@Service
@Slf4j
public class HeirService {

    /**
     * Gets the potential heirs for a person on a given date.
     *
     * @param person the person whose heirs we want to find
     * @param onDate the date (may be his death date, but if we're calling recursively for a grandchild where the
     *               parent predeceases the person, then it would be the grandparent's death date)
     * @param inheritanceStyle the inheritance style to use (used to determine whether women can inherit)
     * @param mayLookInFuture if false, does not consider people not yet born, or people who are dead but for whom
     *                        generation is not finished.
     * @return a list, possibly empty, of one or more potential heirs, sorted by death date
     */
    @NonNull
    public List<Person> findPotentialHeirsForPerson(@NonNull Person person,
                                                    @NonNull LocalDate onDate,
                                                    @NonNull TitleInheritanceStyle inheritanceStyle,
                                                    boolean mayLookInFuture) {
        List<Person> results = new ArrayList<>();
        List<Person> childrenByBirthDate = person.getChildren().stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .filter(p -> mayLookInFuture || p.getBirthDate().isBefore(onDate) || p.getBirthDate().isEqual(onDate))
                .collect(Collectors.toList());

        // Loop through sons in order of birth
        for (Person son : childrenByBirthDate.stream().filter(Person::isMale).collect(Collectors.toList())) {
            // A living son is immediately heir.
            if (son.isLiving(onDate) || (mayLookInFuture && son.getBirthDate().isAfter(onDate))) {
                results.add(son);
                return results;
            }
            // A dead son may himself have heirs. Find his heirs on the death date of his father.
            List<Person> sonsHeirs = findPotentialHeirsForPerson(son, onDate, inheritanceStyle, mayLookInFuture);
            if (!sonsHeirs.isEmpty()) {
                return sonsHeirs;
            } else if (mayLookInFuture && mayHaveOrBeHeir(son, onDate, inheritanceStyle.isMalesOnly())) {
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
            if (daughter.isLiving(onDate) || (mayLookInFuture && daughter.getBirthDate().isAfter(onDate))) {
                results.add(daughter);
            } else {
                // A dead daughter may herself have heirs.
                List<Person> daughtersHeirs = findPotentialHeirsForPerson(daughter, onDate, inheritanceStyle,
                        mayLookInFuture);
                if (daughtersHeirs.isEmpty() && (mayLookInFuture && !daughter.isFinishedGeneration())) {
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
                                                     @NonNull TitleInheritanceStyle inheritanceStyle,
                                                     boolean mayLookInFuture) {
        List<Person> allHeirsOnDate = findPotentialHeirsForPerson(person, onDate, inheritanceStyle, mayLookInFuture)
                .stream()
                .filter(p -> mayLookInFuture || (p.isLiving(onDate)))
                .collect(Collectors.toList());
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
            if (mayLookInFuture && !nextToDie.isFinishedGeneration()) {
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

            allHeirsOnDate = findPotentialHeirsForPerson(person, nextDeathDate.plusDays(1), inheritanceStyle, mayLookInFuture)
                    .stream()
                    .filter(p -> mayLookInFuture || (p.isLiving(onDate)))
                    .collect(Collectors.toList());
            if (allHeirsOnDate.isEmpty()) {
                return null;
            } else if (allHeirsOnDate.size() == 1) {
                return Pair.of(allHeirsOnDate.get(0), nextDeathDate);
            }

        } while (true);

    }

    @NonNull
    List<Person> findHeirsForCashInheritance(@NonNull Person person, @NonNull LocalDate onDate) {
        final List<Person> heirs = new ArrayList<>();
        if (person.isMarried(onDate)) {
            heirs.add(person.getSpouse(onDate));
        }
        heirs.addAll(person.getLivingChildren(onDate));

        if (heirs.isEmpty() && person.getFather() != null && person.getFather().isLiving(onDate)) {
            heirs.add(person.getFather());
        }

        if (heirs.isEmpty() && person.getMother() != null && person.getMother().isLiving(onDate)) {
            heirs.add(person.getMother());
        }

        if (heirs.isEmpty()) {
            if (person.getFather() != null) {
                heirs.addAll(person.getFather().getLivingChildren(onDate).stream()
                        .filter(p -> p.getId() != person.getId())
                        .collect(Collectors.toList()));
            }
            if (person.getMother() != null) {
                heirs.addAll(person.getMother().getLivingChildren(onDate).stream()
                        .filter(p -> p.getId() != person.getId() && !heirs.contains(p))
                        .collect(Collectors.toList()));
            }
        }

        if (heirs.isEmpty()) {
            heirs.addAll(findPotentialHeirsForPerson(person, onDate, TitleInheritanceStyle.HEIRS_GENERAL, false));
        }

        if (heirs.isEmpty()) {
            log.info(String.format("Unable to find any living natural heirs for %d %s on %s. " +
                    "Will try to find a random neighbor.", person.getId(), person.getName(), onDate));

            DwellingPlace dwelling = person.getResidence(onDate.minusDays(1));
            if (dwelling != null) {
                List<Person> dwellingResidents = dwelling.getAllResidents(onDate);
                if (!dwellingResidents.isEmpty()) {
                    Collections.shuffle(dwellingResidents);
                    heirs.add(dwellingResidents.get(0));
                } else {
                    DwellingPlace parentDwelling = dwelling.getParent();
                    if (parentDwelling != null) {
                        dwellingResidents = parentDwelling.getAllResidents(onDate);
                        if (!dwellingResidents.isEmpty()) {
                            Collections.shuffle(dwellingResidents);
                            heirs.add(dwellingResidents.get(0));
                        }
                    }
                }
            }
        }
        return heirs;
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

    @NonNull
    List<Person> findHeirsForRealEstate(@NonNull Person person, @NonNull LocalDate onDate) {
        // Spouse is first
        final List<Person> heirs = new ArrayList<>();
        if (person.isMarried(onDate)) {
            heirs.add(person.getSpouse(onDate));
        }
        // Living children are second, in order of birth
        heirs.addAll(person.getLivingChildren(onDate).stream().sorted(Comparator.comparing(Person::getBirthDate))
                .collect(Collectors.toList()));
        if (!heirs.isEmpty()) {
            return heirs;
        }

        // If there is neither spouse nor living children, look for other heirs.
        heirs.addAll(findHeirsForCashInheritance(person, onDate));
        return heirs;
    }

    Person findMaleHeirForEntailments(@NonNull Person person, @NonNull LocalDate onDate) {
        Person maleHeirForEntailments = findPotentialHeirsForPerson(person, onDate,
                TitleInheritanceStyle.HEIRS_MALE_GENERAL, false).stream()
                .filter(p -> p.isLiving(onDate))
                .min(Comparator.comparing(Person::getBirthDate)).orElse(null);
        Person father;
        while (maleHeirForEntailments == null && (father = person.getFather()) != null) {
            maleHeirForEntailments = findPotentialHeirsForPerson(father, onDate.plusDays(1),
                    TitleInheritanceStyle.HEIRS_MALE_GENERAL, false).stream()
                    .filter(p -> p.isLiving(onDate))
                    .min(Comparator.comparing(Person::getBirthDate)).orElse(null);
        }
        return maleHeirForEntailments;
    }

    @Nullable
    Person findPossibleHeirForDwellingPlace(@NonNull DwellingPlace dwellingPlace, @NonNull LocalDate onDate) {
        List<Person> residents = dwellingPlace.getAllResidents(onDate).stream()
                .filter(p -> p.isLiving(onDate))
                .collect(Collectors.toList());
        if (residents.isEmpty()) {
            residents = dwellingPlace.getParent().getAllResidents(onDate).stream()
                    .filter(p -> p.isLiving(onDate))
                    .collect(Collectors.toList());
            if (residents.isEmpty()) {
                return null;
            }
        }
        Collections.shuffle(residents);
        return residents.get(0);
    }

}
