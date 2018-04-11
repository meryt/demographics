package com.meryt.demographics.domain.person;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
import javax.persistence.OrderBy;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
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
import com.meryt.demographics.domain.title.Title;
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

    private String eyeGenes;

    @Enumerated(EnumType.STRING)
    private EyeColor eyeColor;

    private String hairGenes;

    private Double heightInches;

    @OneToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Maternity maternity;

    @OneToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Paternity paternity;

    @ManyToOne(cascade = { CascadeType.ALL })
    private Family family;

    @OneToMany(mappedBy = "husband", cascade = { CascadeType.ALL })
    @Setter(AccessLevel.PRIVATE)
    private Set<Family> fatheredFamilies = new HashSet<>();

    @OneToMany(mappedBy = "wife", cascade = { CascadeType.ALL })
    @Setter(AccessLevel.PRIVATE)
    private Set<Family> motheredFamilies = new HashSet<>();

    /**
     * A list of the households the person has been a part of, over time
     */
    @OneToMany(mappedBy = "person")
    private List<HouseholdInhabitantPeriod> households = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = { CascadeType.ALL })
    private List<PersonOccupationPeriod> occupations = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = { CascadeType.ALL })
    @OrderBy("from_date")
    private List<PersonTitlePeriod> titles = new ArrayList<>();

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
    public String getAgeAtDeath() {
        if (getBirthDate() == null || getDeathDate() == null) {
            return null;
        } else {
            return FormatPeriod.asYearsMonthsDays(getBirthDate().until(deathDate));
        }
    }

    public String getAge(@NonNull LocalDate onDate) {
        if (getBirthDate() == null || !isLiving(onDate)) {
            return null;
        }
        return FormatPeriod.diffAsYearsMonthsDays(getBirthDate(), onDate);
    }

    /**
     * Determines whether this person is male. If gender is null, defaults to true.
     */
    public boolean isMale() {
        return gender == null || gender.equals(Gender.MALE);
    }

    /**
     * Determines whether the person is female. If gender is null, defaults to false.
     */
    public boolean isFemale() {
        return !isMale();
    }

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
     * Format the height as feet'inches"
     * @return a formatted string, or null if height is null or impossibly small
     */
    public String getHeightString() {
        if (heightInches == null || heightInches < 5.0) {
            return null;
        }
        int feet = (int) Math.floor(heightInches / 12.0);
        int inches = (int) Math.round(heightInches - (feet * 12));
        if (12 <= inches) {
            feet++;
            inches = 0;
        }
        return feet + "'" + inches + "\"";
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
                period.getHousehold().endPersonResidence(this.getId(), fromDate);
            }
        }
        HouseholdInhabitantPeriod newPeriod = new HouseholdInhabitantPeriod();

        newPeriod.setHousehold(household);

        household.addInhabitantPeriod(newPeriod);

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

    /**
     * Add a title to the person's collection of titles (or update dates if he already holds it).
     *
     * @param title the title to add
     * @param fromDate the date when he obtained the title
     * @param toDate the date when he loses the title, or null to use his death date (or the current toDate if this
     *               is modifying an existing title)
     */
    public void addOrUpdateTitle(@NonNull Title title, @NonNull LocalDate fromDate, @Nullable LocalDate toDate) {
        List<PersonTitlePeriod> currentTitles = getTitles();
        PersonTitlePeriod existingTitle = currentTitles.stream()
                .filter(p -> p.getTitle().getId() == title.getId())
                .findFirst().orElse(null);
        if (existingTitle != null) {
            // We are updating an existing title assignment
            existingTitle.setFromDate(fromDate);
            if (toDate != null) {
                existingTitle.setToDate(toDate);
            }
        } else {
            // We are adding a new title
            PersonTitlePeriod newPeriod = new PersonTitlePeriod();
            newPeriod.setTitle(title);
            newPeriod.setTitleId(title.getId());
            newPeriod.setPerson(this);
            newPeriod.setFromDate(fromDate);
            newPeriod.setToDate(toDate == null ? getDeathDate() : toDate);
            getTitles().add(newPeriod);
        }

        // Update the person's social class to match his highest ranking title, if it's higher than his current social
        // class.
        Optional<PersonTitlePeriod> highestRankingTitle = getTitles().stream()
                .max(Comparator.comparing(t -> t.getTitle().getSocialClass().getRank()));
        if (highestRankingTitle.isPresent()) {
            SocialClass titleClass = highestRankingTitle.get().getTitle().getSocialClass();
            if (titleClass.getRank() > getSocialClass().getRank()) {
                setSocialClass(titleClass);
            }
        }
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

    /**
     * Gets an occupation that the person held on this date, if any. If he somehow held multiple occupations on the
     * date, returns an arbitrary one
     */
    @Nullable
    public Occupation getOccupation(@NonNull LocalDate onDate) {
        Set<Occupation> occupationsOnDate = getOccupations().stream()
                .filter(o -> o.contains(onDate))
                .map(PersonOccupationPeriod::getOccupation)
                .collect(Collectors.toSet());

        return occupationsOnDate.isEmpty() ? null : occupationsOnDate.iterator().next();
    }

    /**
     * Gets the household the person occupied on this date, if any. If he somehow had multiple households on that
     * date, returns an arbitrary one
     */
    @Nullable
    public Household getHousehold(@NonNull LocalDate onDate) {
        Set<Household> householdsOnDate = getHouseholds().stream()
                .filter(o -> o.containsDate(onDate))
                .map(HouseholdInhabitantPeriod::getHousehold)
                .collect(Collectors.toSet());

        return householdsOnDate.isEmpty() ? null : householdsOnDate.iterator().next();
    }

    public List<PersonTitlePeriod> getTitles(@NonNull LocalDate onDate) {
        return getTitles().stream()
                .filter(o -> o.contains(onDate))
                .collect(Collectors.toList());
    }
}
