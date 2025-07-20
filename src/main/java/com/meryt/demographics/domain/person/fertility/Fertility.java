package com.meryt.demographics.domain.person.fertility;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;

@Getter
@Setter
@MappedSuperclass
public abstract class Fertility {

    @Id
    @Column(name="person_id")
    private long personId;

    @JsonIgnore
    @MapsId
    @OneToOne
    @JoinColumn(name = "person_id")   //same name as id @Column
    private Person person;

    private double fertilityFactor;

    public void setPerson(Person person) {
        this.person = person;
        if (person != null) {
            personId = person.getId();
        }
    }
}
