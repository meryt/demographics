package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Household {

    @Id
    private long id;

    private String name;

    /**
     * The place where the household is located. May be a Dwelling (a house), or an Estate or other type of place where
     * the household is effectively homeless or not yet assigned to a house.
     */
    @ManyToOne
    private DwellingPlace parent;

    public long getPopulation(@NonNull LocalDate onDate) {
        // TODO need to load the living household members on that date
        // FIXME
        return 0;
    }

}
