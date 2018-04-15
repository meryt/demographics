package com.meryt.demographics.response;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.Period;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Relationship;
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
    private final Relationship relationship;

    public PersonPotentialSpouseResponse(@NonNull Person person,
                                         @NonNull Person spouse,
                                         @Nullable Relationship relationship) {
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
        this.relationship = relationship;
    }


}
