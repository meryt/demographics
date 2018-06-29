package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.response.TitleResponse;

@Getter
public class TitleExtinctionEvent extends CalendarDayEvent {

    private final TitleResponse title;

    public TitleExtinctionEvent(@NonNull LocalDate date, @NonNull Title title) {
        super(date);
        setType(CalendarEventType.TITLE_EXTINCTION);
        this.title = new TitleResponse(title);
    }

}
