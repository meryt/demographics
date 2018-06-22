package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.response.PersonReference;
import com.meryt.demographics.response.PersonResponse;

@Getter
public class BirthEvent extends CalendarDayEvent {

    private final PersonResponse person;
    private final PersonReference father;
    private final PersonReference mother;

    public BirthEvent(@NonNull LocalDate date, @NonNull Person person, @Nullable Person father, @Nullable Person mother) {
        super(date);
        setType(CalendarEventType.BIRTH);
        this.person = new PersonResponse(person);
        this.father = father == null ? null : new PersonReference(father, date);
        this.mother = mother == null ? null : new PersonReference(mother, date);
    }

}
