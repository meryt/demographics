package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.service.AncestryService;

@Getter
public class DwellingPlaceDetailResponse extends DwellingPlaceResponse {

    private List<DwellingPlaceChildSummaryResponse> places;

    private Map<String, List<PersonReferenceWithDwelling>> occupations;

    private List<HouseholdResponse> households;

    private List<DwellingPlaceOwnerResponse> owners;

    private List<PersonResponse> beautifulResidents;

    public DwellingPlaceDetailResponse(@NonNull DwellingPlace dwellingPlace,
                                       @Nullable final LocalDate onDate,
                                       @Nullable AncestryService ancestryService) {
        super(dwellingPlace, onDate);

        if (dwellingPlace.getDwellingPlaces().isEmpty()) {
            places = null;
        } else {
            places = new ArrayList<>();
            places = dwellingPlace.getDwellingPlaces().stream()
                    .sorted(Comparator.comparing(DwellingPlace::getNullSafeValue).reversed())
                    .map(dp -> new DwellingPlaceChildSummaryResponse(dp, onDate))
                    .collect(Collectors.toList());
        }

        owners = dwellingPlace.getOwnerPeriods().stream()
                .sorted(Comparator.comparing(DwellingPlaceOwnerPeriod::getFromDate)
                        .thenComparing(DwellingPlaceOwnerPeriod::getPersonId))
                .map(DwellingPlaceOwnerResponse::new)
                .collect(Collectors.toList());

        if (onDate != null) {
            Map<Occupation, List<Person>> peopleWithOccupations = dwellingPlace.getPeopleWithOccupations(onDate);
            if (!peopleWithOccupations.isEmpty()) {
                occupations = new TreeMap<>();
                for (Map.Entry<Occupation, List<Person>> entry : peopleWithOccupations.entrySet()) {
                    occupations.put(entry.getKey().getName(),
                            entry.getValue().stream()
                                    .map(p -> new PersonReferenceWithDwelling(p, onDate))
                                    .collect(Collectors.toList()));
                }
            }

            households = dwellingPlace.getHouseholds(onDate).stream()
                    .sorted(Comparator.comparing((Household h) ->
                            h.getMaxSocialClassOrDefault(onDate, SocialClass.PAUPER)).reversed())
                    .map(h -> new HouseholdResponse(h, onDate, ancestryService))
                    .collect(Collectors.toList());

            List<Person> beautifulPeople = dwellingPlace.getAllResidents(onDate).stream()
                    .filter(p -> p.getAgeInYears(onDate) >= 14 && p.getComeliness() >= 0.8)
                    .sorted(Comparator.comparing(Person::getComeliness).reversed())
                    .collect(Collectors.toList());
            if (beautifulPeople.size() > 5) {
                beautifulPeople = beautifulPeople.subList(0, 5);
            }
            if (!beautifulPeople.isEmpty()) {
                beautifulResidents = beautifulPeople.stream()
                        .map(p -> new PersonResponse(p, onDate))
                        .collect(Collectors.toList());
            }
        }
    }
}
