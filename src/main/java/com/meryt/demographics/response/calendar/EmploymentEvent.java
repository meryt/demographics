package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.response.OccupationReference;
import com.meryt.demographics.response.PersonResponse;

@Getter
public class EmploymentEvent extends CalendarDayEvent {

    private final PersonResponse person;
    private final OccupationReference occupation;

    public EmploymentEvent(@NonNull LocalDate date, @NonNull Person person, @NonNull Occupation occupation) {
        super(date);
        setType(CalendarEventType.EMPLOYMENT);
        this.person = new PersonResponse(person, date);
        this.occupation = new OccupationReference(occupation);
    }
}
