package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;

@Entity
@IdClass(HouseholdInhabitantPK.class)
@Table(name = "household_inhabitants")
@Getter
@Setter
public class HouseholdInhabitantPeriod {

    @Id
    @Column(name = "person_id", updatable = false, insertable = false)
    private long personId;

    @JsonIgnore
    @ManyToOne
    @MapsId("person_id")
    @PrimaryKeyJoinColumn(name = "person_id", referencedColumnName = "id")
    private Person person;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "household_id", referencedColumnName = "id")
    private Household household;

    @Id
    private LocalDate fromDate;

    private LocalDate toDate;

    private boolean isHouseholdHead;

    /**
     * Determines whether the date range represented by this object contains the given date. The date range is
     * inclusive on the lower bound and exclusive on the upper.
     *
     * @param onDate the date to check
     * @return true if the date is contained in the range
     */
    public boolean containsDate(@NonNull LocalDate onDate) {
        return (fromDate.isEqual(onDate) || fromDate.isBefore(onDate)) && (toDate == null || toDate.isAfter(onDate));
    }

}
