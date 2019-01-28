package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.response.PersonResponse;

@Getter
public class DeathEvent extends CalendarDayEvent {

    private final PersonResponse person;

    public DeathEvent(@NonNull LocalDate date, @NonNull Person person) {
        super(date);
        setType(CalendarEventType.DEATH);
        this.person = new PersonResponse(person, date);
    }

}
