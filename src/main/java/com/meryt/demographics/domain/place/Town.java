package com.meryt.demographics.domain.place;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "TOWN")
public class Town extends DwellingPlace {

    @Override
    public void addDwellingPlace(@NonNull DwellingPlace newMember) {
        if (newMember instanceof Parish) {
            throw new IllegalArgumentException("A Town cannot contain a Parish");
        }
        this.getDwellingPlaces().add(newMember);
    }

}
