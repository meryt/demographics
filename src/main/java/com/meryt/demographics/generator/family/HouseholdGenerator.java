package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.request.FamilyParameters;

/**
 * Can generate a household on a reference date such that at least one adult person is still alive on the date, and
 * there is a head of the household.
 */
@Slf4j
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
        Person founder = familyGenerator.generateFounder(familyParameters);
        Family family = familyGenerator.generate(founder, familyParameters);
        Household household = new Household();

        if (family != null) {
            addFamilyToHousehold(household, family, onDate);
        } else {
            founder.addToHousehold(household, founder.getBirthDate(), true);
        }

        List<String> inhabitants = household.getInhabitants(familyParameters.getReferenceDate())
                .stream().map(p -> p.getName() + " (" + p.getAgeInYears(onDate) + ")").collect(Collectors.toList());

        log.info(String.format("On %s the household contained %s", familyParameters.getReferenceDate(),
                String.join(", ", inhabitants)));

        return household;
    }

    private void addFamilyToHousehold(@NonNull Household household, @NonNull Family family, @NonNull LocalDate onDate) {
        LocalDate weddingDate = family.getWeddingDate();
        if (family.isHusbandLiving(onDate)) {
            family.getHusband().addToHousehold(household,
                    weddingDate != null ? weddingDate : family.getHusband().getBirthDate(),
                    true);
        }
        if (family.isWifeLiving(onDate)) {
            family.getWife().addToHousehold(household,
                    weddingDate != null ? weddingDate : family.getHusband().getBirthDate(),
                    !family.getHusband().isLiving(onDate));
        }
        for (Person child : family.getChildren()) {
            if (child.isLiving(onDate)) {
                child.addToHousehold(household, child.getBirthDate(), false);
            }
        }
    }

}
