package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "ESTATE")
public class Estate extends DwellingPlace {

    public Estate() {
        super();
        setType(DwellingPlaceType.ESTATE);
    }

}
