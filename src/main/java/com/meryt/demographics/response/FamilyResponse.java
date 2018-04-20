package com.meryt.demographics.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Family;

@Getter
@JsonPropertyOrder({ "id", "weddingDate", "husband", "husbandAgeAtMarriage", "wife", "wifeAgeAtMarriage", "children" })
public class FamilyResponse extends AbstractFamilyResponse {

    private final PersonResponse husband;
    private final PersonResponse wife;
    private String husbandAgeAtMarriage;
    private String wifeAgeAtMarriage;

    public FamilyResponse(@NonNull Family family) {
        super(family);
        husband = family.getHusband() == null ? null : new PersonResponse(family.getHusband());
        wife = family.getWife() == null ? null : new PersonResponse(family.getWife());
        husbandAgeAtMarriage = family.getHusbandAgeAtMarriage();
        wifeAgeAtMarriage = family.getWifeAgeAtMarriage();
    }
}
