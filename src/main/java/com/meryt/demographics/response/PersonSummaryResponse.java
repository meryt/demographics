package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

@Getter
public class PersonSummaryResponse extends PersonReference {

    private SocialClass socialClass;

    private List<TitleReference> titles;

    private OccupationReference occupation;

    public PersonSummaryResponse(@NonNull Person person, @NonNull LocalDate onDate) {
        super(person, onDate);
        socialClass = person.getSocialClass();
        Occupation personOcc = person.getOccupation(onDate);
        occupation = personOcc == null ? null : new OccupationReference(personOcc);
        if (!person.getTitles(onDate).isEmpty()) {
            titles = person.getTitles(onDate).stream()
                    .map(t -> new TitleReference(t.getTitle()))
                    .collect(Collectors.toList());
        } else {
            titles = null;
        }
    }
}
