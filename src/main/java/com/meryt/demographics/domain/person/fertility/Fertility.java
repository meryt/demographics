package com.meryt.demographics.domain.person.fertility;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;

@Getter
@Setter
@MappedSuperclass
public abstract class Fertility {

    @Id
    @Column(name = "id")
    private long id;

    @OneToOne
    @JoinColumn(name = "person_id", referencedColumnName = "id")
    private Person person;

    private double fertilityFactor;
}
