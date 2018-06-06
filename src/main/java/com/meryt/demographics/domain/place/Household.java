package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;

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
    @OneToMany(mappedBy = "household", cascade = { CascadeType.ALL })
    private List<HouseholdInhabitantPeriod> inhabitantPeriods = new ArrayList<>();


    /**
     * A list of the dwelling places the household has lived, over time
     */
    @OneToMany(mappedBy = "household", cascade = { CascadeType.ALL })
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
            return head.getName() + " household";
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
                .filter(period -> period.containsDate(onDate))
                .collect(Collectors.toSet());
    }

    public Set<Person> getInhabitants(@NonNull LocalDate onDate) {
        return getHouseholdInhabitants(onDate).stream()
            .map(HouseholdInhabitantPeriod::getPerson)
            .collect(Collectors.toSet());
    }

    @Nullable
    public DwellingPlace getDwellingPlace(@NonNull LocalDate onDate) {
        Set<DwellingPlace> places = getDwellingPlaces().stream()
                .filter(p -> p.contains(onDate))
                .map(HouseholdLocationPeriod::getDwellingPlace)
                .collect(Collectors.toSet());

        return places.isEmpty() ? null : places.iterator().next();
    }

    public void addToDwellingPlace(@NonNull DwellingPlace dwellingPlace, @NonNull LocalDate fromDate, LocalDate toDate) {
        for (HouseholdLocationPeriod period : getDwellingPlaces()) {
            if (period.getFromDate().isBefore(fromDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(fromDate))) {
                period.setToDate(fromDate);
            }  else if (period.getFromDate().equals(fromDate) && (
                    (period.getToDate() == null && toDate == null)
                    || (period.getToDate().equals(toDate)))) {
                // If the periods are identical, just change the dwelling place.
                period.setDwellingPlace(dwellingPlace);
                return;
            }
        }

        HouseholdLocationPeriod newPeriod = new HouseholdLocationPeriod();
        newPeriod.setHouseholdId(getId());
        newPeriod.setHousehold(this);
        newPeriod.setDwellingPlace(dwellingPlace);
        dwellingPlace.getHouseholdPeriods().add(newPeriod);
        newPeriod.setFromDate(fromDate);
        newPeriod.setToDate(toDate);
        getDwellingPlaces().add(newPeriod);
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

    /**
     * Tell this household that the person has moved. Cap his current residency period (if any)
     * @param personId the person
     * @param fromDate the date upon which the person moved to another household
     */
    public void endPersonResidence(long personId, @NonNull LocalDate fromDate) {
        for (HouseholdInhabitantPeriod period : getInhabitantPeriods()) {
            if (period.getPersonId() == personId &&
                    period.getFromDate().isBefore(fromDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(fromDate))) {

                period.setToDate(fromDate);
            }
        }
    }

    /**
     * Use this method to find and set a new head of household as of the given date. Normally used when the current
     * head of the household dies.
     *
     * @param onDate the date the previous head died or left
     */
    public void resetHeadAsOf(@NonNull LocalDate onDate) {
        List<Person> inhabitantsByAge = getInhabitants(onDate).stream()
                .filter(p -> p.getBirthDate() != null && p.getAgeInYears(onDate) >= 16)
                .sorted(Comparator.comparing(Person::getGender).thenComparing(Person::getBirthDate))
                .collect(Collectors.toList());
        if (inhabitantsByAge.isEmpty()) {
            return;
        }

        inhabitantsByAge.get(0).addToHousehold(this, onDate, true);
    }

}
