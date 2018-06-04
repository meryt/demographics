package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "FARM")
public class Farm extends DwellingPlace {

    public Farm() {
        super();
        setType(DwellingPlaceType.FARM);
    }

}
