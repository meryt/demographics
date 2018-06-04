package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "STREET")
public class Street extends DwellingPlace {

    public Street() {
        super();
        setType(DwellingPlaceType.STREET);
    }

}
