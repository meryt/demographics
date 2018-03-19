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

public class HouseholdResponse {

    @Getter
    private final long id;

    @Getter
    private DwellingPlaceReference location;
    @Getter
    private List<DwellingPlaceReference> locations;

    @Getter
    private PersonReference head;
    @Getter
    private List<PersonReference> inhabitants;

    HouseholdResponse(@NonNull Household household) {
        this(household, null);
    }

    public HouseholdResponse(@NonNull Household household, @Nullable LocalDate onDate) {
        id = household.getId();

        if (onDate == null) {
            populateDatelessData(household);
        } else {
            populateDatedData(household, onDate);
        }
    }

    private void populateDatelessData(@NonNull Household household) {
        List<HouseholdLocationPeriod> locationPeriods = household.getDwellingPlaces();
        if (!locationPeriods.isEmpty()) {
            locations = new ArrayList<>();
            for (HouseholdLocationPeriod period : locationPeriods) {
                locations.add(new DwellingPlaceReference(period.getDwellingPlace()));
            }
        }
    }

    private void populateDatedData(@NonNull Household household, @NonNull LocalDate onDate) {
        DwellingPlace place = household.getDwellingPlace(onDate);
        location = place == null ? null : new DwellingPlaceReference(place);

        Set<Person> people = household.getInhabitants(onDate);
        if (!people.isEmpty()) {
            inhabitants = new ArrayList<>();
            for (Person p : people.stream()
                    .sorted(Comparator.comparing(Person::getBirthDate))
                    .collect(Collectors.toList())) {
                inhabitants.add(new PersonReference(p, onDate));
            }

            Person hhHead = household.getHead(onDate);
            if (hhHead != null) {
                head = new PersonReference(hhHead, onDate);
            }
        }
    }

}
