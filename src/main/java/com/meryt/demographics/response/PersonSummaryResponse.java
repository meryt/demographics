package com.meryt.demographics.response;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

@Getter
public class PersonSummaryResponse extends PersonReference {

    private SocialClass socialClass;

    private OccupationReference occupation;

    private String firstNameCulture;
    private String lastNameCulture;

    public PersonSummaryResponse(@NonNull Person person, @Nullable LocalDate onDate) {
        super(person, onDate);
        socialClass = person.getSocialClass();
        if (onDate != null) {
            Occupation personOcc = person.getOccupation(onDate);
            occupation = personOcc == null ? null : new OccupationReference(personOcc);
        }
        firstNameCulture = person.getFirstNameCulture();
        lastNameCulture = person.getLastNameCulture();
    }
}
