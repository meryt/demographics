package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.request.FamilyParameters;

/**
 * Can generate a household on a reference date such that at least one adult person is still alive on the date, and
 * there is a head of the household.
 */
public class HouseholdGenerator {

    private final FamilyGenerator familyGenerator;

    public HouseholdGenerator(@NonNull FamilyGenerator familyGenerator) {
        this.familyGenerator = familyGenerator;
    }

    /**
     * Generate a household by generating a family with a living head on the reference date, and putting that family
     * in the household.
     *
     * @param familyParameters the parameters used to generate the family. The reference date must be set.
     * @return Household containing the living members of a Family (one or both spouses and their children)
     */
    public Household generateHousehold(@NonNull FamilyParameters familyParameters) {
        LocalDate onDate = familyParameters.getReferenceDate();
        if (onDate == null) {
            throw new IllegalArgumentException("Cannot generate a household for a null reference date");
        }
        Family family = familyGenerator.generate(familyParameters);
        Household household = new Household();
        if (family.getHusband().isLiving(onDate)) {
            // FIXME need to add members to household
            //household.a
        }

        // FIXME add wife and children
        return household;
    }

}
