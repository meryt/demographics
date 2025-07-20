package com.meryt.demographics.domain.place;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "TOWNSHIP")
public class Township extends DwellingPlace {

    public Township() {
        super();
        setType(DwellingPlaceType.TOWNSHIP);
    }

}
