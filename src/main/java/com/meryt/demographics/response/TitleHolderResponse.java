package com.meryt.demographics.response;

import java.time.LocalDate;

import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.service.AncestryService;

@Getter
class TitleHolderResponse {

    private final PersonReference titleHolder;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final Relationship relationshipToPrevious;

    TitleHolderResponse(@NonNull PersonTitlePeriod personTitlePeriod,
                        @Nullable Person previousHolder,
                        @Nullable AncestryService ancestryService) {
        titleHolder = new PersonReference(personTitlePeriod.getPerson());
        fromDate = personTitlePeriod.getFromDate();
        toDate = personTitlePeriod.getToDate();
        if (previousHolder == null || ancestryService == null) {
            relationshipToPrevious = null;
        } else {
            relationshipToPrevious = ancestryService.calculateRelationship(personTitlePeriod.getPerson(), previousHolder, true);
        }
    }
}
