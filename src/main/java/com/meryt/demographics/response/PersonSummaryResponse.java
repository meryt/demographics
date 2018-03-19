package com.meryt.demographics.response;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

@Getter
public class PersonSummaryResponse extends PersonReference {

    private SocialClass socialClass;

    private OccupationReference occupation;

    public PersonSummaryResponse(@NonNull Person person, @NonNull LocalDate onDate) {
        super(person, onDate);
        socialClass = person.getSocialClass();
        occupation = new OccupationReference(person.getOccupation(onDate));
    }
}
