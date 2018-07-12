package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.response.DwellingPlaceResponse;
import com.meryt.demographics.response.PersonReference;

@Getter
public class PropertyTransferEvent extends CalendarDayEvent {

    private final DwellingPlaceResponse dwellingPlace;
    private final List<PersonReference> previousOwners;
    private final List<PersonReference> newOwners;


    public PropertyTransferEvent(@NonNull LocalDate date,
                                 @NonNull DwellingPlace dwellingPlace,
                                 @NonNull List<Person> previousOwners,
                                 @NonNull List<Person> newOwners) {
        super(date);
        setType(CalendarEventType.PROPERTY_TRANSFER);
        this.dwellingPlace = new DwellingPlaceResponse(dwellingPlace, date);
        this.previousOwners = previousOwners.stream().
                map(p -> new PersonReference(p, date))
                .collect(Collectors.toList());
        this.newOwners = newOwners.stream().
                map(p -> new PersonReference(p, date))
                .collect(Collectors.toList());
    }

    public String toLogMessage() {
        return String.format("%s - %s %d sold on %s", getType().name(), dwellingPlace.getType(),
                dwellingPlace.getId(), getDate());
    }

}
