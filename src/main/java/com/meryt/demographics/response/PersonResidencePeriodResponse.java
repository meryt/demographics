package com.meryt.demographics.response;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.time.DateRange;

@Getter
@Setter
public class PersonResidencePeriodResponse implements DateRange {

    private PersonReference person;
    private LocalDate fromDate;
    private LocalDate toDate;

}
