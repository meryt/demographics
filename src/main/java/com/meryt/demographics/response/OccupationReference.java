package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.Occupation;

@Getter
public class OccupationReference {

    public final long id;
    public final String name;

    public OccupationReference(@NonNull Occupation occupation) {
        id = occupation.getId();
        name = occupation.getName();
    }
}
