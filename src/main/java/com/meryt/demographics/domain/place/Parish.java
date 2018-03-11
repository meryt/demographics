package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "PARISH")
public class Parish extends DwellingPlace {

    /**
     * A Parish can contain any type
     * @param newMember a DwellingPlace of a type appropriate to belong to this place
     */
    @Override
    public void addDwellingPlace(@NonNull DwellingPlace newMember) {
        this.getDwellingPlaces().add(newMember);
    }
}
