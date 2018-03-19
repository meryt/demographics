package com.meryt.demographics.response;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

@Getter
class PersonSummaryResponse extends PersonReference {

    private SocialClass socialClass;

    private OccupationReference occupation;

    PersonSummaryResponse(@NonNull Person person, @NonNull LocalDate onDate) {
        super(person, onDate);
        socialClass = person.getSocialClass();
        Occupation personOcc = person.getOccupation(onDate);
        occupation = personOcc == null ? null : new OccupationReference(personOcc);
    }
}
