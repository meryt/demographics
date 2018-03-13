package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Household {

    @Id
    private long id;

    private String name;

    /**
     * The place where the household is located. May be a Dwelling (a house), or an Estate or other type of place where
     * the household is effectively homeless or not yet assigned to a house.
     */
    @ManyToOne
    private DwellingPlace parent;

    /**
     * A list of all the people who have ever lived in the household, over time
     */
    @OneToMany(mappedBy = "householdId")
    private List<HouseholdInhabitantPeriod> inhabitantPeriods;

    public int getPopulation(@NonNull LocalDate onDate) {
        return (int) getInhabitantPeriods().stream()
                .filter(p -> (p.getFromDate().isEqual(onDate) || p.getFromDate().isBefore(onDate))
                        && (p.getToDate() == null || p.getToDate().isAfter(onDate)))
                .count();
    }

}
