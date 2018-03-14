package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import java.util.ArrayList;
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

    public void addToDwellingPlace(@NonNull DwellingPlace dwellingPlace, @NonNull LocalDate fromDate, LocalDate toDate) {
        for (HouseholdLocationPeriod period : getDwellingPlaces()) {
            if (period.getFromDate().isBefore(fromDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(fromDate))) {
                period.setToDate(fromDate);
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

}
