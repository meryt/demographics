package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.time.DateRange;

@Entity
@IdClass(HouseholdLocationPK.class)
@Table(name = "household_locations")
@Getter
@Setter
public class HouseholdLocationPeriod implements DateRange {

    @Id
    @Column(name = "household_id", updatable = false, insertable = false)
    private long householdId;

    @JsonIgnore
    @ManyToOne
    @MapsId("household_id")
    @JoinColumn(name = "household_id", referencedColumnName = "id")
    private Household household;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "dwelling_place_id", referencedColumnName = "id")
    private DwellingPlace dwellingPlace;

    @Id
    private LocalDate fromDate;

    private LocalDate toDate;

    public String getName() {
        Person head;
        Occupation occupation;
        if ((head = household.getHead(fromDate)) == null) {
            return "Household " + household.getId();
        } else {
            if ((occupation = head.getOccupation(fromDate)) != null) {
                return head.getName() + " (" + occupation.getName() + ") household";
            }
            return head.getName() + " household";
        }
    }
}
