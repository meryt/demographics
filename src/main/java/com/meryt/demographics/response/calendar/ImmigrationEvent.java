package com.meryt.demographics.response.calendar;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.response.HouseholdResponseWithLocations;

@Getter
public class ImmigrationEvent extends CalendarDayEvent {

    private final HouseholdResponseWithLocations householdResponse;

    public ImmigrationEvent(@NonNull LocalDate date, @NonNull Household household) {
        super(date);
        householdResponse = new HouseholdResponseWithLocations(household, date);
        setType(CalendarEventType.IMMIGRATION);
    }
}
