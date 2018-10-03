package com.meryt.demographics.response;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;

@Getter
class HouseholdInhabitantSummaryResponse extends PersonSummaryResponse {

    private final Relationship relationshipToHead;

    HouseholdInhabitantSummaryResponse(@NonNull Person person,
                                              @Nullable LocalDate onDate,
                                              @Nullable Relationship relationshipToHead) {
        super(person, onDate);
        this.relationshipToHead = relationshipToHead;
    }
}
