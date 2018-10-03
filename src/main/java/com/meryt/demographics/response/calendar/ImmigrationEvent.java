package com.meryt.demographics.response.calendar;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.response.HouseholdResponseWithLocations;
import com.meryt.demographics.service.AncestryService;

@Getter
public class ImmigrationEvent extends CalendarDayEvent {

    private final HouseholdResponseWithLocations householdResponse;

    public ImmigrationEvent(@NonNull LocalDate date, @NonNull Household household, @NonNull AncestryService ancestryService) {
        super(date);
        householdResponse = new HouseholdResponseWithLocations(household, date, ancestryService);
        setType(CalendarEventType.IMMIGRATION);
    }
}
