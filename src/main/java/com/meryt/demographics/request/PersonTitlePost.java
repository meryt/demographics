package com.meryt.demographics.request;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.rest.BadRequestException;

@Getter
@Setter
public class PersonTitlePost {

    private Long titleId;
    private LocalDate fromDate;
    private LocalDate toDate;

    public void validate() {
        if (titleId == null) {
            throw new BadRequestException("titleId may not be null");
        }

        if (fromDate == null) {
            throw new BadRequestException("fromDate may not be null");
        }

        if (toDate != null && toDate.isBefore(fromDate)) {
            throw new BadRequestException("toDate may not be before fromDate");
        }
    }
}
