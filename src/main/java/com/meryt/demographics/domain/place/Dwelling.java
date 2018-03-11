package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A Dwelling is a house or other structure that one or more households lives in.
 */
@Getter
@Setter
@Entity
@DiscriminatorValue(value = "DWELLING")
public class Dwelling extends DwellingPlace {

    @Override
    public void addDwellingPlace(@NonNull DwellingPlace newMember) {
        if (!(newMember instanceof Household)) {
            throw new IllegalArgumentException("A Dwelling can only contain Households");
        }
        this.getDwellingPlaces().add(newMember);
    }
}
