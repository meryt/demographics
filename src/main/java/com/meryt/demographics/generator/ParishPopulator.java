package com.meryt.demographics.generator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ParishPopulator {

    ParishPopulator() {

    }

    void populateParish(ParishTemplate template) {
        log.info("Beginning population of Parish " + template.getParish().getName());

        long currentPopulation = 0;
        while (currentPopulation < template.getExpectedTotalPopulation()) {
            currentPopulation += addHousehold();
        }
    }

    /**
     * Generates a household with living people in it, as of the reference date. Adds the household to one of the towns
     * or to the parish.
     *
     * @return the number of living people in the household on the reference date
     */
    int addHousehold() {
        // FIXME actually do add the household!
        return 1;
    }

}
