package com.meryt.demographics.domain.person.fertility;

import java.time.LocalDate;
import com.meryt.demographics.domain.person.Person;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class Maternity extends Fertility {

    private LocalDate conceptionDate;
    private LocalDate dueDate;
    private boolean carryingIdenticalTwins;
    private boolean carryingFraternalTwins;
    private Person father;

    public boolean isPregnant(@NonNull LocalDate onDate) {
        return null != conceptionDate && conceptionDate.isBefore(onDate) && null != dueDate
                && (dueDate.isAfter(onDate) || dueDate.isEqual(onDate));
    }
}
