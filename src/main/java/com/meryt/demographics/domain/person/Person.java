package com.meryt.demographics.domain.person;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
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
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
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
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
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

    /**
     * For randomly generated persons, this flag can be set to indicate that no more work needs to be done on them.
     */
    private boolean finishedGeneration;

    /**
     * This flag can be used to indicate a person is a founder of a family
     */
    private boolean founder;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private Maternity maternity;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private Paternity paternity;

    @ManyToOne
    private Family family;

    @OneToMany(mappedBy = "husband")
    @Setter(AccessLevel.PRIVATE)
    @OrderBy("wedding_date")
    private List<Family> fatheredFamilies = new ArrayList<>();

    @OneToMany(mappedBy = "wife")
    @Setter(AccessLevel.PRIVATE)
    @OrderBy("wedding_date")
    private List<Family> motheredFamilies = new ArrayList<>();

    /**
     * A list of the households the person has been a part of, over time
     */
    @OneToMany(mappedBy = "person")
    private List<HouseholdInhabitantPeriod> households = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = { CascadeType.MERGE })
    private List<DwellingPlaceOwnerPeriod> ownedDwellingPlaces = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = { CascadeType.MERGE })
    private List<PersonOccupationPeriod> occupations = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = { CascadeType.ALL })
    @OrderBy("from_date")
    private List<PersonTitlePeriod> titles = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = { CascadeType.MERGE })
    @OrderBy("from_date")
    private List<PersonCapitalPeriod> capitalPeriods = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "person_traits",
            joinColumns = @JoinColumn(name = "person_id"),
            inverseJoinColumns = @JoinColumn(name = "trait_id")
    )
    @OrderBy("name")
    private Set<Trait> traits = new HashSet<>();

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
     * @return age in full years (may be negative if date is before birth date)
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

    /**
     * Determines whether the person is married on this date or gets married any time after this date. Useful for
     * determining whether a person is eligible to be married on a given date.
     *
     * @param onDate the date to check
     * @return true if any of the peron's wedding dates are after this date, or if the person's spouse for any previous
     * marriage is still living
     */
    public boolean isMarriedNowOrAfter(@NonNull LocalDate onDate) {
        for (Family fam : getFamilies()) {
            // If the wedding date is on or after the date
            if ((fam.getWeddingDate() != null
                    && (fam.getWeddingDate().isAfter(onDate) || fam.getWeddingDate().equals(onDate)))
            // Or the wedding date is in the past but the spouse is not dead on the date
                || (fam.isMarriage() &&
                    ((getGender() == Gender.MALE && fam.getWife().isLiving(onDate)) ||
                            (getGender() == Gender.FEMALE && fam.getHusband().isLiving(onDate))))) {
                return true;
            }
        }
        return false;
    }

    public boolean isMarried(@NonNull LocalDate onDate) {
        return getFamilies().stream()
                .anyMatch(f -> f.getWeddingDate() != null && f.getWeddingDate().isBefore(onDate) &&
                        ((getGender() == Gender.MALE && f.getWife().isLiving(onDate)) ||
                        (getGender() == Gender.FEMALE && f.getHusband().isLiving(onDate))));
    }

    /**
     * Determines whether this person had a spouse living when he or she died. Returns false if the person was never
     * married as well.
     */
    public boolean isSurvivedByASpouse() {
        return isMarriedNowOrAfter(getDeathDate());
    }

    public String getName() {
        return (firstName + " " + (lastName == null ? "" : lastName)).trim();
    }

    @Override
    public String toString() {
        String s = getId() + " " + getName();
        if (birthDate != null && deathDate != null) {
            s += " " + birthDate.getYear() + "-" + deathDate.getYear();
        }
        return s;
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
                return (husbandOnDate == null || husbandOnDate.getLastName() == null)
                        ? getLastName()
                        : husbandOnDate.getLastName();
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

    public boolean isSibling(@NonNull Person person) {
        if (getFamily() == null || person.getFamily() == null) {
            return false;
        }

        if (getFamily().getId() == person.getFamily().getId()) {
            return true;
        }

        // If they are not in the same family they may still be related if they have one parent in common
        Person myFather = getFather();
        Person theirFather = person.getFather();
        if (myFather != null && theirFather != null && myFather.getId() == theirFather.getId()) {
            return true;
        }

        Person myMother = getMother();
        Person theirMother = person.getMother();
        return myMother != null && theirMother != null && myMother.getId() == theirMother.getId();
    }

    public Person getFather() {
        if (getFamily() == null) {
            return null;
        } else {
            return getFamily().getHusband();
        }
    }

    public Person getMother() {
        if (getFamily() == null) {
            return null;
        } else {
            return getFamily().getWife();
        }
    }

    /**
     * Gets all children from this person's marriages. Excludes any children who do not have birth dates.
     */
    private List<Person> getLegitimateChildren() {
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
    public List<Family> getFamilies() {
        if (isMale()) {
            return getFatheredFamilies();
        } else {
            return getMotheredFamilies();
        }
    }

    /**
     * Gets all of the person's children across all their families
     */
    public List<Person> getChildren() {
        return getFamilies().stream()
                .map(Family::getChildren)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<Person> getSpouses() {
        List<Person> spouses = new ArrayList<>();
        for (Family fam : getFamilies()) {
            if (isMale() && fam.getWife() != null) {
                spouses.add(fam.getWife());
            } else if (isFemale() && fam.getHusband() != null) {
                spouses.add(fam.getHusband());
            }
        }
        return spouses;
    }

    public Person getSpouse(@NonNull LocalDate onDate) {
        return getFamilies().stream()
                .filter(f -> f.getWeddingDate() != null &&
                        (f.getWeddingDate().isEqual(onDate) || f.getWeddingDate().isBefore(onDate)))
                .sorted(Comparator.comparing(Family::getWeddingDate))
                .map(f -> isMale() ? f.getWife() : f.getHusband())
                .filter(p -> p.isLiving(onDate))
                .findFirst().orElse(null);
    }

    /**
     * Returns all children living on the date. Does not include children not yet born.
     */
    public List<Person> getLivingChildren(@NonNull LocalDate onDate) {
        return getChildren().stream()
                .filter(p -> p.isLiving(onDate))
                .collect(Collectors.toList());
    }

    private List<Family> getFatheredFamilies() {
        return Collections.unmodifiableList(fatheredFamilies);
    }

    private List<Family> getMotheredFamilies() {
        return Collections.unmodifiableList(motheredFamilies);
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
            title.getTitleHolders().add(newPeriod);
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
     * Gets a list, possibly empty, of the properties owned by this person on this date
     */
    @NonNull
    public List<DwellingPlace> getOwnedDwellingPlaces(@NonNull LocalDate onDate) {
        return getOwnedDwellingPlaces().stream()
                .filter(o -> o.contains(onDate))
                .map(DwellingPlaceOwnerPeriod::getDwellingPlace)
                .collect(Collectors.toList());
    }

    /**
     * Gets the household the person occupied on this date, if any. If he somehow had multiple households on that
     * date, returns an arbitrary one
     */
    @Nullable
    public Household getHousehold(@NonNull LocalDate onDate) {
        Set<Household> householdsOnDate = getHouseholds().stream()
                .filter(o -> o.contains(onDate))
                .map(HouseholdInhabitantPeriod::getHousehold)
                .collect(Collectors.toSet());

        return householdsOnDate.isEmpty() ? null : householdsOnDate.iterator().next();
    }

    public List<PersonTitlePeriod> getTitles(@NonNull LocalDate onDate) {
        return getTitles().stream()
                .filter(o -> o.contains(onDate))
                .collect(Collectors.toList());
    }

    /**
     * Gets the person's place of residence on the given date. If he does not have a household on the date, or the
     * household does not have a dwelling place on the date, then returns null. The result is most likely to be a
     * Dwelling but may not be.
     *
     * @param onDate the date on which to look up residence
     * @return a DwellingPlace (likely a Dwelling) or null
     */
    @Nullable
    public DwellingPlace getResidence(@NonNull LocalDate onDate) {
        Household household = getHousehold(onDate);
        if (household == null) {
            return null;
        }
        return household.getDwellingPlace(onDate);
    }

    /**
     * Gets a person's capital in cash. May be negative if he is in debt. May be null if he has no record of having
     * capital. This may be treated as 0.0, but is left as null to distinguish from someone with exactly 0.0.
     *
     * @param onDate the date on which to check
     * @return a single Double value reflecting his cash resources on that date, or null if there is no record of him
     * having any on that date.
     */
    public Double getCapital(@NonNull LocalDate onDate) {
        PersonCapitalPeriod period = getCapitalPeriod(onDate);
        return period == null ? null : period.getCapital();
    }

    public PersonCapitalPeriod getCapitalPeriod(@NonNull LocalDate onDate) {
        return getCapitalPeriods().stream()
                .filter(o -> o.contains(onDate))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets the person's capital as of the given date. Closes off any existing capital period.
     *
     * @param capital the new amount of money he should have
     * @param asOfDate the date as of which this amount should take effect
     */
    public void setCapital(double capital, @NonNull LocalDate asOfDate) {
        PersonCapitalPeriod newPeriod = new PersonCapitalPeriod();
        newPeriod.setCapital(capital);
        newPeriod.setPerson(this);
        newPeriod.setPersonId(getId());
        newPeriod.setFromDate(asOfDate);
        newPeriod.setToDate(getDeathDate());
        List<PersonCapitalPeriod> existingPeriods = getCapitalPeriods().stream()
                .filter(p -> p.contains(asOfDate) || p.getFromDate().isAfter(asOfDate))
                .sorted(Comparator.comparing(PersonCapitalPeriod::getFromDate))
                .collect(Collectors.toList());
        if (!existingPeriods.isEmpty()) {
            existingPeriods.get(0).setToDate(asOfDate);
            if (existingPeriods.size() > 1) {
                newPeriod.setToDate(existingPeriods.get(1).getFromDate());
            }
        }
        getCapitalPeriods().add(newPeriod);
    }

    public void addCapital(double capital, @NonNull LocalDate onDate) {
        Double currentCapital = getCapital(onDate);

        setCapital((currentCapital == null ? 0 : currentCapital) + capital, onDate);
    }

    public Double getTotalWealth(@NonNull LocalDate onDate) {
        Double cashWealth = getCapital(onDate);
        List<DwellingPlace> realEstate = getOwnedDwellingPlaces(onDate);
        double realEstateValue = realEstate.stream()
                .mapToDouble(d -> d.getValue() == null ? 0.0 : d.getValue())
                .sum();
        return (cashWealth == null ? 0.0 : cashWealth) + realEstateValue;
    }
}
