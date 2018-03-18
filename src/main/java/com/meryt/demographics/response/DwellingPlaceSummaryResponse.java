package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;

@Getter
public class DwellingPlaceSummaryResponse {
    private final long id;
    private final String name;
    private final Double acres;
    private String location;

    public DwellingPlaceSummaryResponse(@NonNull DwellingPlace dwellingPlace) {
        id = dwellingPlace.getId();
        name = dwellingPlace.getName();
        acres = dwellingPlace.getAcres();

        if (dwellingPlace.getParent() == null) {
            location = null;
        } else {
            DwellingPlace parent = dwellingPlace.getParent();
            String locationString = parent.getName();
            while ((parent = parent.getParent()) != null) {
                locationString += ", " + parent.getName();
            }
            location = locationString;
        }
    }
}
