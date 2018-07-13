package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import javax.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    /**
     * This event needs a pointer to the child since he has not been saved when this object is constructed, so we can't
     * get his ID. We may need a reference to the person if the child dies at or soon after birth.
     */
    @JsonIgnore
    private final Person child;

    public BirthEvent(@NonNull LocalDate date, @NonNull Person person, @Nullable Person father, @Nullable Person mother) {
        super(date);
        setType(CalendarEventType.BIRTH);
        this.person = new PersonResponse(person);
        this.child = person;
        this.father = father == null ? null : new PersonReference(father, date);
        this.mother = mother == null ? null : new PersonReference(mother, date);
    }
}
