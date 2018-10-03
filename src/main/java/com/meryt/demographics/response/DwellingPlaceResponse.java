package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Household;

@Getter
public class DwellingPlaceResponse extends DwellingPlaceSummaryResponse {

    private List<PersonReference> currentOwners;

    private List<HouseholdSummaryResponse> leadingHouseholds;

    public DwellingPlaceResponse(@NonNull DwellingPlace dwellingPlace, @Nullable LocalDate onDate) {
        super(dwellingPlace, onDate);

        if (onDate != null) {
            List<Household> leadingList = dwellingPlace.getLeadingHouseholds(onDate, SocialClass.GENTLEMAN, true);
            if (!leadingList.isEmpty()) {
                leadingHouseholds = new ArrayList<>();
                for (Household household : leadingList) {
                    leadingHouseholds.add(new HouseholdSummaryResponse(household, onDate));
                }
            }

            List<Person> owningPersons = dwellingPlace.getOwners(onDate);
            currentOwners = owningPersons.isEmpty()
                    ? null
                    : owningPersons.stream().map(p -> new PersonReference(p, onDate)).collect(Collectors.toList());
        }

    }
}
