package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "HOUSEHOLD")
public class Household extends DwellingPlace {

    @Override
    public void addDwellingPlace(@NonNull DwellingPlace newMember) {
        throw new IllegalArgumentException("Households cannot contain anything besides people");
    }

    @Override
    public long getPopulation(@NonNull LocalDate onDate) {
        // TODO need to load the living household members on that date
        // FIXME
        return 0;
    }

}
