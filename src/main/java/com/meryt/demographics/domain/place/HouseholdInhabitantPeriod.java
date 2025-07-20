package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.time.DateRange;

@Entity
@IdClass(HouseholdInhabitantPK.class)
@Table(name = "household_inhabitants")
@Getter
@Setter
public class HouseholdInhabitantPeriod implements DateRange {

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

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

}
