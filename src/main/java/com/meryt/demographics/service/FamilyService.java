package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.repository.FamilyRepository;

@Service
@Slf4j
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final HouseholdService householdService;
    private final PersonService personService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;

    FamilyService(@Autowired FamilyRepository familyRepository,
                  @Autowired HouseholdService householdService,
                  @Autowired PersonService personService,
                  @Autowired HouseholdDwellingPlaceService householdDwellingPlaceService) {
        this.familyRepository = familyRepository;
        this.householdService = householdService;
        this.personService = personService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
    }

    /**
     * Save a family using the repository
     */
    public Family save(@NonNull Family family) {
        return familyRepository.save(family);
    }

    /**
     * Finds a family by ID or returns null if none found
     */
    @Nullable
    public Family load(long familyId) {
        return familyRepository.findById(familyId).orElse(null);
    }


    /**
     * Creates and saves a family. If both spouses and a wedding date are provided, a marriage is created, which
     * includes combining households, moving to dwellings, and updating wealth due to marriage settlements, where
     * applicable.
     *
     * @param husband an optional man
     * @param wife an optional woman
     * @param weddingDate an optional wedding date
     * @return the new family
     */
    public Family createAndSaveFamily(@Nullable Person husband, @Nullable Person wife, @Nullable LocalDate weddingDate) {
        if (weddingDate != null && husband != null && wife != null) {
            checkPersonAliveAndUnmarriedOnWeddingDate(husband, weddingDate);
            checkPersonAliveAndUnmarriedOnWeddingDate(wife, weddingDate);

            return createAndSaveMarriage(husband, wife, weddingDate);
        }
        if (husband == null && wife == null) {
            throw new IllegalArgumentException("Both husband and wife cannot be null");
        }

        Family family = new Family();
        family.setHusband(husband);
        family.setWife(wife);
        family.setWeddingDate(weddingDate);
        if (wife != null) {
            wife.getMaternity().setFather(husband);
            personService.save(wife);
        }
        return save(family);
    }

    /**
     * Creates and saves a marriage. This includes combining households, moving to dwellings, and updating wealth due
     * to marriage settlements, where applicable.
     *
     * @param husband a man
     * @param wife a woman
     * @param weddingDate a wedding date
     * @return the new family
     */
    private Family createAndSaveMarriage(@NonNull Person husband, @NonNull Person wife, @NonNull LocalDate weddingDate) {
        Family family = new Family();
        family.setHusband(husband);
        family.setWife(wife);
        family.setWeddingDate(weddingDate);
        wife.getMaternity().setFather(husband);
        personService.save(wife);
        family = save(family);
        return setupMarriage(family, weddingDate, false);
    }

    Family setupMarriage(@NonNull Family family, @NonNull LocalDate weddingDate, boolean moveAwayIfHusbandNonResident) {

        // If a first-time bride has living parents, they should give her some money.
        applyMarriageSettlements(family.getWife(), weddingDate);

        if (moveAwayIfHusbandNonResident && family.getHusband().getResidence(weddingDate) == null &&
                family.getWife().getResidence(weddingDate) == null) {
            log.info("Not creating household. Neither family member lives in the parishes.");
        } else {
            Household husbandsHousehold = moveWifeAndStepchildrenToHusbandsHousehold(family);
            findResidenceForNewFamily(family, husbandsHousehold, moveAwayIfHusbandNonResident);
        }

        return family;
    }

    private void checkPersonAliveAndUnmarriedOnWeddingDate(@NonNull Person person, @NonNull LocalDate weddingDate) {
        if (!person.isLiving(weddingDate)) {
            throw new IllegalArgumentException(String.format(
                    "%d %s (%s - %s) could not be married on %s as they were not alive on that date.",
                    person.getId(),
                    person.getName(),
                    person.getBirthDate().getYear(),
                    person.getDeathDate().getYear(),
                    weddingDate));
        }
        if (person.isMarriedNowOrAfter(weddingDate)) {
            throw new IllegalArgumentException(String.format("%s was already married on or after %s",
                    person.getName(), weddingDate));
        }
    }

    /**
     * Combines the households. The wife and any of her minor children always join the man's household.
     *
     * @param family the newly wedded family
     */
    private Household moveWifeAndStepchildrenToHusbandsHousehold(@NonNull Family family) {
        LocalDate date = family.getWeddingDate();
        Person man = family.getHusband();
        Person wife = family.getWife();

        Household mansHousehold = man.getHousehold(date);
        if (mansHousehold == null) {
            mansHousehold = new Household();
            man = householdService.addPersonToHousehold(man, mansHousehold, date, true);
            personService.save(man);
        }
        wife = householdService.addPersonToHousehold(wife, mansHousehold, date, false);
        personService.save(wife);

        householdService.addStepchildrenToHousehold(man, family, mansHousehold).forEach(personService::save);
        return mansHousehold;
    }

    /**
     * Find a residence for the new couple.
     *
     * @param family the newly wedded family
     * @param mansHousehold the household of the man
     * @param moveAwayIfHusbandNonResident if true, and the husband is not a resident of any dwelling place, and the
     *                                     wife is neither a property owner nor employed, then move the household will
     *                                     not be moved anywhere
     */
    private void findResidenceForNewFamily(@NonNull Family family,
                                           @NonNull Household mansHousehold,
                                           boolean moveAwayIfHusbandNonResident) {
        LocalDate date = family.getWeddingDate();
        Person man = family.getHusband();
        Person wife = family.getWife();

        // She has already moved to the husband's household so we need to get the house of the household she lived in
        // the day before her marriage.
        DwellingPlace wifesFormerHouse = wife.getResidence(date.minusDays(1));
        DwellingPlace husbandsCurrentHouse = man.getResidence(date);

        // If the man is not a resident of the parishes, and the wife does not own a house or have a job, move the
        // household away from the parishes.
        if (husbandsCurrentHouse == null && moveAwayIfHusbandNonResident &&
                wife.getOwnedDwellingPlaces(date).isEmpty()
                && wife.getOccupation(date) == null) {
            log.info("Moving new family away from the parishes, as the husband is not a resident, and the wife is " +
                    "unemployed and owns no property.");
            return;
        }

        DwellingPlace residence = selectResidenceForNewFamily(family);
        if (residence == null) {
            return;
        }

        // If the husband is not living in the new residence move him there.
        if (!residence.equals(husbandsCurrentHouse)) {
            residence = householdDwellingPlaceService.addToDwellingPlace(mansHousehold, residence, date, null);
        }

        // If one of the couple moved out of a dwelling place, it might be empty now, or contain only minors, or require
        // a new head of household.
        DwellingPlace possiblyEmptyResidence = residence.equals(husbandsCurrentHouse)
                ? wifesFormerHouse
                : husbandsCurrentHouse;
        if (possiblyEmptyResidence == null || possiblyEmptyResidence.equals(residence)) {
            return;
        }

        for (Household household : possiblyEmptyResidence.getAllHouseholds(date)) {
            if (household.getInhabitants(date).isEmpty()) {
                // If the household is now empty, set the "to" date of its location so that it no longer resides
                // anywhere.
                HouseholdLocationPeriod oldPeriod = household.getHouseholdLocationPeriod(date);
                if (oldPeriod != null) {
                    oldPeriod.setToDate(date);
                    householdService.save(oldPeriod);
                }
            } else {
                if (household.getHead(date) == null) {
                    householdService.resetHeadAsOf(household, date);
                    if (household.getHead(date) == null) {
                        log.warn("Unable to find a new head for household " + household.getId() + " containing " +
                                household.getInhabitants(date).stream()
                                        .map(p -> p.getId() + " " + p.getName() + " (" + p.getAgeInYears(date) + ")")
                                        .collect(Collectors.joining(",")));
                    }
                    // This household should move in with man
                    residence = householdDwellingPlaceService.addToDwellingPlace(household, residence, date, null);
                }
            }
        }
    }

    @Nullable
    private DwellingPlace selectResidenceForNewFamily(@NonNull Family family) {
        LocalDate date = family.getWeddingDate();
        Person man = family.getHusband();
        Person wife = family.getWife();
        // She has already moved to the husband's household so we need to get the house of the household she lived in
        // the day before her marriage.
        DwellingPlace wifesFormerHouse = wife.getResidence(date.minusDays(1));
        DwellingPlace husbandsCurrentHouse = man.getResidence(date);
        DwellingPlace residence;
        // If one of the residences is null, they will live in the one that is not.
        if (wifesFormerHouse == null && husbandsCurrentHouse != null) {
            residence = husbandsCurrentHouse;
        } else if (wifesFormerHouse != null && husbandsCurrentHouse == null) {
            residence = wifesFormerHouse;
        } else if (wifesFormerHouse == null) {
            // Neither lives anywhere. Do nothing.
            return null;
        } else {
            // Both residences are non-null. Figure out which one they will live in.
            // First, if one lives in a house and the other is homeless, they move into the house.
            if (!(wifesFormerHouse instanceof Dwelling) && husbandsCurrentHouse instanceof Dwelling) {
                residence = husbandsCurrentHouse;
            } else if (wifesFormerHouse instanceof Dwelling && !(husbandsCurrentHouse instanceof Dwelling)) {
                residence = wifesFormerHouse;
            } else if (!wifesFormerHouse.getOwners(date).contains(family.getWife())) {
                // If the wife lives in a house but does not own it, she will move into the husband's house
                // regardless of how nice it is.
                residence = husbandsCurrentHouse;
            } else {
                // If both man and wife own a house, they will move into the wife's house only if it is of significantly
                // higher value.
                residence = wifesFormerHouse.getValue() > (1.2 * husbandsCurrentHouse.getValue())
                        ? wifesFormerHouse
                        : husbandsCurrentHouse;
            }
        }

        if (residence != null) {
            // If the selected residence is not owned by either of the two people, they may buy a new house instead.
            Parish parish = residence.getParish();

        }

        return residence;
    }

    private void applyMarriageSettlements(@NonNull Person wife, @NonNull LocalDate weddingDate) {
        if (!wife.isFemale()) {
            throw new IllegalArgumentException("Marriage settlements are only made to women.");
        }
        if (wife.getFamilies().size() > 1 || wife.getFamily() == null) {
            // It's not her first marriage, or she has no parents. She gets nothing.
            return;
        }

        Person father = wife.getFather();
        Person mother = wife.getMother();

        double settlement = transferSettlementFromParentToDaughter(father, wife, weddingDate);
        settlement += transferSettlementFromParentToDaughter(mother, wife, weddingDate);
        if (settlement <= 0.0) {
            return;
        }

        Double brideCapital = wife.getCapital(weddingDate);
        if (brideCapital == null) {
            brideCapital = 0.0;
        }
        wife.setCapital(brideCapital + settlement, weddingDate);
        personService.save(wife);
    }

    /**
     * Caculates the marriage settlement from a particular parent, if any. Removes that much capital from their wealth
     * and saves the parent. Does not affect the daughter's current wealth, but does return the amount that was
     * transferred away from theparent.
     *
     * @param parent the parent (possibly null, in which case 0.0 is returned0
     * @param daughter the daughter getting married. She gets no settlement if she has been married before.
     * @param date the wedding date
     * @return an amount of money that was transferred away from the parent, or 0.0
     */
    private double transferSettlementFromParentToDaughter(@Nullable Person parent,
                                                          @NonNull Person daughter,
                                                          @NonNull LocalDate date) {
        if (parent == null || !parent.isLiving(date) || !daughter.isLiving(date)) {
            return 0.0;
        }
        Double parentCapital = parent.getCapital(date);
        if (parentCapital == null || parentCapital < 0) {
            return 0.0;
        }

        // Filter out the married children from the living children as they already got theirs. Add back the new bride
        // herself.
        long numPeopleWithShares = parent.getLivingChildren(date).stream()
                .filter(p -> p.getFamilies().size() >= 1)
                .count() + 1;
        if (parent.isMarried(date)) {
            numPeopleWithShares += 1;
        }

        // Taking the living unmarried children, plus the parent and possibly the parent's spouse, the bride gets
        // one half of her share of the parent's cash.
        double bridesSettlement = parentCapital / (2 * numPeopleWithShares);

        parent.setCapital(parentCapital - bridesSettlement, date);

        personService.save(parent);
        log.info(String.format("%d %s received a marriage settlement of %.2f from her parent %d %s",
                daughter.getId(), daughter.getName(), bridesSettlement, parent.getId(), parent.getName()));
        return bridesSettlement;
    }

}
