package com.meryt.demographics.response;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.place.DwellingPlace;

@Getter
public class DwellingPlaceReference extends DwellingPlacePointer {

    private Double value;
    private Boolean entailed;
    private final Double acres;
    private final Double squareMiles;
    private final String location;
    private final DwellingPlacePointer parent;
    private LocalDate foundedDate;
    private LocalDate ruinedDate;
    private final String mapId;

    public DwellingPlaceReference(@NonNull DwellingPlace dwellingPlace) {
        super(dwellingPlace);
        acres = dwellingPlace.getAcres();
        squareMiles = dwellingPlace.getSquareMiles();
        value = dwellingPlace.getValue();
        entailed = dwellingPlace.isEntailed() ? true : null;
        foundedDate = dwellingPlace.getFoundedDate();
        ruinedDate = dwellingPlace.getRuinedDate();
        mapId = dwellingPlace.getMapId();

        if (dwellingPlace.getParent() == null) {
            location = null;
            parent = null;
        } else {
            DwellingPlace parentPlace = dwellingPlace.getParent();
            StringBuilder bld = new StringBuilder();
            bld.append(parentPlace.getName());
            while ((parentPlace = parentPlace.getParent()) != null) {
                bld.append(", ").append(parentPlace.getName());
            }
            location = bld.toString();
            parent = new DwellingPlacePointer(dwellingPlace.getParent());
        }
    }

}
