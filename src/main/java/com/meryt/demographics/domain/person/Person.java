package com.meryt.demographics.domain.person;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.fertility.Fertility;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.domain.person.fertility.Paternity;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdInhabitantPeriod;
import com.meryt.demographics.time.FormatPeriod;
import com.meryt.demographics.time.LocalDateComparator;

@Entity
@Table(name = "persons")
@Getter
@Setter
public class Person {

    private static final double BASE_PER_DAY_MARRY_DESIRE_FACTOR = 0.0019;

    @Id
    @SequenceGenerator(name="persons_id_seq", sequenceName="persons_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="persons_id_seq")
    private long id;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String firstName;

    private String middleNames;

    private String lastName;

    private LocalDate birthDate;

    private LocalDate deathDate;

    private String birthPlace;

    private String deathPlace;

    @Enumerated(EnumType.STRING)
    private SocialClass socialClass;

    private double domesticity;

    private double charisma;

    private double comeliness;

    private double intelligence;

    private double morality;

    private double strength;

    @JsonIgnore
    private String eyeGenes;

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private EyeColor eyeColor;

    @JsonIgnore
    private String hairGenes;

    @OneToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Maternity maternity;

    @OneToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Paternity paternity;

    @ManyToOne(cascade = { CascadeType.ALL })
    @JsonIgnore
    private Family family;

    @OneToMany(mappedBy = "husband", cascade = { CascadeType.ALL })
    @JsonIgnore
    @Setter(AccessLevel.PRIVATE)
    private Set<Family> fatheredFamilies = new HashSet<>();

    @OneToMany(mappedBy = "wife", cascade = { CascadeType.ALL })
    @JsonIgnore
    @Setter(AccessLevel.PRIVATE)
    private Set<Family> motheredFamilies = new HashSet<>();

    /**
     * A list of the households the person has been a part of, over time
     */
    @OneToMany(mappedBy = "person")
    private List<HouseholdInhabitantPeriod> households = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = { CascadeType.ALL })
    private List<PersonOccupationPeriod> occupations = new ArrayList<>();

    @JsonIgnore
    public Fertility getFertility() {
        if (gender == null) {
            return null;
        } else {
            return isMale() ? getPaternity() : getMaternity();
        }

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

    /**
     * Expose the age at death to the JSON
     *
     * @return null if there is no death and/or birth date, or the age in years, months, days (a string)
     */
    @SuppressWarnings("unused")
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
        Person father = getFather();
        if (father == null) {
            return false;
        }
        List<Person> legitimateChildren = father.getLegitimateChildren();
        legitimateChildren.sort(Comparator.comparing(Person::getBirthDate));
        LocalDate fatherDeathDate = father.getDeathDate();
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
    private Person getFather() {
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
    private List<Person> getLegitimateChildren() {
        Set<Family> families = getFamilies();
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
    public Set<Family> getFamilies() {
        if (isMale()) {
            return getFatheredFamilies();
        } else {
            return getMotheredFamilies();
        }
    }

    private Set<Family> getFatheredFamilies() {
        return Collections.unmodifiableSet(fatheredFamilies);
    }

    private Set<Family> getMotheredFamilies() {
        return Collections.unmodifiableSet(motheredFamilies);
    }

    public void setMaternity(Maternity maternity) {
        if (maternity != null && gender != null && !isFemale()) {
            throw new IllegalArgumentException("Cannot set Maternity on a male Person");
        }
        this.maternity = maternity;
        if (maternity != null) {
            this.maternity.setPerson(this);
        }
    }

    public void setPaternity(Paternity paternity) {
        if (paternity != null && gender != null && !isMale()) {
            throw new IllegalArgumentException("Cannot set Paternity on a female Person");
        }
        this.paternity = paternity;
        if (paternity != null) {
            this.paternity.setPerson(this);
        }
    }

    public void addToHousehold(@NonNull Household household, @NonNull LocalDate fromDate, boolean isHead) {
        for (HouseholdInhabitantPeriod period : getHouseholds()) {
            if (period.getFromDate().isBefore(fromDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(fromDate))) {
                period.setToDate(fromDate);
            }
        }
        HouseholdInhabitantPeriod newPeriod = new HouseholdInhabitantPeriod();

        newPeriod.setHousehold(household);

        household.getInhabitantPeriods().add(newPeriod);

        newPeriod.setPerson(this);
        newPeriod.setPersonId(getId());
        newPeriod.setFromDate(fromDate);
        newPeriod.setToDate(getDeathDate());
        newPeriod.setHouseholdHead(isHead);
        getHouseholds().add(newPeriod);
    }

    public void addOccupation(@NonNull Occupation occupation, @NonNull LocalDate fromDate) {
        for (PersonOccupationPeriod period : getOccupations()) {
            if (period.getFromDate().isBefore(fromDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(fromDate))) {
                period.setToDate(fromDate);
            }
        }
        PersonOccupationPeriod newPeriod = new PersonOccupationPeriod();
        newPeriod.setOccupation(occupation);
        newPeriod.setPerson(this);
        newPeriod.setPersonId(getId());
        newPeriod.setFromDate(fromDate);
        LocalDate age60 = getBirthDate().plusYears(60);
        if (age60.isBefore(getDeathDate())) {
            newPeriod.setToDate(age60);
        } else {
            newPeriod.setToDate(getDeathDate());
        }
        getOccupations().add(newPeriod);
    }

    public void addFatheredFamily(@NonNull Family family) {
        fatheredFamilies.add(family);
    }

    public void addMotheredFamily(@NonNull Family family) {
        motheredFamilies.add(family);
    }

    /**
     * Gets the eye color as a JSON-friendly string
     */
    public String getEyeColorName() {
        if (eyeColor == null) {
            return null;
        } else {
            return eyeColor.name().toLowerCase();
        }
    }

    /**
     * Gets the hair color in a JSON-friendly string
     * @return the hair color as determined by the genes, or null if genes are null
     */
    public String getHairColor() {
        return HairColor.getHairColorFromGenes(hairGenes);
    }
}
