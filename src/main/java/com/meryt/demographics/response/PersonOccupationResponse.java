package com.meryt.demographics.response;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.PersonOccupationPeriod;

@Getter
public class PersonOccupationResponse {

    private String name;
    private LocalDate fromDate;
    private LocalDate toDate;

    public PersonOccupationResponse(@NonNull PersonOccupationPeriod occupationPeriod) {
        this.name = occupationPeriod.getOccupation().getName();
        this.fromDate = occupationPeriod.getFromDate();
        this.toDate = occupationPeriod.getToDate();
    }

}
