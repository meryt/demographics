package com.meryt.demographics.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.rest.BadRequestException;

/**
 * A request that lets you either advance to a specific date or advance a specific number of days. Only one can be
 * selected.
 */
@Getter
@Setter
public class AdvanceToDatePost {
    private LocalDate date;
    private Integer advanceDays;
    private RandomFamilyParameters familyParameters;

    public void validate() {
        if (date == null && advanceDays == null) {
            throw new BadRequestException("One of date or advanceDays must be specified");
        }
        if (date != null && advanceDays != null) {
            throw new BadRequestException("Only one of date or advanceDays may be specified, not both");
        }
        if (advanceDays != null && advanceDays < 0) {
            throw new BadRequestException("advanceDays must be 0 or a positive integer");
        }
    }
}
