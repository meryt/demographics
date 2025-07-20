package com.meryt.demographics.domain.place;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
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
