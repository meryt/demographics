package com.meryt.demographics.response;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlace;

@Getter
public class PersonReferenceWithDwelling extends PersonReference {

    private final DwellingPlacePointer dwelling;

    public PersonReferenceWithDwelling(@NonNull Person person, @NonNull LocalDate onDate) {
        super(person, onDate);

        DwellingPlace place = person.getDwellingPlace(onDate);
        dwelling = place == null ? null : new DwellingPlacePointer(place);
    }

}
