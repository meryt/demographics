package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.Farm;
import com.meryt.demographics.response.DwellingPlaceDetailResponse;

@Getter
public class NewFarmEvent extends CalendarDayEvent {

    private DwellingPlaceDetailResponse dwellingPlace;

    public NewFarmEvent(@NonNull LocalDate date, @NonNull Farm farm) {
        super(date);
        dwellingPlace = new DwellingPlaceDetailResponse(farm, date, null);
        setType(CalendarEventType.NEW_FARM);
    }

    public String toLogMessage() {
        return String.format("%s - %d created on %s", getType().name(), dwellingPlace.getId(), getDate());
    }

}
