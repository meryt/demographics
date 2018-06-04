package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;

@Getter
public class DwellingPlacePointer {
    private final long id;
    private final String name;
    private final String type;

    public DwellingPlacePointer(@NonNull DwellingPlace dwellingPlace) {
        id = dwellingPlace.getId();
        name = dwellingPlace.getName();
        type = dwellingPlace.getType().toString();
    }
}
