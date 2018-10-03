package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.response.DwellingPlaceDetailResponse;

@Getter
public class NewHouseEvent extends CalendarDayEvent {

    private DwellingPlaceDetailResponse dwellingPlace;

    public NewHouseEvent(@NonNull LocalDate date, @NonNull DwellingPlace house) {
        super(date);
        dwellingPlace = new DwellingPlaceDetailResponse(house, date, null);
        setType(CalendarEventType.NEW_HOUSE);
    }

    public String toLogMessage() {
        return String.format("%s - %d created on %s", getType().name(), dwellingPlace.getId(), getDate());
    }

}
