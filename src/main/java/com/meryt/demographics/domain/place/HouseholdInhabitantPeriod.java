package com.meryt.demographics.domain.place;

import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;

@Entity
@IdClass(HouseholdInhabitantPK.class)
@Table(name = "household_inhabitants")
@Getter
@Setter
public class HouseholdInhabitantPeriod {

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "household_id", referencedColumnName = "id")
    private Household household;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "person_id", referencedColumnName = "id")
    private Person person;

    private LocalDate fromDate;

    private LocalDate toDate;

    private boolean isHouseholdHead;

}
