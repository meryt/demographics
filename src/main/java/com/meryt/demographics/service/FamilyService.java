package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.generator.random.PercentDie;
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
    public Family createAndSaveFamily(@Nullable Person husband,
                                      @Nullable Person wife,
                                      @Nullable LocalDate weddingDate,
                                      boolean skipCreateHouseholds,
                                      boolean skipManageCapital) {
        if (weddingDate != null && husband != null && wife != null) {
            checkPersonAliveAndUnmarriedOnWeddingDate(husband, weddingDate);
            checkPersonAliveAndUnmarriedOnWeddingDate(wife, weddingDate);

            return createAndSaveMarriage(husband, wife, weddingDate, skipCreateHouseholds, skipManageCapital);
        }
        if (husband == null && wife == null) {
            throw new IllegalArgumentException("Both husband and wife cannot be null");
        }

        Family family = new Family();
        if (husband != null) {
            family.setHusband(husband);
        }
        if (wife != null) {
            family.setWife(wife);
            wife.getMaternity().setFather(husband);
            personService.save(wife);
        }
        family.setWeddingDate(weddingDate);
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
    private Family createAndSaveMarriage(@NonNull Person husband,
                                         @NonNull Person wife,
                                         @NonNull LocalDate weddingDate,
                                         boolean skipCreateHouseholds,
                                         boolean skipManageCapital) {
        Family family = new Family();
        family.setHusband(husband);
        family.setWife(wife);
        family.setWeddingDate(weddingDate);
        wife.getMaternity().setFather(husband);
        wife.getMaternity().cycleForwardsToDate(weddingDate, false);
        wife.getMaternity().setLastCheckDate(weddingDate);

        personService.save(wife);
        family = save(family);
        return setupMarriage(family, weddingDate, !skipCreateHouseholds, !skipManageCapital, false);
    }

    Family setupMarriage(@NonNull Family family,
                         @NonNull LocalDate weddingDate,
                         boolean manageHouseholds,
                         boolean manageCapital,
                         boolean moveAwayIfHusbandNonResident) {

        // If a first-time bride has living parents, they should give her some money.
        if (manageCapital) {
            applyMarriageSettlements(family.getWife(), weddingDate);
        }

        Person husband = family.getHusband();
        Person wife = family.getWife();

        Occupation husbandOcc = husband.getOccupation(weddingDate);
        if (husbandOcc != null && !husbandOcc.isMayMarry()) {
            husband.quitJob(weddingDate);
        }
        Occupation wifeOcc = wife.getOccupation(weddingDate);
        if (wifeOcc != null && !wifeOcc.isMayMarry()) {
            wife.quitJob(weddingDate);
        }

        if (manageHouseholds) {
            DwellingPlace husbandPlace = husband.getResidence(weddingDate);
            DwellingPlace wifePlace = wife.getResidence(weddingDate);

            Household husbandsHousehold = moveWifeAndStepchildrenToHusbandsHousehold(family);

            if (moveAwayIfHusbandNonResident && husbandPlace == null && wifePlace == null) {
                log.info("Not moving household. Neither family member lives in the parishes.");
                personService.maybeDisableMaternityCheckingForNonResidentFamily(husband, wife);
                return family;
            }

            // If neither partner owns a place to live, they might move away.
            if (moveAwayIfHusbandNonResident && husband.getOwnedDwellingPlaces(weddingDate).isEmpty() &&
                    wife.getOwnedDwellingPlaces(weddingDate).isEmpty() && husband.getLivingChildren(weddingDate).isEmpty()
                    && wife.getLivingChildren(weddingDate).isEmpty()) {
                Parish parish = husbandPlace == null ? wifePlace.getParish() : husbandPlace.getParish();
                if (parish != null && shouldEmigrate(parish, weddingDate)) {
                    emigrate(family, weddingDate);

                    return family;
                }
            }

            findResidenceForNewFamily(family, husbandsHousehold, moveAwayIfHusbandNonResident);
        }

        return family;
    }

    private boolean shouldEmigrate(@NonNull Parish parish, @NonNull LocalDate date) {
        double popPerSquareMile = parish.getPopulationPerSquareMile(date);
        // The chance of moving away is based on the population per square mile. This is a parabolic function
        // that ranges from about 0 at 30 to about 1 at 100.
        double chanceOfEmigrating = parish.getChanceOfEmigrating(date);
        double roll = PercentDie.roll();
        if (roll < chanceOfEmigrating) {
            log.info(String.format(
                    "Current population per square mile of %s on %s is %.2f. New family will emigrate.",
                    parish.getName(),
                    date,
                    popPerSquareMile));

            return true;
        }
        return false;
    }

    private void emigrate(@NonNull Family family, @NonNull LocalDate onDate) {
        Person husband = family.getHusband();
        Person wife = family.getWife();
        if (husband.getResidence(onDate) != null) {
            husband = householdService.endPersonResidence(husband.getHousehold(onDate), husband, onDate);
        }
        if (wife.getResidence(onDate) != null) {
            wife = householdService.endPersonResidence(wife.getHousehold(onDate), wife, onDate);
        }
        personService.maybeDisableMaternityCheckingForNonResidentFamily(husband, wife);
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
        Person woman = family.getWife();

        Household womanHousehold = woman.getHousehold(date);
        Household manHousehold = man.getHousehold(date);
        if (manHousehold == null || !man.equals(manHousehold.getHead(date))) {
            // If the man does not have a household or if he is not the head of a household, either create a new
            // household for him, or use the wife's household if she is the head of it.
            if (womanHousehold == null || !woman.equals(womanHousehold.getHead(date))) {
                log.info(String.format("Creating household for %d %s to head", man.getId(), man.getName()));
                manHousehold = new Household();
                man = householdService.addPersonToHousehold(man, manHousehold, date, true);
                personService.save(man);
                householdService.addChildrenToHousehold(man, manHousehold, date).forEach(personService::save);
            } else {
                // If the wife is the head of her household, add the man to it but make him the head.
                log.info(String.format("Making %d %s head of the household %d %s was previously heading",
                        man.getId(), man.getName(), woman.getId(), woman.getName()));
                woman = householdService.addPersonToHousehold(woman, womanHousehold, date, false);
                personService.save(woman);
                man = householdService.addPersonToHousehold(man, womanHousehold, date, true);
                personService.save(man);
                // If the man had some children, add them to the wife's household.
                householdService.addStepchildrenToHousehold(woman, family, womanHousehold);
                return womanHousehold;
            }
        } else {
            log.info(String.format("%d %s is already head of a household; adding wife", man.getId(), man.getName()));
        }
        woman = householdService.addPersonToHousehold(woman, manHousehold, date, false);
        personService.save(woman);

        householdService.addStepchildrenToHousehold(man, family, manHousehold).forEach(personService::save);
        return manHousehold;
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
        DwellingPlace wifeFormerHouse = wife.getResidence(date.minusDays(1));
        DwellingPlace husbandFormerHouse = man.getResidence(date.minusDays(1));

        // If the man is not a resident of the parishes, and the wife does not own a house or have a job, move the
        // household away from the parishes.
        if (husbandFormerHouse == null && moveAwayIfHusbandNonResident &&
                wife.getOwnedDwellingPlaces(date).isEmpty()
                && wife.getOccupation(date) == null) {

            Parish wifeParish = wifeFormerHouse == null ? null : wifeFormerHouse.getParish();
            if (wifeParish == null || shouldEmigrate(wifeParish, date)) {
                log.info("Moving new family away from the parishes, as the husband is not a resident, and the wife is " +
                        "unemployed and owns no property.");
                personService.maybeDisableMaternityCheckingForNonResidentFamily(man, wife);
                return;
            }
        }

        DwellingPlace residence = selectResidenceForNewFamily(family, mansHousehold);
        if (residence == null) {
            HouseholdLocationPeriod hp;
            if ((hp = mansHousehold.getHouseholdLocationPeriod(date)) != null) {
                hp.setToDate(date);
                householdService.save(mansHousehold);
            }
            personService.maybeDisableMaternityCheckingForNonResidentFamily(man, wife);
            return;
        }

        // If the husband is not living in the new residence move him there.
        if (!residence.equals(man.getResidence(date))) {
            residence = householdDwellingPlaceService.addToDwellingPlace(mansHousehold, residence, date, null);
        }

        // If one of the couple moved out of a dwelling place, it might be empty now, or contain only minors, or require
        // a new head of household.
        DwellingPlace possiblyEmptyResidence = residence.equals(husbandFormerHouse)
                ? wifeFormerHouse
                : husbandFormerHouse;
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
    private DwellingPlace selectResidenceForNewFamily(@NonNull Family family, @NonNull Household mansHousehold) {
        LocalDate date = family.getWeddingDate();
        Person man = family.getHusband();
        Person wife = family.getWife();

        Dwelling ownedHouse = findBestOwnedEmptyResidenceForNewCouple(man, wife, date, false);
        if (ownedHouse != null) {
            return ownedHouse;
        }

        // She has already moved to the husband's household so we need to get the house of the household she lived in
        // the day before her marriage. The husband may also have created a new household which does not have a location
        // yet. So use yesterday's location.
        DwellingPlace wifeFormerHouse = wife.getResidence(date.minusDays(1));
        DwellingPlace husbandCurrentHouse = man.getResidence(date.minusDays(1));
        DwellingPlace residence;
        // If one of the residences is null, they will live in the one that is not.
        if (wifeFormerHouse == null && husbandCurrentHouse != null) {
            residence = husbandCurrentHouse;
        } else if (wifeFormerHouse != null && husbandCurrentHouse == null) {
            residence = wifeFormerHouse;
        } else if (wifeFormerHouse == null) {
            // Neither lives anywhere. Do nothing.
            return null;
        } else {
            // Both residences are non-null. Figure out which one they will live in.
            // First, if one lives in a house and the other is homeless, they move into the house.
            if (!(wifeFormerHouse instanceof Dwelling) && husbandCurrentHouse instanceof Dwelling) {
                residence = husbandCurrentHouse;
            } else if (wifeFormerHouse instanceof Dwelling && !(husbandCurrentHouse instanceof Dwelling)) {
                residence = wifeFormerHouse;
            } else if (wifeFormerHouse.getOwner(date) != family.getWife()) {
                // If the wife lives in a house but does not own it, she will move into the husband's house
                // regardless of how nice it is.
                residence = husbandCurrentHouse;
            } else {
                // If both man and wife own a house, they will move into the wife's house only if it is of significantly
                // higher value.
                residence = wifeFormerHouse.getValue() > (1.2 * husbandCurrentHouse.getValue())
                        ? wifeFormerHouse
                        : husbandCurrentHouse;
            }
        }

        // If the residence is already owned by one of the spouses, there is no need to move
        if (residence.getOwner(date) == family.getHusband() || residence.getOwner(date) == family.getWife()) {
            return residence;
        }

        if (residence.getParish() != null) {
            // If the selected residence is not owned by either of the two people, they may buy a new house instead.
            Parish parish = residence.getParish();
            Person richerSpouse = family.getHusband().getCapitalNullSafe(date) > family.getWife().getCapitalNullSafe(date)
                    ? family.getHusband()
                    : family.getWife();
            double capital = richerSpouse.getCapitalNullSafe(date);
            SocialClass maxSocialClass = SocialClass.fromRank(Math.max(man.getSocialClassRank(), wife.getSocialClassRank()));
            double minAcceptableValue = WealthGenerator.getHouseValueRange(maxSocialClass).getFirst();

            List<Dwelling> emptyHouses = parish.getEmptyHouses(date);
            // Couple will buy the most expensive house they can afford, assuming they can afford any corresponding to
            // their taste, which depends on social class.
            Dwelling buyableHouse = householdDwellingPlaceService.findBuyableHousesFarmsAndEstates(parish, date, capital)
                    .stream()
                    .filter(d -> d.getValue() >= minAcceptableValue)
                    .max(Comparator.comparing(Dwelling::getNullSafeValueIncludingAttachedParent))
                    .orElse(null);

            if (buyableHouse != null && (buyableHouse.getOwner(date) == family.getWife()
                    || buyableHouse.getOwner(date) == family.getHusband())) {
                return buyableHouse;
            }

            if (buyableHouse != null) {
                log.info(String.format("%d %s is buying a house %d in %s on %s", richerSpouse.getId(),
                        richerSpouse.getName(), buyableHouse.getId(), buyableHouse.getLocationString(), date));
                householdDwellingPlaceService.buyAndMoveIntoHouse(buyableHouse, richerSpouse, date,
                        DwellingPlaceOwnerPeriod.ReasonToPurchase.MARRIAGE.getMessage());
                return buyableHouse;
            }

            // If no buyable house could be found, and the rank of the family is high, they will move away rather
            // than settle for a cheap house.
            if (maxSocialClass.getRank() >= SocialClass.GENTLEMAN.getRank()) {
                log.info(String.format("No house could be found suitable for a %s; couple will move away",
                        maxSocialClass.getFriendlyName()));
                return null;
            }

            double randomNewHouseValue = WealthGenerator.getRandomHouseValue(maxSocialClass);
            if (capital > randomNewHouseValue) {
                DwellingPlace placeToBuildHouse = residence.getTownOrParish();
                if (placeToBuildHouse != null) {
                    Dwelling house = householdDwellingPlaceService.moveFamilyIntoNewHouse(placeToBuildHouse, mansHousehold,
                            date, randomNewHouseValue, DwellingPlaceOwnerPeriod.ReasonToPurchase.MARRIAGE.getMessage());
                    richerSpouse.addCapital(-1.0 * randomNewHouseValue, date,
                            PersonCapitalPeriod.Reason.builtNewDwellingPlaceMessage(house));
                    personService.save(richerSpouse);
                    log.info(String.format("%d %s's new family built a new house in %s, worth %.2f",
                            family.getHusband().getId(), family.getHusband().getName(),
                            placeToBuildHouse.getFriendlyName(),
                            randomNewHouseValue));
                    return house;
                }
            }

            if (!emptyHouses.isEmpty()) {
                Collections.shuffle(emptyHouses);
                Dwelling house = emptyHouses.get(0);
                Person owner = house.getOwner(date);
                log.info(String.format("%d %s's new family is moving into empty house in %s owned by %s",
                        family.getHusband().getId(), family.getHusband().getName(),
                        house.getLocationString(), owner == null
                                ? "nobody"
                                : owner.getId() + " " + owner.getName()));
                householdDwellingPlaceService.addToDwellingPlace(mansHousehold, house, date, null);
                return house;
            }

            log.info(String.format("No empty houses are available in %s; new family will stay in %s",
                    parish.getName(), (wifeFormerHouse == residence ? "wife's house" : "husband's house")));
        }

        return residence;
    }

    @Nullable
    private Dwelling findBestOwnedEmptyResidenceForNewCouple(@NonNull Person man,
                                                             @NonNull Person wife,
                                                             @NonNull LocalDate onDate,
                                                             boolean mustBeAtLeastMinimumValue) {

        SocialClass maxSocialClass = SocialClass.fromRank(Math.max(man.getSocialClassRank(),
                wife.getSocialClassRank()));

        final double minValue = mustBeAtLeastMinimumValue
                ? WealthGenerator.getHouseValueRange(maxSocialClass).getFirst()
                : 0;

        // Check for any houses they already own. Pick the best one -- manor houses first, then the most
        // expensive.
        List<DwellingPlace> ownedResidences = new ArrayList<>();
        ownedResidences.addAll(wife.getOwnedDwellingPlaces(onDate));
        ownedResidences.addAll(man.getOwnedDwellingPlaces(onDate));
        List<Dwelling> ownedResidencesOnEstates = ownedResidences.stream()
                .filter(r -> r.getParent().isEstate() && r.isAttachedToParent() && r.isHouse() && r.getValue() >= minValue)
                .map(r -> (Dwelling) r)
                .sorted(Comparator.comparing(DwellingPlace::getValue).reversed())
                .collect(Collectors.toList());
        if (!ownedResidencesOnEstates.isEmpty()) {
            return ownedResidencesOnEstates.get(0);
        }

        if (!ownedResidences.isEmpty()) {
            Dwelling house = ownedResidences.stream()
                    .filter(h -> h.isHouse() && h.getValue() >= minValue)
                    .map(h -> (Dwelling) h)
                    .max(Comparator.comparing(DwellingPlace::getValue).reversed())
                    .orElse(null);
            if (house != null) {
                return house;
            }
        }
        return null;
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

        wife.addCapital(settlement, weddingDate,
                PersonCapitalPeriod.Reason.receivedMarriageSettlementMessage(settlement));
        personService.save(wife);
    }

    /**
     * Calculates the marriage settlement from a particular parent, if any. Removes that much capital from their wealth
     * and saves the parent. Does not affect the daughter's current wealth, but does return the amount that was
     * transferred away from the parent.
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

        parent.addCapital(-1 * bridesSettlement, date,
                PersonCapitalPeriod.Reason.providedMarriageSettlementMessage(daughter, bridesSettlement));

        personService.save(parent);
        log.info(String.format("%d %s received a marriage settlement of %.2f from her parent %d %s",
                daughter.getId(), daughter.getName(), bridesSettlement, parent.getId(), parent.getName()));
        return bridesSettlement;
    }

    List<Family> loadFamiliesNotInSameHousehold(@NonNull LocalDate onDate) {
        return familyRepository.loadFamiliesNotInSameHousehold(onDate);
    }

}
