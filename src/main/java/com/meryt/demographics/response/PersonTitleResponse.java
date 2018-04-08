package com.meryt.demographics.response;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.PersonTitlePeriod;

@Getter
public class PersonTitleResponse {

    private final TitleReference title;
    private final LocalDate fromDate;
    private final LocalDate toDate;

    public PersonTitleResponse(@NonNull PersonTitlePeriod period) {
        this.title = new TitleReference(period.getTitle());
        this.fromDate = period.getFromDate();
        this.toDate = period.getToDate();
    }
}
