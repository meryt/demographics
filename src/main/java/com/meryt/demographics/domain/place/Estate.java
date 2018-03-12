package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "ESTATE")
public class Estate extends DwellingPlace {

    @Override
    public void addDwellingPlace(@NonNull DwellingPlace newMember) {
        if (newMember instanceof Parish) {
            throw new IllegalArgumentException("An Estate cannot contain a Parish");
        }
        if (newMember instanceof Town) {
            throw new IllegalArgumentException("An Estate cannot contain a Town");
        }
        super.addDwellingPlace(newMember);
    }
}
