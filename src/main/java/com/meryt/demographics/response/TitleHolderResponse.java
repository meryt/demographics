package com.meryt.demographics.response;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.PersonTitlePeriod;

@Getter
public class TitleHolderResponse {

    private final PersonReference titleHolder;
    private final LocalDate fromDate;
    private final LocalDate toDate;

    public TitleHolderResponse(@NonNull PersonTitlePeriod personTitlePeriod) {
        titleHolder = new PersonReference(personTitlePeriod.getPerson());
        fromDate = personTitlePeriod.getFromDate();
        toDate = personTitlePeriod.getToDate();
    }
}
