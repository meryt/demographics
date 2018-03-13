package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.generator.family.HouseholdGenerator;
import com.meryt.demographics.request.FamilyParameters;

@Slf4j
class ParishPopulator {

    private final HouseholdGenerator householdGenerator;

    ParishPopulator(@NonNull HouseholdGenerator householdGenerator) {
        this.householdGenerator = householdGenerator;
    }

    void populateParish(@NonNull ParishTemplate template) {
        log.info("Beginning population of Parish " + template.getParish().getName());

        long currentPopulation = 0;
        while (currentPopulation < template.getExpectedTotalPopulation()) {
            currentPopulation += addHousehold(template);
            log.info("Done producing a test household; exiting");
            break;
        }
    }

    /**
     * Generates a household with living people in it, as of the reference date. Adds the household to one of the towns
     * or to the parish.
     *
     * @return the number of living people in the household on the reference date
     */
    private int addHousehold(@NonNull ParishTemplate parishTemplate) {

        FamilyParameters familyParameters = parishTemplate.getFamilyParameters();

        Household household = householdGenerator.generateHousehold(familyParameters);

        Person person = household.getHead(parishTemplate.getFamilyParameters().getReferenceDate());
        boolean placedInTown = findTownForHouseholdHead(parishTemplate, household, person);

        if (!placedInTown) {
            addHouseholdToDwellingPlaceOnWeddingDate(parishTemplate.getParish(), household, getMoveInDate(person,
                    parishTemplate.getFamilyParameters().getReferenceDate()));
        }

        return household.getPopulation(familyParameters.getReferenceDate());
    }

    /**
     * The household head will try to find a job opening in a town whose upper social class range corresponds to
     * his social class. If no town has such an opening, null is returned. If an opening is found, the person's
     * occupation will be set and the town will be returned so his household can be added to it.
     *
     * @param household the household of which the head is looking for a job
     * @return true if a job was found and the household was added to a town
     */
    private boolean findTownForHouseholdHead(@NonNull ParishTemplate template,
                                             @NonNull Household household,
                                             Person person) {

        if (person == null || person.getSocialClass() == null) {
            return false;
        }

        SocialClass socialClass = person.getSocialClass();
        boolean anyJobsRemaining = false;
        for (TownTemplate townTemplate : template.getTowns()) {
            for (Map.Entry<Occupation, Integer> occupationSlot : townTemplate.getExpectedOccupations().entrySet()) {
                Occupation occupation = occupationSlot.getKey();
                if (personWillAcceptOccupation(person, occupation) && occupationSlot.getValue() > 0) {
                    log.info(String.format("%s (%s) will take a job in %s as a %s", person.getName(),
                            socialClass.name().toLowerCase(), townTemplate.getTown().getName(),
                            occupationSlot.getKey().getName()));

                    // TODO set the occupation on the person

                    if (occupationSlot.getValue() == 1) {
                        townTemplate.getExpectedOccupations().remove(occupation);
                    } else {
                        occupationSlot.setValue(occupationSlot.getValue() - 1);
                    }
                    addHouseholdToDwellingPlaceOnWeddingDate(townTemplate.getTown(), household,
                            getMoveInDate(person, template.getFamilyParameters().getReferenceDate()));

                    return true;
                }
            }
        }

        log.info(String.format("%s (%s) could not find a job in any town", person.getName(),
                socialClass.name().toLowerCase()));

        return false;
    }


    /**
     * Determines whether a person will accept an occupation on offer. The max social class for the occupation must
     * be the same as his occupation, and the gender must be allowed.
     */
    private boolean personWillAcceptOccupation(@NonNull Person person, @NonNull Occupation occupation) {
        return person.getSocialClass() == occupation.getMaxClass()
                && (!person.isMale() || occupation.isAllowMale())
                && (!person.isFemale() || occupation.isAllowFemale());
    }

    private void addHouseholdToDwellingPlaceOnWeddingDate(@NonNull DwellingPlace dwellingPlace,
                                                          @NonNull Household household,
                                                          @NonNull LocalDate moveInDate) {
        household.addToDwellingPlace(dwellingPlace, moveInDate, null);
    }

    private LocalDate getMoveInDate(Person person, @NonNull LocalDate referenceDate) {
        if (person == null || person.getFamilies().isEmpty()) {
            return referenceDate;
        } else {
            Family family = person.getFamilies().iterator().next();
            return family.getWeddingDate() == null ? referenceDate : family.getWeddingDate();
        }
    }
}
