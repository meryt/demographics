package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.response.PersonSummaryResponse;
import com.meryt.demographics.response.TitleReference;

@Getter
public class TitleAbeyanceEvent extends CalendarDayEvent {

    private final TitleReference title;
    private List<PersonSummaryResponse> possibleHeirs;

    public TitleAbeyanceEvent(@NonNull LocalDate date, @NonNull Title title, @NonNull List<Person> possibleHeirs) {
        super(date);
        setType(CalendarEventType.TITLE_IN_ABEYANCE);
        this.title = new TitleReference(title);
        this.possibleHeirs = possibleHeirs.stream()
                .map(p -> new PersonSummaryResponse(p, date))
                .collect(Collectors.toList());
    }

}
