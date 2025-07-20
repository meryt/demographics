package com.meryt.demographics.domain.person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

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
