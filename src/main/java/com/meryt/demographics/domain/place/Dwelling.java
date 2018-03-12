package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A Dwelling is a house or other structure that one or more households lives in. It cannot contain
 * any other type of dwelling place.
 */
@Getter
@Setter
@Entity
@DiscriminatorValue(value = "DWELLING")
public class Dwelling extends DwellingPlace {

    @Override
    public void addDwellingPlace(@NonNull DwellingPlace newMember) {
        throw new IllegalArgumentException("A Dwelling can only contain Households");
    }
}
