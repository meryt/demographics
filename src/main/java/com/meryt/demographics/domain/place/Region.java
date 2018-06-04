package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/**
 * A Region is a generic high-level region larger than a Parish, such as a country or state
 */
@Getter
@Setter
@Entity
@DiscriminatorValue(value = "REGION")
public class Region extends DwellingPlace {

    public Region() {
        super();
        setType(DwellingPlaceType.REGION);
    }

}
