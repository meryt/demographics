package com.meryt.demographics.response.calendar;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.response.FamilyResponse;

@Getter
@Setter
public class MarriageEvent extends CalendarDayEvent {

    private FamilyResponse family;

    public MarriageEvent(@NonNull LocalDate date, @NonNull Family family) {
        super(date);
        setType(CalendarEventType.MARRIAGE);
        setFamily(family);
    }

    public void setFamily(@NonNull Family family) {
        this.family = new FamilyResponse(family);
    }
}
