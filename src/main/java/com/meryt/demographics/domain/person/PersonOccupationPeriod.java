package com.meryt.demographics.domain.person;

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
import lombok.Setter;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.time.DateRange;

@Entity
@IdClass(PersonOccupationPK.class)
@Table(name = "person_occupations")
@Getter
@Setter
public class PersonOccupationPeriod implements DateRange {

    @Id
    @Column(name = "person_id", updatable = false, insertable = false)
    private long personId;

    @JsonIgnore
    @ManyToOne
    @MapsId("person_id")
    @PrimaryKeyJoinColumn(name = "person_id", referencedColumnName = "id")
    private Person person;

    @ManyToOne
    @JoinColumn(name = "occupation_id", referencedColumnName = "id")
    private Occupation occupation;

    @Id
    private LocalDate fromDate;

    private LocalDate toDate;

}
