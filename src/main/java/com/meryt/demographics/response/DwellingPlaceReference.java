package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;

@Getter
public class DwellingPlaceReference {

    private final long id;
    private final String name;
    private final Double acres;
    private final String location;

    public DwellingPlaceReference(@NonNull DwellingPlace dwellingPlace) {
        id = dwellingPlace.getId();
        name = dwellingPlace.getName();
        acres = dwellingPlace.getAcres();

        if (dwellingPlace.getParent() == null) {
            location = null;
        } else {
            DwellingPlace parent = dwellingPlace.getParent();
            StringBuilder bld = new StringBuilder();
            bld.append(parent.getName());
            while ((parent = parent.getParent()) != null) {
                bld.append(", ").append(parent.getName());
            }
            location = bld.toString();
        }
    }

}
