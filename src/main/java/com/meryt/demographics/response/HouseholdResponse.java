package com.meryt.demographics.response;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;

@Getter
public class HouseholdResponse {

    private final long id;

    private final DwellingPlaceSummaryResponse location;
    private final List<DwellingPlaceSummaryResponse> locations;

    private final PersonReference head;
    private final List<PersonReference> inhabitants;

    public HouseholdResponse(@NonNull Household household) {
        this(household, null);
    }

    public HouseholdResponse(@NonNull Household household, @Nullable LocalDate onDate) {
        id = household.getId();

        if (onDate == null) {
            location = null;
            List<HouseholdLocationPeriod> locationPeriods = household.getDwellingPlaces();
            if (locationPeriods.isEmpty()) {
                locations = null;
            } else {
                locations = new ArrayList<>();
                for (HouseholdLocationPeriod period : locationPeriods) {
                    locations.add(new DwellingPlaceSummaryResponse(period.getDwellingPlace()));
                }
            }
        } else {
            locations = null;
            DwellingPlace place = household.getDwellingPlace(onDate);
            location = place == null ? null : new DwellingPlaceSummaryResponse(place, onDate);
        }

        if (onDate == null) {
            inhabitants = null;
            head = null;
        } else {
            Set<Person> people = household.getInhabitants(onDate);
            if (!people.isEmpty()) {
                inhabitants = new ArrayList<>();
                for (Person p : people.stream()
                        .sorted(Comparator.comparing(Person::getBirthDate))
                        .collect(Collectors.toList())) {
                    inhabitants.add(new PersonReference(p, onDate));
                }

                head = new PersonReference(household.getHead(onDate), onDate);
            } else {
                inhabitants = null;
                head = null;
            }

        }
    }

}
