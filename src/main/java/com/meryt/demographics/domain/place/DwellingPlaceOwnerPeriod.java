package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import javax.persistence.CascadeType;
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

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.time.DateRange;

@Entity
@IdClass(DwellingPlaceOwnerPK.class)
@Table(name = "dwelling_place_owners")
@Getter
@Setter
public class DwellingPlaceOwnerPeriod implements DateRange {
    @Id
    @Column(name = "dwelling_place_id", updatable = false, insertable = false)
    private long dwellingPlaceId;

    @Id
    @Column(name = "person_id", updatable = false, insertable = false)
    private long personId;

    @Id
    private LocalDate fromDate;

    private LocalDate toDate;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @MapsId("dwelling_place_id")
    @JoinColumn(name = "dwelling_place_id", referencedColumnName = "id")
    private DwellingPlace dwellingPlace;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @MapsId("person_id")
    @JoinColumn(name = "person_id", referencedColumnName = "id")
    private Person owner;

}
