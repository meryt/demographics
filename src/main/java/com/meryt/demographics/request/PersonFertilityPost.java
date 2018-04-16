package com.meryt.demographics.request;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.rest.BadRequestException;

@Getter
@Setter
public class PersonFertilityPost {

    private String cycleToDate;

    public LocalDate getCycleToDateAsDate() {
        if (cycleToDate == null) {
            return null;
        }
        try {
            return LocalDate.parse(cycleToDate);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid cycleToDate: " + e.getMessage());
        }
    }
}

