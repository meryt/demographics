package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.response.TitleReference;

@Getter
public class TitleCreationEvent extends CalendarDayEvent {

    private final TitleReference title;

    public TitleCreationEvent(@NonNull LocalDate date, @NonNull Title title) {
        super(date);
        setType(CalendarEventType.TITLE_CREATION);
        this.title = new TitleReference(title);
    }

}
