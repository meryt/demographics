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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;
import com.meryt.demographics.time.LocalDateComparator;

@Service
@Slf4j
public class HeirService {

    private final PersonService personService;

    public HeirService(@NonNull @Autowired PersonService personService) {
        this.personService = personService;
    }

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
                                                    boolean mayLookInFuture,
                                                    boolean singleFemaleMayInherit) {
        
        if (inheritanceStyle == TitleInheritanceStyle.IRISH_KIN_GROUP) {
            return findPotentalHeirsForIrishKinGroup(person, onDate);
        }

        List<Person> results = new ArrayList<>();
        List<Person> childrenByBirthDate = person.getChildren().stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .filter(p -> mayLookInFuture || p.getBirthDate().isBefore(onDate) || p.getBirthDate().isEqual(onDate))
                .collect(Collectors.toList());
        List<Person> sonsByBirthDate = childrenByBirthDate.stream().filter(Person::isMale).collect(Collectors.toList());
        List<Person> daughtersByBirthDate = childrenByBirthDate.stream()
                .filter(Person::isFemale)
                .collect(Collectors.toList());

        if (singleFemaleMayInherit && !inheritanceStyle.isMalesOnly()) {
            sonsByBirthDate.addAll(daughtersByBirthDate);
        }

        // Loop through sons in order of birth
        for (Person son : sonsByBirthDate) {
            // A living son is immediately heir.
            if (son.isLiving(onDate) || (mayLookInFuture && son.getBirthDate().isAfter(onDate))) {
                results.add(son);
                return results;
            }
            // A dead son may himself have heirs. Find his heirs on the death date of his father.
            List<Person> sonsHeirs = findPotentialHeirsForPerson(son, onDate, inheritanceStyle, mayLookInFuture,
                    singleFemaleMayInherit);
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
        // Or, if it's not males-only but single females may inherit, we have already checked daughters and their issue
        // above, as well, so there is no need to examine daughters specifically.
        if (inheritanceStyle.isMalesOnly() || singleFemaleMayInherit) {
            return results;
        }


        for (Person daughter : daughtersByBirthDate) {
            // A living daughter is a possible heir. But unlike sons, we don't return immediately, but continue looping
            // through all daughters, since daughters split an inheritance.
            if (daughter.isLiving(onDate) || (mayLookInFuture && daughter.getBirthDate().isAfter(onDate))) {
                results.add(daughter);
            } else {
                // A dead daughter may herself have heirs.
                List<Person> daughtersHeirs = findPotentialHeirsForPerson(daughter, onDate, inheritanceStyle,
                        mayLookInFuture, singleFemaleMayInherit);
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

    private List<Person> findPotentalHeirsForIrishKinGroup(@NonNull Person person, @NonNull LocalDate onDate) {
        // Irish kin groups use a common great-grandfather
        Person root = person;
        int numGens = 3;
        while (numGens > 0 && root.getFather() != null) {
            root = root.getFather();
            numGens--;
        }

        List<Person> sons = root.getChildren().stream().filter(p -> p.isMale()).collect(Collectors.toList());
        List<Person> grandsons = sons.stream()
                .flatMap(son -> son.getChildren().stream())
                .filter(grandson -> grandson.isMale())
                .collect(Collectors.toList());
        List<Person> greatGrandsons = grandsons.stream()
                .flatMap(grandson -> grandson.getChildren().stream())
                .filter(greatGrandson -> greatGrandson.isMale())
                .collect(Collectors.toList());
        List<Person> greatGreatGrandsons = greatGrandsons.stream()
                .flatMap(greatGrandson -> greatGrandson.getChildren().stream())
                .filter(greatGreatGrandson -> greatGreatGrandson.isMale())
                .collect(Collectors.toList());

        List<Person> maleDescendants = new ArrayList<>();
        maleDescendants.addAll(sons);
        maleDescendants.addAll(grandsons);
        maleDescendants.addAll(greatGrandsons);
        maleDescendants.addAll(greatGreatGrandsons);

        List<Person> livingMaleDescendants = maleDescendants.stream()
                .filter(p -> p.isLiving(onDate))
                .collect(Collectors.toList());

        return livingMaleDescendants;
    }
    
    @Nullable
    public Pair<Person, LocalDate> findHeirForPerson(@NonNull Person person,
                                                     @NonNull LocalDate onDate,
                                                     @NonNull TitleInheritanceStyle inheritanceStyle,
                                                     boolean mayLookInFuture,
                                                     boolean singleFemaleMayInherit) {
        List<Person> allHeirsOnDate = findPotentialHeirsForPerson(person, onDate, inheritanceStyle, mayLookInFuture,
                singleFemaleMayInherit)
                .stream()
                .filter(p -> mayLookInFuture || (p.isLiving(onDate)))
                .collect(Collectors.toList());
        if (allHeirsOnDate.isEmpty()) {
            return null;
        } else if (allHeirsOnDate.size() == 1) {
            return Pair.of(allHeirsOnDate.get(0), onDate);
        }

        if (inheritanceStyle == TitleInheritanceStyle.IRISH_KIN_GROUP) {
            return findHeirForIrishKinGroup(allHeirsOnDate, onDate);
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

            allHeirsOnDate = findPotentialHeirsForPerson(person, nextDeathDate.plusDays(1), inheritanceStyle,
                    mayLookInFuture, singleFemaleMayInherit)
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

    private Pair<Person, LocalDate> findHeirForIrishKinGroup(@NonNull List<Person> persons, @NonNull LocalDate onDate) {
        // Find the best heir.

        // First look for the strongest adult with a son
        List<Person> adultsWithSons = persons.stream()
            .filter(p -> p.getAgeInYears(onDate) >= 30)
            .filter(p -> p.getChildren().stream().anyMatch(c -> c.isMale()))
            .sorted(Comparator.comparing(Person::getStrength).reversed())
            .collect(Collectors.toList());
        if (!adultsWithSons.isEmpty()) {
            return Pair.of(adultsWithSons.get(0), onDate);
        }
        
        // Then look for the strongest adult
        List<Person> adults = persons.stream()
            .filter(p -> p.getAgeInYears(onDate) >= 30)
            .sorted(Comparator.comparing(Person::getStrength).reversed())
            .collect(Collectors.toList());
        if (!adults.isEmpty()) {
            return Pair.of(adults.get(0), onDate);
        }

        // Finally look for the strongest person
        List<Person> any = persons.stream()
            .sorted(Comparator.comparing(Person::getStrength).reversed())
            .collect(Collectors.toList());
        if (!any.isEmpty()) {
            return Pair.of(any.get(0), onDate);
        }
        return null;
    }

    @NonNull
    List<Person> findHeirsForCashInheritance(@NonNull Person person, @NonNull LocalDate onDate, boolean allowUnrelated) {
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

        Person ancestor = person;
        // Look for heirs up paternal line
        while (heirs.isEmpty() && ancestor.getFather() != null) {
            ancestor = ancestor.getFather();
            // Recursively get heirs for father, grandfather, etc., but excluding the person himself.
            List<Person> potentialHeirs = findPotentialHeirsForPerson(ancestor, onDate,
                    TitleInheritanceStyle.HEIRS_GENERAL, false, true).stream()
                .filter(p -> !p.equals(person))
                .collect(Collectors.toList());
            heirs.addAll(potentialHeirs);
        }

        // Look for any living descendants
        if (heirs.isEmpty()) {
            heirs.addAll(personService.findDescendants(person, onDate).stream()
                    .sorted(Comparator.comparing(Person::getBirthDate))
                    .collect(Collectors.toList()));
        }

        // Look for any living relatives
        if (heirs.isEmpty()) {
            List<Person> closestLiving = personService.findClosestLivingRelatives(person, onDate, 8L);
            // All persons returned will be at same distance from the dead person. So we sort them randomly.
            Collections.shuffle(closestLiving);
            if (closestLiving.size() > 5) {
                // For distant relationships there might be dozens of 3rd cousins, great-nieces, etc. Just pick
                // 5 random people at that distance.
                heirs.addAll(closestLiving.subList(0, 5));
            } else {
                heirs.addAll(closestLiving);
            }
        }

        if (heirs.isEmpty() && allowUnrelated) {
            log.info(String.format("Unable to find any living natural heirs for %d %s on %s. " +
                    "Will try to find a random neighbor.", person.getId(), person.getName(), onDate));

            DwellingPlace dwelling = person.getResidence(onDate.minusDays(1));
            if (dwelling != null) {
                // In case a person is erroneously listed as still living in the household after death, filter out
                // the dead.
                List<Person> dwellingResidents = dwelling.getAllResidents(onDate).stream()
                        .filter(p -> p.isLiving(onDate))
                        .collect(Collectors.toList());
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

        final List<Person> heirs = new ArrayList<>();

        Person eldestSon = null;
        if (person.getSocialClassRank() >= SocialClass.GENTLEMAN.getRank()) {
            // A gentleman's proper heir should inherit the principal real estate first, ensuring a wife's second
            // husband's child will not later inherit it if she inherits.
            Pair<Person, LocalDate> son = findHeirForPerson(person, onDate, TitleInheritanceStyle.HEIRS_OF_THE_BODY,
                    false, true);
            if (son != null && son.getFirst().isLiving(onDate)) {
                eldestSon = son.getFirst();
                heirs.add(son.getFirst());
            }
        }

        final Person alreadyAddedSon = eldestSon;

        // Spouse is first
        if (person.isMarried(onDate)) {
            heirs.add(person.getSpouse(onDate));
        }
        // Living children are second, in order of birth
        heirs.addAll(person.getLivingChildren(onDate).stream()
                .filter(p -> !p.equals(alreadyAddedSon))
                .sorted(Comparator.comparing(Person::getBirthDate))
                .collect(Collectors.toList()));
        if (!heirs.isEmpty()) {
            return heirs;
        }

        // If there is neither spouse nor living children, look for other heirs.
        heirs.addAll(findHeirsForCashInheritance(person, onDate, true));
        return heirs;
    }

    Person findMaleHeirForEntailments(@NonNull Person person, @NonNull LocalDate onDate) {
        Person maleHeirForEntailments = findPotentialHeirsForPerson(person, onDate,
                TitleInheritanceStyle.HEIRS_MALE_GENERAL, false, false).stream()
                .filter(p -> p.isLiving(onDate))
                .min(Comparator.comparing(Person::getBirthDate)).orElse(null);
        Person father = person;
        while (maleHeirForEntailments == null && (father = father.getFather()) != null) {
            maleHeirForEntailments = findPotentialHeirsForPerson(father, onDate.plusDays(1),
                    TitleInheritanceStyle.HEIRS_MALE_GENERAL, false, false).stream()
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
