package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "TOWN")
public class Town extends DwellingPlace {

    public Town() {
        super();
        setType(DwellingPlaceType.TOWN);
    }

}
