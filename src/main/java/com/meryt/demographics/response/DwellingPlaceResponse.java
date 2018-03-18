package com.meryt.demographics.response;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;

@Getter
public class DwellingPlaceResponse extends DwellingPlaceSummaryResponse {

    private List<DwellingPlaceSummaryResponse> places;

    public DwellingPlaceResponse(@NonNull DwellingPlace dwellingPlace) {
        this(dwellingPlace, null);
    }

    public DwellingPlaceResponse(@NonNull DwellingPlace dwellingPlace, @Nullable LocalDate onDate) {
        super(dwellingPlace, onDate);

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
