package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.response.PersonReference;

@Getter
@Setter
public class ConceptionEvent extends CalendarDayEvent {

    private final PersonReference mother;
    private final PersonReference father;
    private final LocalDate dueDate;

    public ConceptionEvent(@NonNull LocalDate date,
                           @NonNull Person mother,
                           @NonNull Person father,
                           @NonNull LocalDate dueDate) {
        super(date);
        setType(CalendarEventType.CONCEPTION);
        this.mother = new PersonReference(mother, date);
        this.father = new PersonReference(father, date);
        this.dueDate = dueDate;
    }
}
