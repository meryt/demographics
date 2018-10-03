package com.meryt.demographics.response;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.PersonCapitalPeriod;

@Getter
public class PersonCapitalResponse {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final double capital;
    private final String reason;

    public PersonCapitalResponse(@NonNull PersonCapitalPeriod period) {
        this.fromDate = period.getFromDate();
        this.toDate = period.getToDate();
        this.capital = period.getCapital();
        this.reason = period.getReason();
    }

}
