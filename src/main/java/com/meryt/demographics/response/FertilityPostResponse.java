package com.meryt.demographics.response;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.fertility.Fertility;
import com.meryt.demographics.response.calendar.CalendarDayEvent;

@Getter
public class FertilityPostResponse {
    private final FertilityResponse fertility;
    private final List<CalendarDayEvent> events;

    public FertilityPostResponse(@NonNull Fertility fertility, @NonNull List<CalendarDayEvent> events) {
        this.fertility = new FertilityResponse(fertility);
        this.events = events;
    }
}
