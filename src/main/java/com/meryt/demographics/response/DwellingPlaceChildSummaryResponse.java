package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlace;

@Getter
class DwellingPlaceChildSummaryResponse extends DwellingPlacePointer {

    private Double value;
    private LocalDate foundedDate;
    private Boolean entailed;
    private final Double acres;
    private final Double squareMiles;
    private final String location;
    private Long directPopulation;
    private Long totalPopulation;
    private final List<PersonSummaryResponse> owners;

    DwellingPlaceChildSummaryResponse(@NonNull DwellingPlace dwellingPlace, @Nullable LocalDate onDate) {
        super(dwellingPlace);
        value = dwellingPlace.getValue();
        acres = dwellingPlace.getAcres();
        squareMiles = dwellingPlace.getSquareMiles();
        entailed = dwellingPlace.isEntailed() ? true : null;
        foundedDate = dwellingPlace.getFoundedDate();

        if (dwellingPlace.getParent() == null) {
            location = null;
        } else {
            DwellingPlace parentPlace = dwellingPlace.getParent();
            StringBuilder bld = new StringBuilder();
            bld.append(parentPlace.getName());
            while ((parentPlace = parentPlace.getParent()) != null) {
                bld.append(", ").append(parentPlace.getName());
            }
            location = bld.toString();
        }

        if (onDate != null) {
            List<Person> ownerPersons = dwellingPlace.getOwners(onDate);
            if (!ownerPersons.isEmpty()) {
                owners = ownerPersons.stream().map(p -> new PersonSummaryResponse(p, onDate)).collect(Collectors.toList());
            } else {
                owners = null;
            }

            long pop = dwellingPlace.getPopulation(onDate);
            long directPop = dwellingPlace.getDirectPopulation(onDate);
            totalPopulation = pop == 0 ? null : pop;
            directPopulation = directPop == 0 ? null : directPop;

        } else {
            owners = null;
        }
    }
}
