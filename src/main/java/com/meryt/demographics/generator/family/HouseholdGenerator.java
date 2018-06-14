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
import com.meryt.demographics.domain.place.HouseholdInhabitantPeriod;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.service.AncestryService;
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
    private final AncestryService ancestryService;

    public HouseholdGenerator(@NonNull FamilyGenerator familyGenerator,
                              @NonNull PersonService personService,
                              @NonNull FamilyService familyService,
                              @NonNull HouseholdService householdService,
                              @NonNull AncestryService ancestryService) {
        this.familyGenerator = familyGenerator;
        this.personService = personService;
        this.familyService = familyService;
        this.householdService = householdService;
        this.ancestryService = ancestryService;
    }

    /**
     * Generate a household by generating a family with a living head on the reference date, and putting that family
     * in the household.
     *
     * @param familyParameters the parameters used to generate the family. The reference date must be set.
     * @return Household containing the living members of a Family (one or both spouses and their children)
     */
    public Household generateHousehold(@NonNull FamilyParameters familyParameters) {
        FamilyParameters localParameters = new FamilyParameters(familyParameters);
        LocalDate onDate = localParameters.getReferenceDate();
        if (onDate == null) {
            throw new IllegalArgumentException("Cannot generate a household for a null reference date");
        }
        Person founder = familyGenerator.generateFounder(localParameters);
        if (founder.isFemale()) {
            // The founder must be alive on the reference date, so don't allow her to die in childbirth.
            localParameters.setAllowMaternalDeath(false);
        }
        Family family = familyGenerator.generate(founder, localParameters);
        Person spouse = null;
        if (family != null) {
            spouse = family.getHusband().equals(founder)
                    ? family.getWife()
                    : family.getHusband();
        }
        Household household;
        if (!localParameters.isAllowExistingSpouse() || spouse == null
                || (household = spouse.getHousehold(family.getWeddingDate())) == null) {
            // A spouse with an existing household will re-use their household
            household = new Household();
            household = householdService.save(household);
        }

        if (family != null) {
            familyService.save(family);
            if (localParameters.isAllowExistingSpouse()) {
                ancestryService.updateAncestryTable();
            }
            addFamilyToHousehold(household, family, onDate);
            householdService.save(household);
            familyService.save(family);
            if (localParameters.isAllowExistingSpouse()) {
                householdService.addStepchildrenToHousehold(founder, family, household);
            }

        } else {
            personService.save(founder);
            householdService.addPersonToHousehold(founder, household, founder.getBirthDate(), true);
            householdService.save(household);
            personService.save(founder);
        }

        // If the person was married but is now not, try to find a second spouse
        if (family != null && !founder.isMarriedNowOrAfter(onDate)) {
            if (founder.isFemale() && household.getHead(onDate) == null) {
                householdService.resetHeadAsOf(household, family.getHusband().getDeathDate());
            }
            Family secondFamily = familyGenerator.generate(founder, localParameters);
            if (secondFamily != null) {
                personService.save(founder);
                familyService.save(secondFamily);
                if (localParameters.isAllowExistingSpouse()) {
                    ancestryService.updateAncestryTable();
                }
                addFamilyToHousehold(household, secondFamily, onDate);
                householdService.save(household);
                familyService.save(secondFamily);
                if (localParameters.isAllowExistingSpouse()) {
                    householdService.addStepchildrenToHousehold(founder, secondFamily, household);
                }
            }
        }

        List<String> inhabitants = household.getInhabitants(localParameters.getReferenceDate())
                .stream()
                .sorted(Comparator.comparing(Person::getBirthDate))
                .map(p -> p.getName() + " (" + p.getAgeInYears(onDate) + ")")
                .collect(Collectors.toList());

        log.info(String.format("On %s the household contained %s", localParameters.getReferenceDate(),
                String.join(", ", inhabitants)));

        return householdService.save(household);
    }

    /**
     * Add a family to a household. If the existingHead is not null, this person will not be re-added if he's already
     * in the household.
     *
     * @param household the household
     * @param family the family to add
     * @param onDate the date that might be used, unless the wedding date is a better choice
     */
    public void addFamilyToHousehold(@NonNull Household household,
                                     @NonNull Family family,
                                     @NonNull LocalDate onDate) {
        LocalDate weddingDate = family.getWeddingDate();
        // Will only be non-null if the father is dead and there is a son at least 16
        Person oldestLivingSon = null;
        if (family.isHusbandLiving(onDate) && !household.getInhabitants(onDate).contains(family.getHusband())) {
            LocalDate moveInDate = weddingDate != null ? weddingDate : family.getHusband().getBirthDate();
            moveToNewHousehold(family.getHusband(), household, moveInDate, true);
        } else {
            oldestLivingSon = getOldestLivingSonOverFifteen(family, onDate);
        }

        if (family.isWifeLiving(onDate) && !household.getInhabitants(onDate).contains(family.getWife())) {
            LocalDate moveInDate = weddingDate != null ? weddingDate : family.getWife().getBirthDate();
            // She's only the head if she has no husband or son old enough
            boolean isHead = !family.getHusband().isLiving(moveInDate) && household.getHead(moveInDate) == null
                    && oldestLivingSon == null;
            moveToNewHousehold(family.getWife(), household, moveInDate, isHead);
        }

        for (Person child : family.getChildren()) {
            if (child.isLiving(onDate) && !household.getInhabitants(onDate).contains(child)) {
                boolean isHead = child.equals(oldestLivingSon) && household.getHead(onDate) == null;
                if (isHead) {
                    replaceHouseholdHead(household, child.getBirthDate());
                }
                householdService.addPersonToHousehold(child, household, child.getBirthDate(), isHead);
            }
        }
    }

    /**
     * Moves a person to a new household as of the given date, with no end date. If he was previously a member of
     * another household, that residence will end as of the given date. If he is only in a household in the future, that
     * future household membership will be deleted and replaced with the new one, as there is no end date specified
     * in this method.
     *
     * @param person the person to move in (and possibly out of an existing household)
     * @param household the household to move into
     * @param moveInDate the date when he takes up residence in the new household
     */
    private void moveToNewHousehold(@NonNull Person person,
                                    @NonNull Household household,
                                    @NonNull LocalDate moveInDate,
                                    boolean isHead) {
        Household formerHousehold = person.getHousehold(moveInDate);
        if (formerHousehold != null) {
            householdService.endPersonResidence(formerHousehold, person, moveInDate);
            householdService.save(formerHousehold);
        }
        // Remove any future households
        List<HouseholdInhabitantPeriod> futureHouseholds = person.getHouseholds().stream()
                .filter(hip -> hip.getFromDate().isAfter(moveInDate))
                .collect(Collectors.toList());
        if (!futureHouseholds.isEmpty()) {
            for (HouseholdInhabitantPeriod period : futureHouseholds) {
                householdService.delete(period);
            }
        }

        if (isHead && !person.equals(household.getHead(moveInDate))) {
            replaceHouseholdHead(household, moveInDate);
        }
        householdService.addPersonToHousehold(person, household, moveInDate, isHead);
    }

    private void replaceHouseholdHead(@NonNull Household household, @NonNull LocalDate endOfHeadShip) {
        Person currentHead = household.getHead(endOfHeadShip);
        if (currentHead != null) {
            householdService.endPersonResidence(household, currentHead, endOfHeadShip);
            householdService.addPersonToHousehold(currentHead, household, endOfHeadShip, false);
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
