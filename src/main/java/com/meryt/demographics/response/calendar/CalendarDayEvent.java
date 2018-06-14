package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class CalendarDayEvent {

    private CalendarEventType type;

    private final LocalDate date;

    public CalendarDayEvent(@NonNull LocalDate date) {
        this.date = date;
    }

}
