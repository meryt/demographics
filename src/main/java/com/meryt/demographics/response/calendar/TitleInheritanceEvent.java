package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.response.PersonResponse;
import com.meryt.demographics.response.TitleReference;

@Getter
public class TitleInheritanceEvent extends CalendarDayEvent {

    private final TitleReference title;
    private final PersonResponse person;
    @JsonIgnore
    private final Person personRecord;

    public TitleInheritanceEvent(@NonNull LocalDate date, @NonNull Title title, @NonNull Person person) {
        super(date);
        setType(CalendarEventType.TITLE_INHERITED);
        this.title = new TitleReference(title);
        this.person = new PersonResponse(person);
        this.personRecord = person;
    }

}
