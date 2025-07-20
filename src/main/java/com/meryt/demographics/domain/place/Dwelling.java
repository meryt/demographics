package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;

/**
 * A Dwelling is a house or other structure that one or more households lives in. It cannot contain
 * any other type of dwelling place.
 */
@Getter
@Setter
@Entity
@DiscriminatorValue(value = "DWELLING")
public class Dwelling extends DwellingPlace {

    public Dwelling() {
        super();
        setType(DwellingPlaceType.DWELLING);
    }

    public List<Person> getDomesticServants(@NonNull LocalDate onDate) {
        return getAllResidents(onDate).stream()
                .filter(p -> p.isDomesticServant(onDate))
                .collect(Collectors.toList());
    }

    public boolean mayHireServants(@NonNull LocalDate onDate) {
        return getHouseholds(onDate).stream().anyMatch(hh -> hh.mayHireServants(onDate));
    }
}
