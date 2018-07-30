package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.Farm;
import com.meryt.demographics.response.DwellingPlaceResponse;

@Getter
public class NewFarmEvent extends CalendarDayEvent {

    private DwellingPlaceResponse dwellingPlace;

    public NewFarmEvent(@NonNull LocalDate date, @NonNull Farm farm) {
        super(date);
        dwellingPlace = new DwellingPlaceResponse(farm, date);
        setType(CalendarEventType.NEW_FARM);
    }

    public String toLogMessage() {
        return String.format("%s - %d created on %s", getType().name(), dwellingPlace.getId(), getDate());
    }

}
