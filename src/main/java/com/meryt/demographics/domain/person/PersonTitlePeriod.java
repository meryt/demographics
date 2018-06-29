package com.meryt.demographics.domain.person;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.time.DateRange;

@Entity
@IdClass(PersonTitlePK.class)
@Table(name = "person_titles")
@Getter
@Setter
public class PersonTitlePeriod implements DateRange {
    @Id
    @Column(name = "title_id", updatable = false, insertable = false)
    private long titleId;

    @JsonIgnore
    @ManyToOne(cascade = { CascadeType.MERGE })
    @JoinColumn(name = "person_id", referencedColumnName = "id")
    private Person person;

    @ManyToOne(cascade = { CascadeType.MERGE })
    @MapsId("title_id")
    @PrimaryKeyJoinColumn(name = "title_id", referencedColumnName = "id")
    private Title title;

    @Id
    private LocalDate fromDate;

    private LocalDate toDate;

}
