package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.family.HouseholdGenerator;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.HouseholdService;
import com.meryt.demographics.service.PersonService;

@Slf4j
class ParishPopulator {

    private final HouseholdGenerator householdGenerator;

    private final FamilyGenerator familyGenerator;

    private final FamilyService familyService;

    private final HouseholdService householdService;

    private final PersonService personService;

    ParishPopulator(@NonNull HouseholdGenerator householdGenerator,
                    @NonNull FamilyGenerator familyGenerator,
                    @NonNull FamilyService familyService,
                    @NonNull HouseholdService householdService,
                    @NonNull PersonService personService) {
        this.householdGenerator = householdGenerator;
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.householdService = householdService;
        this.personService = personService;
    }

    void populateParish(@NonNull ParishTemplate template) {
        log.info("Beginning population of Parish " + template.getParish().getName());

        long currentPopulation = 0;
        int index = 0;
        while (currentPopulation < template.getExpectedTotalPopulation() && index < 10) {
            currentPopulation += addHousehold(template);
            index++;
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

        // If persist is true on the family template, the household and its inhabitants will be saved.
        Household household = householdGenerator.generateHousehold(familyParameters);

        moveHouseholdToTownOrParish(household, parishTemplate);

        int childHouseholdPopulation = tryMoveOutSons(household, parishTemplate);

        return childHouseholdPopulation + household.getPopulation(familyParameters.getReferenceDate());
    }

    /**
     * Look for a job for the head of the household. If one is found in a town, move to that town. Otherwise move to
     * the parish itself.
     */
    private void moveHouseholdToTownOrParish(@NonNull Household household, @NonNull ParishTemplate parishTemplate) {
        LocalDate onDate = parishTemplate.getFamilyParameters().getReferenceDate();
        Person person = household.getHead(onDate);
        boolean placedInTown = findTownWithOccupationForHouseholdHead(parishTemplate, household, person);

        if (!placedInTown) {
            if (parishTemplate.getParish().getPopulation(onDate) < parishTemplate.getExpectedRuralPopulation()) {
                addHouseholdToDwellingPlaceOnWeddingDate(parishTemplate.getParish(), household, getMoveInDate(person,
                        onDate));
            } else {
                // Add to a random town even without a job. First try to fill out any towns that have room left but
                // no more jobs.
                List<TownTemplate> townsWithNoOccupationsLeft = parishTemplate.getTowns().stream()
                        .filter(t -> t.getExpectedOccupations().isEmpty())
                        .collect(Collectors.toList());
                int size = townsWithNoOccupationsLeft.size();
                if (size > 0) {
                    TownTemplate townTemplate = townsWithNoOccupationsLeft.get(new Die(size).roll() - 1);
                    addHouseholdToDwellingPlaceOnWeddingDate(townTemplate.getTown(), household, getMoveInDate(person,
                            onDate));
                    return;
                }

                // Otherwise look for any town that has room
                List<TownTemplate> townsWithPopulationLeft = parishTemplate.getTowns().stream()
                        .filter(t -> t.getExpectedPopulation() > t.getTown().getPopulation(onDate))
                        .collect(Collectors.toList());

                size = townsWithPopulationLeft.size();
                if (size > 0) {
                    TownTemplate townTemplate = townsWithPopulationLeft.get(new Die(size).roll() - 1);
                    addHouseholdToDwellingPlaceOnWeddingDate(townTemplate.getTown(), household, getMoveInDate(person,
                            onDate));
                    return;
                }

                log.info("There was no room in the parish nor in any town. Adding household to parish");
                addHouseholdToDwellingPlaceOnWeddingDate(parishTemplate.getParish(), household, getMoveInDate(person,
                        onDate));
            }
        }
    }

    /**
     * The household head will try to find a job opening in a town whose upper social class range corresponds to
     * his social class. If no town has such an opening, null is returned. If an opening is found, the person's
     * occupation will be set and the town will be returned so his household can be added to it.
     *
     * @param household the household of which the head is looking for a job
     * @return true if a job was found and the household was added to a town
     */
    private boolean findTownWithOccupationForHouseholdHead(@NonNull ParishTemplate template,
                                                           @NonNull Household household,
                                                           Person person) {

        if (person == null || person.getSocialClass() == null) {
            return false;
        }

        LocalDate referenceDate = template.getFamilyParameters().getReferenceDate();
        SocialClass socialClass = person.getSocialClass();

        for (TownTemplate townTemplate : template.getTowns()) {
            if (townTemplate.getExpectedOccupations().size() == 0 &&
                    (townTemplate.getTown().getPopulation(referenceDate) < townTemplate.getExpectedPopulation())) {
                log.info(String.format(
                        "There are no more jobs in %s but there is still population space remaining. " +
                                "%s (%s) will move in but not take a job.", townTemplate.getTown().getName(),
                        person.getName(), socialClass.name().toLowerCase()));
                addHouseholdToDwellingPlaceOnWeddingDate(townTemplate.getTown(), household,
                        getMoveInDate(person, referenceDate));

                return true;
            }
            for (Map.Entry<Occupation, Integer> occupationSlot : townTemplate.getExpectedOccupations().entrySet()) {
                Occupation occupation = occupationSlot.getKey();
                if (personWillAcceptOccupation(person, occupation) && occupationSlot.getValue() > 0) {
                    log.info(String.format("%s (%s) will take a job in %s as a %s", person.getName(),
                            socialClass.name().toLowerCase(), townTemplate.getTown().getName(),
                            occupationSlot.getKey().getName()));

                    person.addOccupation(occupation, getJobStartDate(person));

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

    private LocalDate getJobStartDate(@NonNull Person person) {

        if (person.isLiving(person.getBirthDate().plusYears(16))) {
            return person.getBirthDate().plusYears(16);
        }

        if (!person.getFamilies().isEmpty()) {
            Family family = person.getFamilies().iterator().next();
            if (family.getWeddingDate() != null) {
                return family.getWeddingDate();
            }
        }

        return person.getDeathDate();
    }

    private int tryMoveOutSons(@NonNull Household household, @NonNull ParishTemplate parishTemplate) {
        LocalDate onDate = parishTemplate.getFamilyParameters().getReferenceDate();

        if (household.getHead(onDate) == null) {
            household.resetHeadAsOf(onDate);
        }
        // We don't want to move out a son who is the head of the household due to the death of his father
        Person head = household.getHead(onDate);

        List<Person> adultSons = household.getInhabitants(onDate).stream()
                .filter(Person::isMale)
                .filter(p -> !p.equals(head))
                .filter(p -> p.getAgeInYears(onDate) >= 18)
                .collect(Collectors.toList());

        int sonHouseholdPopulation = 0;
        for (Person adultSon : adultSons) {
            Family family = familyGenerator.generate(adultSon, parishTemplate.getFamilyParameters());
            if (family != null) {
                log.info(String.format("Adult son %s married on %s and moved out", adultSon.getName(),
                        family.getWeddingDate()));
                Household sonsHousehold = moveOutSon(household, family, parishTemplate);

                moveHouseholdToTownOrParish(sonsHousehold, parishTemplate);

                sonHouseholdPopulation += sonsHousehold.getPopulation(onDate);

            } else {
                log.info(String.format("Adult son %s could not find a wife and stayed home", adultSon.getName()));
            }
        }
        return sonHouseholdPopulation;
    }

    private Household moveOutSon(@NonNull Household oldHousehold,
                           @NonNull Family family,
                           @NonNull ParishTemplate parishTemplate) {

        if (parishTemplate.getFamilyParameters().isPersist()) {
            family = familyService.save(family);
        }

        LocalDate onDate = parishTemplate.getFamilyParameters().getReferenceDate();

        Household newHousehold = new Household();
        householdGenerator.addFamilyToHousehold(newHousehold, family, family.getWeddingDate());
        householdService.save(oldHousehold);

        if (!family.getHusband().isLiving(onDate)) {
            newHousehold.resetHeadAsOf(family.getHusband().getDeathDate());
        }

        newHousehold = householdService.save(newHousehold);

        return newHousehold;
    }
}
