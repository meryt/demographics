package com.meryt.demographics.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;

@Getter
public class DwellingPlaceResponse extends DwellingPlaceSummaryResponse {

    private List<DwellingPlaceSummaryResponse> places;

    public DwellingPlaceResponse(@NonNull DwellingPlace dwellingPlace) {
        super(dwellingPlace);

        if (dwellingPlace.getDwellingPlaces().isEmpty()) {
            places = null;
        } else {
            places = new ArrayList<>();
            for (DwellingPlace place : dwellingPlace.getDwellingPlaces()) {
                places.add(new DwellingPlaceSummaryResponse(place));
            }
        }
    }

}
