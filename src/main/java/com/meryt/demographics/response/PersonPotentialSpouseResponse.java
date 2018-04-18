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
    private final double comeliness;
    private final String ageDifference;
    private final String ageOnSearchDate;
    private final Relationship relationship;

    public PersonPotentialSpouseResponse(@NonNull Person person,
                                         @NonNull Person spouse,
                                         @NonNull LocalDate searchDate,
                                         @Nullable Relationship relationship) {
        id = spouse.getId();
        name = spouse.getName();
        birthDate = spouse.getBirthDate();
        deathDate = spouse.getDeathDate();
        socialClass = spouse.getSocialClass();
        comeliness = spouse.getComeliness();
        Period rawAgeDiff = Period.between(person.getBirthDate(), spouse.getBirthDate());
        if (Math.abs(rawAgeDiff.getYears()) == 0) {
            this.ageDifference = "same age";
        } else if (rawAgeDiff.isNegative()) {
            this.ageDifference = Math.abs(rawAgeDiff.getYears()) + " years older";
        } else {
            this.ageDifference = rawAgeDiff.getYears() + " years younger";
        }
        ageOnSearchDate = spouse.getAge(searchDate);
        this.relationship = relationship;
    }


}
