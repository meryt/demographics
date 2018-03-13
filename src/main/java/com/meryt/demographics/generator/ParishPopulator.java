package com.meryt.demographics.generator;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.generator.family.HouseholdGenerator;
import com.meryt.demographics.request.FamilyParameters;

@Slf4j
class ParishPopulator {

    private final HouseholdGenerator householdGenerator;

    ParishPopulator(@NonNull HouseholdGenerator householdGenerator) {
        this.householdGenerator = householdGenerator;
    }

    void populateParish(ParishTemplate template) {
        log.info("Beginning population of Parish " + template.getParish().getName());

        long currentPopulation = 0;
        while (currentPopulation < template.getExpectedTotalPopulation()) {
            currentPopulation += addHousehold(template.getFamilyParameters());
        }
    }

    /**
     * Generates a household with living people in it, as of the reference date. Adds the household to one of the towns
     * or to the parish.
     *
     * @return the number of living people in the household on the reference date
     */
    int addHousehold(@NonNull FamilyParameters familyParameters) {
        // FIXME actually do add the household!
        Household household = householdGenerator.generateHousehold(familyParameters);


        return household.getPopulation(familyParameters.getReferenceDate());
    }

}
