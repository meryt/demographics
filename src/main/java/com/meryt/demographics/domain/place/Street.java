package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "STREET")
public class Street extends DwellingPlace {

    @Override
    public void addDwellingPlace(@NonNull DwellingPlace newMember) {
        if (!(newMember instanceof Dwelling || newMember instanceof Estate)) {
            throw new IllegalArgumentException("A Street can only contain a Dwelling or Estate");
        }
        super.addDwellingPlace(newMember);
    }
}
