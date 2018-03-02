package com.meryt.demographics.domain.place;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuralArea implements DwellingPlace {

    private String name;
    private long population;

}
