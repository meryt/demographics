package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Family;

@Getter
public class FamilyResponse extends AbstractFamilyResponse {

    private final PersonReference husband;
    private final PersonReference wife;
    private String husbandAgeAtMarriage;
    private String wifeAgeAtMarriage;

    public FamilyResponse(@NonNull Family family) {
        super(family);
        husband = family.getHusband() == null ? null : new PersonReference(family.getHusband());
        wife = family.getWife() == null ? null : new PersonReference(family.getWife());
        husbandAgeAtMarriage = family.getHusbandAgeAtMarriage();
        wifeAgeAtMarriage = family.getWifeAgeAtMarriage();
    }
}
