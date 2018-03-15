package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.HouseholdService;
import com.meryt.demographics.service.PersonService;

/**
 * Can generate a household on a reference date such that at least one adult person is still alive on the date, and
 * there is a head of the household.
 */
@Slf4j
public class HouseholdGenerator {

    private final FamilyGenerator familyGenerator;
    private final PersonService personService;
    private final FamilyService familyService;
    private final HouseholdService householdService;

    public HouseholdGenerator(@NonNull FamilyGenerator familyGenerator,
                              @NonNull PersonService personService,
                              @NonNull FamilyService familyService,
                              @NonNull HouseholdService householdService) {
        this.familyGenerator = familyGenerator;
        this.personService = personService;
        this.familyService = familyService;
        this.householdService = householdService;
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
        if (familyParameters.isPersist()) {
            household = householdService.save(household);
        }

        if (family != null) {
            if (familyParameters.isPersist()) {
                familyService.save(family);
            }
            addFamilyToHousehold(household, family, onDate);
        } else {
            if (familyParameters.isPersist()) {
                personService.save(founder);
            }
            founder.addToHousehold(household, founder.getBirthDate(), true);
        }

        List<String> inhabitants = household.getInhabitants(familyParameters.getReferenceDate())
                .stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .map(p -> p.getName() + " (" + p.getAgeInYears(onDate) + ")")
                .collect(Collectors.toList());

        log.info(String.format("On %s the household contained %s", familyParameters.getReferenceDate(),
                String.join(", ", inhabitants)));

        if (familyParameters.isPersist()) {
            return householdService.save(household);
        } else {
            return household;
        }
    }

    public void addFamilyToHousehold(@NonNull Household household, @NonNull Family family, @NonNull LocalDate onDate) {
        LocalDate weddingDate = family.getWeddingDate();
        // Will only be non-null if the father is dead and there is a son at least 16
        Person oldestLivingSon = null;
        if (family.isHusbandLiving(onDate)) {
            family.getHusband().addToHousehold(household,
                    weddingDate != null ? weddingDate : family.getHusband().getBirthDate(),
                    true);
        } else {
            oldestLivingSon = getOldestLivingSonOverFifteen(family, onDate);
        }
        if (family.isWifeLiving(onDate)) {
            family.getWife().addToHousehold(household,
                    weddingDate != null ? weddingDate : family.getWife().getBirthDate(),
                    // She's only the head if she has no husband or son old enough
                    (!family.getHusband().isLiving(onDate) && oldestLivingSon == null));
        }
        for (Person child : family.getChildren()) {
            if (child.isLiving(onDate)) {
                child.addToHousehold(household, child.getBirthDate(), child.equals(oldestLivingSon));
            }
        }
    }

    @Nullable
    private Person getOldestLivingSonOverFifteen(@NonNull Family family, @NonNull LocalDate onDate) {
        Person oldestLivingSon = null;
        for (Person child : family.getChildren()) {
            if (child.isMale() && child.isLiving(onDate) && child.getAgeInYears(onDate) >= 16 &&
                    (oldestLivingSon == null || child.getBirthDate().isBefore(oldestLivingSon.getBirthDate()))) {
                oldestLivingSon = child;
            }
        }
        return oldestLivingSon;
    }

}
