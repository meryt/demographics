package com.meryt.demographics.domain.person;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.fertility.Fertility;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.domain.person.fertility.Paternity;
import com.meryt.demographics.time.FormatPeriod;
import com.meryt.demographics.time.LocalDateComparator;

@Entity
@Table(name = "persons")
@Getter
@Setter
public class Person {

    private static final double BASE_PER_DAY_MARRY_DESIRE_FACTOR = 0.0019;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private Gender gender;
    private String firstName;
    private String middleNames;
    private String lastName;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private SocialClass socialClass;
    private double domesticity;
    private double charisma;
    private double comeliness;
    private double intelligence;
    private double morality;
    private double strength;
    @OneToOne
    @JoinColumn(name = "id")
    private Maternity maternity;
    @OneToOne
    @JoinColumn(name = "id")
    private Paternity paternity;
    @ManyToOne
    private Family family;

    @JsonIgnore
    public Fertility getFertility() {
        return gender == null ? null : (isMale() ? getPaternity() : getMaternity());
    }

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

    public String getAgeAtDeath() {
        if (getBirthDate() == null || getDeathDate() == null) {
            return null;
        } else {
            return FormatPeriod.asYearsMonthsDays(getBirthDate().until(deathDate));
        }
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

    @JsonIgnore
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

    public void setMaternity(Maternity maternity) {
        if (maternity != null && gender != null && !isFemale()) {
            throw new IllegalArgumentException("Cannot set Maternity on a male Person");
        }
        this.maternity = maternity;
    }

    public void setPaternity(Paternity paternity) {
        if (paternity != null && gender != null && !isMale()) {
            throw new IllegalArgumentException("Cannot set Paternity on a female Person");
        }
        this.paternity = paternity;
    }
}
