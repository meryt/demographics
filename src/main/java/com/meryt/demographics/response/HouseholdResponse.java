package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.service.AncestryService;

@Getter
class HouseholdResponse {

    @Getter
    private final long id;

    @Getter
    private PersonReference head;
    @Getter
    private List<PersonReference> inhabitants;

    HouseholdResponse(@NonNull Household household,
                      @Nullable LocalDate onDate,
                      @Nullable AncestryService ancestryService) {
        id = household.getId();

        if (onDate != null) {
            Set<Person> people = household.getInhabitants(onDate);
            if (!people.isEmpty()) {
                Person hhHead = household.getHead(onDate);
                if (hhHead != null) {
                    Relationship headRel = ancestryService != null
                            ? ancestryService.calculateRelationship(hhHead, hhHead)
                            : null;
                    head = new HouseholdInhabitantSummaryResponse(hhHead, onDate, headRel);
                }

                inhabitants = new ArrayList<>();
                for (Person p : people.stream()
                        .sorted(Comparator.comparing(Person::getBirthDate))
                        .filter(p -> p != hhHead)
                        .collect(Collectors.toList())) {
                    Relationship relToHead = (ancestryService != null && hhHead != null)
                            ? ancestryService.calculateRelationship(p, hhHead)
                            : null;
                    inhabitants.add(new HouseholdInhabitantSummaryResponse(p, onDate, relToHead));
                }
            }
        }
    }
}
