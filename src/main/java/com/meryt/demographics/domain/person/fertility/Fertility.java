package com.meryt.demographics.domain.person.fertility;

import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;

//@Entity
@Getter
@Setter
//@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
//@DiscriminatorColumn(name = "gender")
public abstract class Fertility {

    //@Id
    //@OneToOne
    //@JoinColumn(name = "person_id")
    private Person person;
    private double fertilityFactor;
}
