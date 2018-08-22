package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.response.PersonResponse;
import com.meryt.demographics.response.TitleResponse;

@Getter
public class TitleInheritanceEvent extends CalendarDayEvent {

    private final TitleResponse title;
    private final PersonResponse person;
    @JsonIgnore
    private final Person personRecord;

    public TitleInheritanceEvent(@NonNull LocalDate date, @NonNull Title title, @NonNull Person person) {
        super(date);
        setType(CalendarEventType.TITLE_INHERITED);
        this.title = new TitleResponse(title);
        this.person = new PersonResponse(person);
        this.personRecord = person;
    }

}
