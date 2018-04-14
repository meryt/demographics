package com.meryt.demographics.response;

import java.time.LocalDate;
import java.time.Period;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

@Getter
public class PersonPotentialSpouseResponse {

    private final long id;
    private final String name;
    private final SocialClass socialClass;
    private final LocalDate birthDate;
    private final LocalDate deathDate;
    private final String ageDifference;
    //private final int degreesOfConsanguity;

    public PersonPotentialSpouseResponse(@NonNull Person person, @NonNull Person spouse) {
        id = spouse.getId();
        name = spouse.getName();
        birthDate = spouse.getBirthDate();
        deathDate = spouse.getDeathDate();
        socialClass = spouse.getSocialClass();
        Period ageDifference = Period.between(person.getBirthDate(), spouse.getBirthDate());
        if (Math.abs(ageDifference.getYears()) == 0) {
            this.ageDifference = "same age";
        } else if (ageDifference.isNegative()) {
            this.ageDifference = Math.abs(ageDifference.getYears()) + " years older";
        } else {
            this.ageDifference = ageDifference.getYears() + " years younger";
        }
        //degreesOfConsanguity = person.calculateDegreesOfConsanguinity(spouse);
    }


}
