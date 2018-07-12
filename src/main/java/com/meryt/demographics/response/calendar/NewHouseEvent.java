package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.response.DwellingPlaceResponse;

@Getter
public class NewHouseEvent extends CalendarDayEvent {

    private DwellingPlaceResponse dwellingPlace;

    public NewHouseEvent(@NonNull LocalDate date, @NonNull DwellingPlace house) {
        super(date);
        dwellingPlace = new DwellingPlaceResponse(house, date);
        setType(CalendarEventType.NEW_HOUSE);
    }

    public String toLogMessage() {
        return String.format("%s - %d created on %s", getType().name(), dwellingPlace.getId(), getDate());
    }

}
