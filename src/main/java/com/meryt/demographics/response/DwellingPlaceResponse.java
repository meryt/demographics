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
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Household;

@Getter
public class DwellingPlaceResponse extends DwellingPlaceSummaryResponse {

    private List<PersonReference> owners;

    private List<HouseholdSummaryResponse> leadingHouseholds;

    private List<DwellingPlaceChildSummaryResponse> places;

    private Map<String, List<PersonReference>> occupations;

    private List<HouseholdResponse> households;

    public DwellingPlaceResponse(@NonNull DwellingPlace dwellingPlace, @Nullable LocalDate onDate) {
        super(dwellingPlace, onDate);

        if (dwellingPlace.getDwellingPlaces().isEmpty()) {
            places = null;
        } else {
            places = new ArrayList<>();
            for (DwellingPlace place : dwellingPlace.getDwellingPlaces()) {
                places.add(new DwellingPlaceChildSummaryResponse(place, onDate));
            }
        }

        if (onDate != null) {
            List<Household> leadingList = dwellingPlace.getLeadingHouseholds(onDate, SocialClass.GENTLEMAN, true);
            if (!leadingList.isEmpty()) {
                leadingHouseholds = new ArrayList<>();
                for (Household household : leadingList) {
                    leadingHouseholds.add(new HouseholdSummaryResponse(household, onDate));
                }
            }

            Map<Occupation, List<Person>> peopleWithOccupations = dwellingPlace.getPeopleWithOccupations(onDate);
            if (!peopleWithOccupations.isEmpty()) {
                occupations = new TreeMap<>();
                for (Map.Entry<Occupation, List<Person>> entry : peopleWithOccupations.entrySet()) {
                    occupations.put(entry.getKey().getName(),
                            entry.getValue().stream()
                                    .map(p -> new PersonReference(p, onDate))
                                    .collect(Collectors.toList()));
                }
            }

            List<Person> owningPersons = dwellingPlace.getOwners(onDate);
            owners = owningPersons.isEmpty()
                    ? null
                    : owningPersons.stream().map(p -> new PersonReference(p, onDate)).collect(Collectors.toList());

            households = dwellingPlace.getHouseholds(onDate).stream()
                    .map(h -> new HouseholdResponse(h, onDate))
                    .collect(Collectors.toList());
        }
    }
}
