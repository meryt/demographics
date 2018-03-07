package com.meryt.demographics.domain.person;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.fertility.Fertility;
import com.meryt.demographics.time.LocalDateComparator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class Person {

    private static final double BASE_PER_DAY_MARRY_DESIRE_FACTOR = 0.0019;

    private long id;
    private Gender gender;
    private String firstName;
    private String middleNames;
    private String lastName;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private Long familyId;
    private SocialClass socialClass;
    private double domesticity;
    private double comeliness;
    private double charisma;
    private Fertility fertility;
    private Family family;

    /**
     * Gets the person's age in years on a given date. Does not check death date to ensure person is still alive.
     *
     * @return age in full years (may be negative if date is before birth date
     * @throws IllegalStateException if the person has a null birth date
     */
    @JsonIgnore
    public int getAgeInYears(@NonNull LocalDate onDate) {
        if (getBirthDate() == null) {
            throw new IllegalStateException("Cannot determine age for a person with a null birth date");
        }

        return (int) getBirthDate().until(onDate, ChronoUnit.YEARS);
    }

    /**
     * Gets the person's age in days on a given date. Does not check death date to ensure person is still alive.
     *
     * @return age in days (may be negative if date is before birth date
     * @throws IllegalStateException if the person has a null birth date
     */
    @JsonIgnore
    public long getAgeInDays(@NonNull LocalDate onDate) {
        if (getBirthDate() == null) {
            throw new IllegalStateException("Cannot determine age for a person with a null birth date");
        }
        return LocalDateComparator.daysBetween(birthDate, onDate);
    }

    /**
     * Determines whether this person is male. If gender is null, defaults to true.
     */
    @JsonIgnore
    public boolean isMale() {
        return gender == null || gender.equals(Gender.MALE);
    }

    /**
     * Determines whether the person is female. If gender is null, defaults to false.
     */
    @JsonIgnore
    public boolean isFemale() {
        return !isMale();
    }

    @JsonIgnore
    public boolean isLiving(@NonNull LocalDate onDate) {
        return birthDate != null && !onDate.isBefore(birthDate) && (deathDate == null || !onDate.isAfter(deathDate));
    }

    public String getName() {
        return (firstName + " " + lastName).trim();
    }

    public String getLastName(@NonNull LocalDate onDate) {
        if (isMale()) {
            return getLastName();
        } else {
            List<Family> marriages = getFamilies().stream()
                    .filter(Family::isMarriage)
                    .filter(f -> f.getWeddingDate().isBefore(onDate))
                    .sorted(Comparator.comparing(Family::getWeddingDate))
                    .collect(Collectors.toList());
            if (marriages.isEmpty()) {
                return getLastName();
            } else {
                Person husbandOnDate = marriages.get(0).getHusband();
                return husbandOnDate == null ? getLastName() : husbandOnDate.getLastName();
            }
        }
    }

    /**
     * Determines whether the person is the oldest surviving legitimate son of his father. If a date is passed in,
     * only determines whether the person is still living on that date. If no date is passed, only determines whether
     * the son lived longer than the father.
     *
     * @param onDate optional parameter to determine whether the son is living on this date, versus surviving longer
     *               than the father
     * @return true if the person is a son, and has a father with a non-null death date, and is either still alive on
     * this date or does not predecease the father.
     */
    @JsonIgnore
    public boolean isFirstbornSurvivingSonOfFather(LocalDate onDate) {
        if (!isMale()) {
            return false;
        }
        if (getFather() == null) {
            return false;
        }
        List<Person> legitimateChildren = getFather().getLegitimateChildren();
        legitimateChildren.sort(Comparator.comparing(Person::getBirthDate));
        LocalDate fatherDeathDate = getFather().getDeathDate();
        if (fatherDeathDate == null) {
            return false;
        }
        List<Person> legitimateMalesSurvivingFather = legitimateChildren.stream()
                .filter(Person::isMale)
                .filter(p -> p.getDeathDate() != null && (onDate != null ? p.isLiving(onDate) : p.getDeathDate().isAfter(fatherDeathDate)))
                .collect(Collectors.toList());
        return !legitimateMalesSurvivingFather.isEmpty() && legitimateChildren.get(0).getId() == getId();
    }

    @JsonIgnore
    public Person getFather() {
        if (getFamily() == null) {
            return null;
        } else {
            return getFamily().getHusband();
        }
    }

    /**
     * Gets all children from this person's marriages. Excludes any children who do not have birth dates.
     */
    @JsonIgnore
    public List<Person> getLegitimateChildren() {
        List<Family> families = getFamilies();
        List<Person> children = new ArrayList<>();
        for (Family fam : families) {
            if (fam.isMarriage()) {
                children.addAll(fam.getChildren().stream()
                        .filter(p -> p.getBirthDate() != null)
                        .collect(Collectors.toList()));
            }
        }
        children.sort(Comparator.comparing(Person::getBirthDate));
        return children;
    }

    /**
     * Gets the families of which this person is the father or mother, NOT the one of which he is a child.
     */
    @JsonIgnore
    public List<Family> getFamilies() {
        // TODO
        return Collections.emptyList();
    }
}
