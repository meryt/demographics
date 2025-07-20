package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.time.LocalDateComparator;

@Getter
@Setter
@Entity
@Table(name = "households")
public class Household {

    @Id
    @SequenceGenerator(name="households_id_seq", sequenceName="households_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="households_id_seq")
    private long id;

    private String name;

    /**
     * A list of all the people who have ever lived in the household, over time
     */
    @OneToMany(mappedBy = "household", cascade = { CascadeType.MERGE })
    private List<HouseholdInhabitantPeriod> inhabitantPeriods = new ArrayList<>();


    /**
     * A list of the dwelling places the household has lived, over time
     */
    @OneToMany(mappedBy = "household", cascade = { CascadeType.MERGE })
    private List<HouseholdLocationPeriod> dwellingPlaces = new ArrayList<>();

    /**
     * Get the number of people in this household on the given date. Does not check aliveness of the persons. (Dead or
     * unborn people should not be in households, however).
     */
    public int getPopulation(@NonNull LocalDate onDate) {
        return getInhabitants(onDate).size();
    }

    /**
     * Gets the name of the household, if set, otherwise names the household after the head
     * @return String
     */
    @NonNull
    public String getFriendlyName(@NonNull LocalDate onDate) {
        if (name != null) {
            return name;
        }
        Person head = getHead(onDate);
        if (head != null) {
            return String.format("Household %d of %s", id, head.getIdAndName());
        } else {
            return "Household " + id;
        }
    }

    /**
     * Get the head of the household on the given date, if any. If there is more than one (which would be considered a
     * bug), returns an arbitrary one.
     */
    @JsonIgnore
    @Nullable
    public Person getHead(@NonNull LocalDate onDate) {
        Set<HouseholdInhabitantPeriod> inhabitants = getHouseholdInhabitants(onDate);
        for (HouseholdInhabitantPeriod inhabitantPeriod : inhabitants) {
            if (inhabitantPeriod.isHouseholdHead()) {
                return inhabitantPeriod.getPerson();
            }
        }
        return null;
    }

    private Set<HouseholdInhabitantPeriod> getHouseholdInhabitants(@NonNull LocalDate onDate) {
        return getInhabitantPeriods().stream()
                .filter(period -> period.contains(onDate))
                .collect(Collectors.toSet());
    }

    public Set<Person> getInhabitants(@NonNull LocalDate onDate) {
        return getHouseholdInhabitants(onDate).stream()
            .map(HouseholdInhabitantPeriod::getPerson)
            .collect(Collectors.toSet());
    }

    @Nullable
    public DwellingPlace getDwellingPlace(@NonNull LocalDate onDate) {
        return getDwellingPlaces().stream()
                .filter(p -> p.contains(onDate))
                .map(HouseholdLocationPeriod::getDwellingPlace)
                .findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public List<HouseholdLocationPeriod> getLocations(@NonNull LocalDate fromDate, @Nullable LocalDate toDate) {
        return (List<HouseholdLocationPeriod>) LocalDateComparator.getRangesWithinRange(dwellingPlaces, fromDate, toDate);
    }

    /**
     * Determines whether the household lives in a house on the date. If the household does not live anywhere, also
     * returns false.
     *
     * @param onDate the date to check
     * @return true if the household has a dwelling place on the date which is of type DWELLING; false if they do not
     * have a dwelling place at all or if it is not a house
     */
    public boolean dwellsInHouse(@NonNull LocalDate onDate) {
        DwellingPlace place = getDwellingPlace(onDate);
        return place != null && place.isHouse();
    }

    public void addInhabitantPeriod(@NonNull HouseholdInhabitantPeriod newPeriod) {
        for (HouseholdInhabitantPeriod period : getInhabitantPeriods()) {
            if (period.getPersonId() == newPeriod.getPersonId() &&
                    period.getFromDate().isBefore(newPeriod.getFromDate()) &&
                    (period.getToDate() == null || period.getToDate().isAfter(newPeriod.getFromDate()))) {

                period.setToDate(newPeriod.getFromDate());
            }

        }
        getInhabitantPeriods().add(newPeriod);
    }

    @Nullable
    public HouseholdLocationPeriod getHouseholdLocationPeriod(@NonNull LocalDate onDate) {
        return getDwellingPlaces().stream()
                .filter(hip -> hip.contains(onDate))
                .findFirst().orElse(null);
    }

    /**
     * Get the max social class of any resident, or null if there are no residents on this date
     */
    @Nullable
    public SocialClass getMaxSocialClass(@NonNull LocalDate onDate) {
        return getInhabitants(onDate).stream()
                .map(Person::getSocialClass)
                .max(Comparator.comparing(SocialClass::getRank))
                .orElse(null);
    }

    @NonNull
    public SocialClass getMaxSocialClassOrDefault(@NonNull LocalDate onDate, @NonNull SocialClass defaultClass) {
        SocialClass maxClass = getMaxSocialClass(onDate);
        return maxClass == null ? defaultClass : maxClass;
    }

    private boolean hasInhabitantOfAtLeastClass(@NonNull LocalDate onDate, @NonNull SocialClass socialClass) {
        SocialClass maxClass = getMaxSocialClass(onDate);
        return maxClass != null && socialClass.getRank() <= maxClass.getRank();
    }

    @NonNull
    public Double getCapital(@NonNull LocalDate onDate) {
        return getInhabitants(onDate).stream()
                .mapToDouble(p -> p.getCapitalNullSafe(onDate))
                .sum();
    }

    /**
     * Get the change in capital of the inhabitants living in the household (on toDate), from fromDate to toDate.
     *
     * This is not the same as calling getCapital on the different dates, since the inhabitants may have changed in
     * between the dates.
     *
     * @param onDate look at the people living in the household on this date, and find out their income on this date
     */
    public double getIncome(@NonNull LocalDate onDate) {
        return getInhabitants(onDate).stream()
                .mapToDouble(p -> p.getIncomeOrProjectedIncome(onDate))
                .sum();
    }

    public boolean mayHireServants(@NonNull LocalDate onDate) {
        return hasInhabitantOfAtLeastClass(onDate, SocialClass.YEOMAN_OR_MERCHANT)
                && !hasInhabitantThatIsDomesticServant(onDate);
    }

    private boolean hasInhabitantThatIsDomesticServant(@NonNull LocalDate onDate) {
        return getInhabitants(onDate).stream()
                .anyMatch(p -> p.isDomesticServant(onDate));
    }
}
