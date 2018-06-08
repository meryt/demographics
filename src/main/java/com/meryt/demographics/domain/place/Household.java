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
        Set<DwellingPlace> places = getDwellingPlaces().stream()
                .filter(p -> p.contains(onDate))
                .map(HouseholdLocationPeriod::getDwellingPlace)
                .collect(Collectors.toSet());

        return places.isEmpty() ? null : places.iterator().next();
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
}
