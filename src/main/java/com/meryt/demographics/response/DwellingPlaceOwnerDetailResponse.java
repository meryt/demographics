package com.meryt.demographics.response;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;

@Getter
class DwellingPlaceOwnerDetailResponse extends PersonReference {

    private final String acquired;

    DwellingPlaceOwnerDetailResponse(@NonNull Person person,
                                     @NonNull LocalDate onDate,
                                     @Nullable String reason) {
        super(person, onDate);
        this.acquired = reason;
    }

}
