package com.meryt.demographics.domain.place;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Town implements DwellingPlace {

    private String name;
    private long population;
}
