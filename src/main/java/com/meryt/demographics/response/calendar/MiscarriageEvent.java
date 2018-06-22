package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.response.PersonResponse;

@Getter
public class MiscarriageEvent extends CalendarDayEvent {

    private final PersonResponse mother;

    public MiscarriageEvent(@NonNull LocalDate date, @NonNull Person person) {
        super(date);
        setType(CalendarEventType.MISCARRIAGE);
        this.mother = new PersonResponse(person);
    }

}
