package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlace;

@Getter
public class DwellingPlaceResponse extends DwellingPlaceSummaryResponse {

    private List<DwellingPlaceSummaryResponse> places;

    private Map<String, List<PersonReference>> occupations;

    public DwellingPlaceResponse(@NonNull DwellingPlace dwellingPlace, @Nullable LocalDate onDate) {
        super(dwellingPlace, onDate);

        if (dwellingPlace.getDwellingPlaces().isEmpty()) {
            places = null;
        } else {
            places = new ArrayList<>();
            for (DwellingPlace place : dwellingPlace.getDwellingPlaces()) {
                places.add(new DwellingPlaceSummaryResponse(place, onDate));
            }
        }

        Map<Occupation, List<Person>> peopleWithOccupations = dwellingPlace.getAllHouseholds(onDate).stream()
                .map(h -> h.getInhabitants(onDate))
                .flatMap(Collection::stream)
                .filter(p -> p.getOccupation(onDate) != null)
                .collect(Collectors.groupingBy(p -> p.getOccupation(onDate)));
        if (!peopleWithOccupations.isEmpty()) {
            occupations = new TreeMap<>();
            for (Map.Entry<Occupation, List<Person>> entry : peopleWithOccupations.entrySet()) {
                occupations.put(entry.getKey().getName(),
                        entry.getValue().stream()
                            .map(p -> new PersonReference(p, onDate))
                            .collect(Collectors.toList()));
            }
        }
    }
}
