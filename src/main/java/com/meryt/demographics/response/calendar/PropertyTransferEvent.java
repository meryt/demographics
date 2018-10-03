package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.response.DwellingPlaceReference;

@Getter
public class PropertyTransferEvent extends CalendarDayEvent {

    private final DwellingPlaceReference dwellingPlace;

    public PropertyTransferEvent(@NonNull LocalDate date,
                                 @NonNull DwellingPlace dwellingPlace) {
        super(date);
        setType(CalendarEventType.PROPERTY_TRANSFER);
        this.dwellingPlace = new DwellingPlaceReference(dwellingPlace);
    }

    public String toLogMessage() {
        return String.format("%s - %s %d sold on %s", getType().name(), dwellingPlace.getType(),
                dwellingPlace.getId(), getDate());
    }

}
