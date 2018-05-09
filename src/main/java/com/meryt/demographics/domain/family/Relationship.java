package com.meryt.demographics.domain.family;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Relationship {
    private final String name;
    private final int degreeOfSeparation;
    private final String personVia;
    private final String relatedPersonVia;
}
