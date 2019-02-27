package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Estate;
import com.meryt.demographics.domain.place.Farm;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.generator.ParishPopulator;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.request.EstatePost;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.response.calendar.NewHouseEvent;
import com.meryt.demographics.response.calendar.PropertyTransferEvent;

/**
 * Service used for finding houses for households and moving them in.
 */
@Service
@Slf4j
public class HouseholdDwellingPlaceService {

    private final HouseholdService householdService;
    private final DwellingPlaceService dwellingPlaceService;
    private final PersonService personService;
    private final OccupationService occupationService;
    private final ParishPopulator parishPopulator;
    private final TownTemplateService townTemplateService;
    private final PersonGenerator personGenerator;

    public HouseholdDwellingPlaceService(@Autowired @NonNull HouseholdService householdService,
                                         @Autowired @NonNull DwellingPlaceService dwellingPlaceService,
                                         @Autowired @NonNull PersonService personService,
                                         @Autowired @NonNull OccupationService occupationService,
                                         @Autowired @NonNull @Lazy ParishPopulator parishPopulator,
                                         @Autowired @NonNull TownTemplateService townTemplateService,
                                         @Autowired @NonNull PersonGenerator personGenerator) {
        this.householdService = householdService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.personService = personService;
        this.occupationService = occupationService;
        this.parishPopulator = parishPopulator;
        this.townTemplateService = townTemplateService;
        this.personGenerator = personGenerator;
    }

    public DwellingPlace addToDwellingPlace(@NonNull Household household,
                                            @NonNull DwellingPlace dwellingPlace,
                                            @NonNull LocalDate fromDate,
                                            LocalDate toDate) {
        List<HouseholdLocationPeriod> periodsToDelete = new ArrayList<>();
        List<HouseholdLocationPeriod> periodsToCap = new ArrayList<>();
        for (HouseholdLocationPeriod period : household.getDwellingPlaces()) {
            if (period.getFromDate().isBefore(fromDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(fromDate))) {
                periodsToCap.add(period);
            }  else if (period.getFromDate().equals(fromDate) && (
                    (period.getToDate() == null && toDate == null)
                            || (period.getToDate().equals(toDate)))) {
                // If the periods are identical, just change the dwelling place.
                DwellingPlace oldDwellingPlace = period.getDwellingPlace();
                if (oldDwellingPlace.getId() != dwellingPlace.getId()) {
                    oldDwellingPlace.getHouseholdPeriods().remove(period);
                    dwellingPlaceService.save(oldDwellingPlace);
                    period.setDwellingPlace(dwellingPlace);
                    householdService.save(household);
                    householdService.save(period);
                }
                return dwellingPlace;
            } else if (toDate == null && period.getFromDate().isAfter(fromDate)) {
                // If this is an open-ended date range, we should delete any future locations for this household
                periodsToDelete.add(period);
            }
        }

        for (HouseholdLocationPeriod period : periodsToCap) {
            period.setToDate(fromDate);
            period = householdService.save(period);
            household = householdService.save(household);
            period.setDwellingPlace(dwellingPlaceService.save(period.getDwellingPlace()));
        }

        for (HouseholdLocationPeriod periodToDelete : periodsToDelete) {
            household.getDwellingPlaces().remove(periodToDelete);
            DwellingPlace oldDwellingPlace = periodToDelete.getDwellingPlace();
            oldDwellingPlace.getHouseholdPeriods().remove(periodToDelete);
            householdService.delete(periodToDelete);
            household = householdService.save(household);
            dwellingPlaceService.save(oldDwellingPlace);
        }

        HouseholdLocationPeriod newPeriod = new HouseholdLocationPeriod();
        newPeriod.setHouseholdId(household.getId());
        newPeriod.setHousehold(household);
        newPeriod.setDwellingPlace(dwellingPlace);
        newPeriod.setFromDate(fromDate);
        newPeriod.setToDate(toDate);
        household.getDwellingPlaces().add(newPeriod);

        newPeriod = householdService.save(newPeriod);

        dwellingPlace.getHouseholdPeriods().add(newPeriod);
        return dwellingPlaceService.save(dwellingPlace);
    }

    /**
     * Move the household into a house, depending on their occupation and/or social class.
     */
    @Nullable
    public DwellingPlace moveHomelessHouseholdIntoHouse(@NonNull Household household,
                                                        @NonNull LocalDate referenceDate,
                                                        @NonNull LocalDate moveInDate,
                                                        @NonNull DwellingPlaceOwnerPeriod.ReasonToPurchase reason) {
        Person head = household.getHead(referenceDate);
        if (head == null) {
            householdService.resetHeadAsOf(household, referenceDate);
            householdService.save(household);
            head = household.getHead(referenceDate);
            if (head == null) {
                log.warn(String.format("Unable to create a house for %s; the household had no viable head on the date",
                        household.getFriendlyName(referenceDate)));
                return null;
            }
            log.info(String.format("Reset household head of household %s to %d %s", household.getId(),
                    head.getId(), head.getName()));
        }
        Occupation occupation = head.getOccupation(referenceDate);

        log.info(String.format("Looking for house for household of %d %s%s (%s)", head.getId(), head.getName(),
                occupation == null ? "" : " (" + occupation.getName() + ")",
                head.getSocialClass().name()));

        DwellingPlace currentLocation = household.getDwellingPlace(referenceDate);
        if (currentLocation == null) {
            // Should never happen, but we can't create a house in the middle of literal nowhere
            log.warn(String.format("Could not create house for %s: household was not in any location",
                    household.getFriendlyName(moveInDate)));
            return null;
        }
        if (currentLocation.getType() == DwellingPlaceType.DWELLING) {
            // Should never happen; these are supposed to be homeless households
            log.warn(String.format("Not moving household %s; they are already in a house",
                    household.getFriendlyName(moveInDate)));
            return currentLocation;
        }

        if (occupation != null) {
            if (occupation.isDomesticServant()) {
                log.info("Moving domestic servant into house");
                return moveDomesticServantIntoHouse(currentLocation, head, household, moveInDate, reason);
            } else if (occupation.isFarmLaborer()) {
                log.info("Moving farm laborer onto farm");
                return moveFarmLaborerOntoFarm(currentLocation, household, moveInDate, reason);
            } else {
                // All other occupations just get a house
                log.info("Moving household into a new house");
                return moveFamilyIntoNewHouse(currentLocation, household, moveInDate, null, reason.getMessage());
            }
        }

        if (head.getSocialClass() == SocialClass.PAUPER) {
            // Move into a random house
            log.info("Moving pauper into random house");
            return movePauperIntoHouse(currentLocation, household, moveInDate, reason);
        } else {
            // Anyone not a pauper and not employed gets a house
            log.info("Moving household into a new house");
            return moveFamilyIntoNewHouse(currentLocation, household, moveInDate, null, reason.getMessage());
        }
    }

    /**
     * Moves the given household to the parish on the given date.
     *
     * @param parish the parish to move to
     * @param household the household
     * @param date the date to start the residency
     * @param reason the reason to use if a house must be purchased
     * @param prioritizeBestOwnedHouse if true, and if the household head owns a house in any parish, moves to the
     *                                 best owned house instead of trying to buy or find something something new
     * @return events, in some cases
     */
    public List<CalendarDayEvent> buyOrCreateOrMoveIntoEmptyHouse(@NonNull Parish parish,
                                                                  @NonNull Household household,
                                                                  @NonNull LocalDate date,
                                                                  @NonNull DwellingPlaceOwnerPeriod.ReasonToPurchase reason,
                                                                  boolean prioritizeBestOwnedHouse) {
        List<CalendarDayEvent> results = new ArrayList<>();

        Person head = household.getHead(date);
        if (head == null) {
            throw new IllegalArgumentException("The household had no head on the date");
        }

        if (prioritizeBestOwnedHouse && !head.getOwnedDwellingPlaces(date).isEmpty()) {
            DwellingPlace place = head.getOwnedDwellingPlaces(date).stream()
                    .filter(DwellingPlace::isHouse)
                    .max(Comparator.comparing(DwellingPlace::getValue))
                    .orElse(null);
            if (place != null) {
                addToDwellingPlace(household, place, date, null);
                return results;
            }
        }

        double capital = head.getCapital(date) == null ? 0.0 : head.getCapital(date);

        List<Dwelling> buyableHouses = findBuyableHousesFarmsAndEstates(parish, date, capital);
        if (!buyableHouses.isEmpty()) {
            DwellingPlace house = buyableHouses.get(0);
            results.addAll(buyAndMoveIntoHouse((Dwelling) house, head, date, reason.getMessage()));
            return results;
        }

        List<Dwelling> emptyHouses = parish.getEmptyHouses(date);
        if (!emptyHouses.isEmpty()) {
            Collections.shuffle(emptyHouses);
            Dwelling house = emptyHouses.get(0);
            Person owner = house.getOwner(date);
            log.info(String.format("Household is moving into empty house in %s owned by %s",
                    house.getLocationString(), owner == null
                            ? "nobody"
                            : owner.getId() + " " + owner.getName()));
            addToDwellingPlace(household, house, date, null);
            return results;
        }

        // Find a dwelling place in the parish. May purchase if there is an empty one, or build a new
        // house, or...?
        List<DwellingPlace> towns = new ArrayList<>(parish.getRecursiveDwellingPlaces(DwellingPlaceType.TOWN));
        DwellingPlace townOrParish;
        if (towns.isEmpty()) {
            townOrParish = parish;
        } else {
            int whichTown = BetweenDie.roll(1, towns.size() + 1);
            if (whichTown > towns.size()) {
                // add ot parish
                townOrParish = parish;
            } else {
                Collections.shuffle(towns);
                townOrParish = towns.get(0);
            }
        }
        // Either build a house or move into an existing household depending on the social class
        addToDwellingPlace(household, townOrParish, date, null);
        DwellingPlace newHouse = moveHomelessHouseholdIntoHouse(household, date, date, reason);
        if (newHouse != null) {
            Person owner = newHouse.getOwner(date);
            if (newHouse instanceof Dwelling && owner == head) {
                results.add(new NewHouseEvent(date, newHouse));
                return results;
            }
        }

        return results;
    }

    /**
     * Moves the household to the cheapest empty house in the parish, if there is any empty house available, and if
     * the house is cheaper than the current house, if any.
     *
     * @param parish the parish in which to search
     * @param household the household to move
     * @param onDate the date on which to search and move
     */
    void maybeMoveHouseholdToCheapestEmptyHouse(@NonNull Parish parish,
                                                @NonNull Household household,
                                                @NonNull LocalDate onDate) {
        if (household.getInhabitants(onDate).stream().anyMatch(p -> p.isDomesticServant(onDate))) {
            return;
        }
        Dwelling cheapestHouse = parish.getEmptyHouses(onDate).stream()
                .min(Comparator.comparing(Dwelling::getValue))
                .orElse(null);
        Dwelling currentHouse = (Dwelling) household.getDwellingPlace(onDate);
        if (cheapestHouse != null && (currentHouse == null || cheapestHouse.getValue() < currentHouse.getValue())) {
            Person owner = cheapestHouse.getOwner(onDate);
            log.info(String.format("%s is moving into empty house in %s owned by %s on %s",
                    household.getFriendlyName(onDate),
                    cheapestHouse.getLocationString(),
                    owner == null
                            ? "nobody"
                            : owner.getIdAndName(),
                    onDate));
            addToDwellingPlace(household, cheapestHouse, onDate, null);
        }
    }

    /**
     * Moves the person's current household out of its current location, if any. The person will remain in the
     * household, but the household will no longer be in any specific location.
     *
     * @param person the person whose household we will move away (if he is in a household)
     * @param onDate the date on which to move
     */
    void movePersonsHouseholdAway(@NonNull Person person, @NonNull LocalDate onDate) {
        Household hh = person.getHousehold(onDate);
        if (hh == null || person.getResidence(onDate) == null) {
            return;
        }

        HouseholdLocationPeriod period = hh.getHouseholdLocationPeriod(onDate);
        if (period != null) {
            period.setToDate(onDate);
            householdService.save(hh);
        }
    }

    public List<PropertyTransferEvent> buyAndMoveIntoHouse(@NonNull Dwelling house,
                                                           @NonNull Person buyer,
                                                           @NonNull LocalDate date,
                                                           @NonNull String reason) {
        List<PropertyTransferEvent> results = new ArrayList<>();
        if (house.isAttachedToParent() && house.getParent() != null) {
            DwellingPlace parent = house.getParent();
            dwellingPlaceService.buyDwellingPlace(parent, buyer, date, reason);
            if (parent instanceof Estate) {
                personService.maybeUpdateLastNameForNewOwnerOfEstate(buyer, (Estate) parent, date);
            }
            results.add(new PropertyTransferEvent(date, parent));
        }
        dwellingPlaceService.buyDwellingPlace(house, buyer, date, reason);
        house = (Dwelling) addToDwellingPlace(buyer.getHousehold(date), house, date, null);
        results.add(new PropertyTransferEvent(date, house));
        return results;
    }

    /**
     * Find houses that are buyable. They must have no current residents, must not be entailed, and must cost less
     * than the available capital. If the house is attached to a farm or estate, the total value must be checked, as the
     * person would have to buy both. The search is limited to places recursively contained by the given parent place,
     * e.g. a Parish.
     *
     * @param parentPlace the parent place in which to search
     * @param onDate the date on which to search
     * @param availableCapital the amount of money available to buy the house
     * @return the list of empty, affordable houses
     */
    List<Dwelling> findBuyableHousesFarmsAndEstates(@NonNull DwellingPlace parentPlace,
                                                    @NonNull LocalDate onDate,
                                                    double availableCapital) {
        List<Dwelling> emptyHouses = parentPlace.getEmptyHouses(onDate);
        return emptyHouses.stream()
                .filter(h -> h.getNullSafeValueIncludingAttachedParent() < availableCapital
                        && !h.isEntailed()
                        && h.getEntailedTitle() == null)
                .collect(Collectors.toList());
    }

    @Nullable
    Dwelling findBestBuyableHouseFarmOrEstate(@NonNull DwellingPlace parentPlace,
                                              @NonNull LocalDate onDate,
                                              double availableCapital,
                                              double minAcceptableValue) {
        return findBuyableHousesFarmsAndEstates(parentPlace, onDate, availableCapital)
                .stream()
                .filter(d -> d.getValue() >= minAcceptableValue)
                .max(Comparator.comparing(Dwelling::getNullSafeValueIncludingAttachedParent))
                .orElse(null);
    }

    /**
     * Creates a house in a dwelling place and moves a household into it
     *
     * @param dwellingPlace the place, such as a town, parish, or estate
     * @param household the household
     * @param moveInDate the date they begin to inhabit the place
     * @param value if non-null, this value will be used for the house; otherwise a random value will be generated
     *              based on the social class of the head of the household. The value will not be subtracted from the
     *              person's capital
     */
    public Dwelling moveFamilyIntoNewHouse(@NonNull DwellingPlace dwellingPlace,
                                           @NonNull Household household,
                                           @NonNull LocalDate moveInDate,
                                           @Nullable Double value,
                                           @NonNull String reason) {
        Dwelling house = new Dwelling();
        house.setFoundedDate(moveInDate);
        Person head = household.getHead(moveInDate);
        if (head == null) {
            head = household.getInhabitants(moveInDate).stream()
                    .max(Comparator.comparing(Person::getSocialClassRank).reversed())
                    .orElse(null);
        }
        setHouseValueAndMapId(house, dwellingPlace, value, head);
        house = (Dwelling) dwellingPlaceService.save(house);
        dwellingPlace.addDwellingPlace(house);
        dwellingPlaceService.save(dwellingPlace);
        house = (Dwelling) dwellingPlaceService.save(house);
        house = (Dwelling) addToDwellingPlace(household, house, moveInDate, null);
        if (head != null) {
            house.addOwner(head, moveInDate, head.getDeathDate(), reason);
            house = (Dwelling) dwellingPlaceService.save(house);
        }
        log.info(String.format("Moved %s into new house in %s", household.getFriendlyName(moveInDate),
                (dwellingPlace.getName() == null ? "a " + dwellingPlace.getType() : dwellingPlace.getName())));
        return house;
    }

    private void setHouseValueAndMapId(@NonNull Dwelling house,
                                       @NonNull DwellingPlace location,
                                       @Nullable Double desiredValue,
                                       @Nullable Person head) {
        Double value;
        if (desiredValue != null) {
            value = desiredValue;
        } else {
            SocialClass forClass = head == null ? SocialClass.LABORER : head.getSocialClass();
            value = WealthGenerator.getRandomHouseValue(forClass);
        }

        if (location.isTown() && location.getMapId() != null) {
            Pair<String, Double> mapIdAndValue = townTemplateService.getClosestAvailablePolygonForMapId(
                    location.getMapId(), value);
            if (mapIdAndValue != null) {
                house.setMapId(mapIdAndValue.getLeft());
                value = mapIdAndValue.getRight();
            } else {
                log.warn("No empty polygons available for town " + location.getLocationString());
            }
        }

        house.setValue(value);
    }

    /**
     * Create a house on an estate, put the estate into the provided dwelling place, and add the household to the house.
     *
     * @param dwellingPlace the place (such as a parish or town) where the estate should be created
     * @param headOfHousehold the head of the household
     * @param household the household
     * @param moveInDate the date on which the inhabitants should move in
     */
    public DwellingPlace moveGentlemanIntoEstate(@NonNull DwellingPlace dwellingPlace,
                                                  @NonNull Person headOfHousehold,
                                                  @NonNull Household household,
                                                  @NonNull LocalDate moveInDate,
                                                  @NonNull String reason) {
        Estate estate = new Estate();
        estate.setFoundedDate(moveInDate);
        estate.setValue(WealthGenerator.getRandomLandValue(headOfHousehold.getSocialClass()));
        // A 100 acre estate in 1801 sold for about 3500 pounds (35 pounds per acre)
        estate.setAcres(estate.getValue() / 35.0);
        boolean isEntailed = headOfHousehold.isMale() && BetweenDie.roll(1, 100) > 30;
        estate = (Estate) dwellingPlaceService.save(estate);
        estate.setEntailed(isEntailed);
        dwellingPlace.addDwellingPlace(estate);
        dwellingPlaceService.save(dwellingPlace);
        dwellingPlaceService.save(estate);
        estate.addOwner(headOfHousehold, moveInDate, headOfHousehold.getDeathDate(), reason);
        Dwelling manorHouse = new Dwelling();
        manorHouse.setFoundedDate(moveInDate);
        manorHouse.setName(estate.getName());
        manorHouse.setValue(WealthGenerator.getRandomHouseValue(headOfHousehold.getSocialClass()));
        manorHouse.setEntailed(isEntailed);
        manorHouse.setAttachedToParent(true);
        estate.addDwellingPlace(manorHouse);
        manorHouse = (Dwelling) dwellingPlaceService.save(manorHouse);
        estate = (Estate) dwellingPlaceService.save(estate);
        manorHouse.addOwner(headOfHousehold, moveInDate, headOfHousehold.getDeathDate(), reason);
        log.info(String.format("Generated estate '%s' for household of %s, %s", estate.getName(),
                headOfHousehold.getName(), headOfHousehold.getSocialClass().getFriendlyName()));
        manorHouse = (Dwelling) addToDwellingPlace(household, manorHouse, moveInDate, null);
        householdService.save(household);
        return manorHouse;
    }

    /**
     * Find a gentleman's (or higher) house in the same area for this household to move into. If the gentleman lives on
     * an estate or farm, the family may move into its own house. Otherwise may move into the main house.
     */
    private DwellingPlace moveDomesticServantIntoHouse(@NonNull DwellingPlace currentLocation,
                                                       @NonNull Person head,
                                                       @NonNull Household household,
                                                       @NonNull LocalDate moveInDate,
                                                       @NonNull DwellingPlaceOwnerPeriod.ReasonToPurchase reason) {
        // Find a gentleman or higher household
        DwellingPlace parent = currentLocation;

        List<Household> leadingHouseholds;
        do {
            leadingHouseholds = parent.getLeadingHouseholds(moveInDate, SocialClass.GENTLEMAN, true).stream()
                    .filter(h -> h.dwellsInHouse(moveInDate))
                    .collect(Collectors.toList());
        } while (leadingHouseholds.isEmpty() && (parent = parent.getParent()) != null);

        if (leadingHouseholds.isEmpty()) {
            // There were no leading households anywhere. Just create a house.
            return moveFamilyIntoNewHouse(currentLocation, household, moveInDate, null, reason.getMessage());
        }
        Collections.shuffle(leadingHouseholds);
        Household employerHousehold = leadingHouseholds.get(0);
        DwellingPlace employerHouse = employerHousehold.getDwellingPlace(moveInDate);
        DwellingPlace employeeHouseholdParentPlace = employerHouse == null ? null : employerHouse.getParent();
        // The employer household is definitely living in a DWELLING. But if he lives on an ESTATE or FARM, we might
        // want to create a separate house for the household on the estate, rather than moving them into the employer's
        // house. 50% chance he lives in a separate house, unless he is a pauper, in which case he always moves into
        // the main house.
        if (head.getSocialClass().getRank() > SocialClass.PAUPER.getRank() && new Die(2).roll() == 1
                && (employeeHouseholdParentPlace != null && employeeHouseholdParentPlace.isEstateOrFarm())) {
            return moveFamilyIntoNewHouse(employeeHouseholdParentPlace, household, moveInDate, null, reason.getMessage());
        } else {
            // Otherwise add the household directly to the employer's house.
            employerHouse = addToDwellingPlace(household, employerHouse, moveInDate, null);
            log.info(String.format("%s moved into the house of their employer, %s",
                    household.getFriendlyName(moveInDate), employerHousehold.getFriendlyName(moveInDate)));
            return employerHouse;
        }
    }

    /**
     * Find a random farm in this area for the family to move into. They will get their own house on the farm.
     */
    private DwellingPlace moveFarmLaborerOntoFarm(@NonNull DwellingPlace currentLocation,
                                                  @NonNull Household household,
                                                  @NonNull LocalDate moveInDate,
                                                  @NonNull DwellingPlaceOwnerPeriod.ReasonToPurchase reason) {
        return moveHouseholdIntoRandomDwellingPlaceOfType(currentLocation, DwellingPlaceType.FARM, household,
                moveInDate, reason);
    }

    /**
     * Find a random house is this area to move into. The household will move into the house.
     */
    private DwellingPlace movePauperIntoHouse(@NonNull DwellingPlace currentLocation,
                                              @NonNull Household household,
                                              @NonNull LocalDate moveInDate,
                                              @NonNull DwellingPlaceOwnerPeriod.ReasonToPurchase reason) {
        return moveHouseholdIntoRandomDwellingPlaceOfType(currentLocation, DwellingPlaceType.DWELLING, household,
                moveInDate, reason);
    }


    /**
     * Finds a random dwelling place of the specified type for this household to move into. Starts at the current
     * location of the household and broadens the search until at least one random dwelling is found.
     */
    private DwellingPlace moveHouseholdIntoRandomDwellingPlaceOfType(@NonNull DwellingPlace currentLocation,
                                                                     @NonNull DwellingPlaceType type,
                                                                     @NonNull Household household,
                                                                     @NonNull LocalDate moveInDate,
                                                                     @NonNull DwellingPlaceOwnerPeriod.ReasonToPurchase reason)
    {
        DwellingPlace parent = currentLocation;

        List<DwellingPlace> placesOfType;
        do {
            placesOfType = new ArrayList<>(parent.getRecursiveDwellingPlaces(type));
        } while (placesOfType.isEmpty() && (parent = parent.getParent()) != null);

        if (placesOfType.isEmpty()) {
            // There were no places of this type anywhere, just move the household into a house.
            return moveFamilyIntoNewHouse(currentLocation, household, moveInDate, null, reason.getMessage());
        }

        Collections.shuffle(placesOfType);
        if (type == DwellingPlaceType.DWELLING) {
            // Just move the household into the house (you can't put a house inside a house)
            DwellingPlace house = placesOfType.get(0);
            Person houseOwner = house.getOwner(moveInDate);
            log.info(String.format("Moved pauper %s into house of %s", household.getFriendlyName(moveInDate),
                    houseOwner == null ? "id " + house.getId() : houseOwner.getName()));
            return addToDwellingPlace(household, house, moveInDate, null);
        } else {
            // Create a house for the family and place it in the random dwelling place
            return moveFamilyIntoNewHouse(placesOfType.get(0), household, moveInDate, null, reason.getMessage());
        }
    }

    /**
     * This method can be used when a rural household starts a farm. A farm will be created in the place where the house
     * is, and the house will be added to the farm.
     *
     * @param house the house to create a farm around
     * @param onDate the date (used to find the farmer)
     * @return the new farm (or existing farm if it is already on a farm) or null if none could be created.
     */
    @Nullable
    Farm convertRuralHouseToFarm(@NonNull Dwelling house, @NonNull LocalDate onDate, @NonNull List<String> farmNames) {
        if (house.getParent() == null) {
            return null;
        }
        if (house.getParent().isFarm()) {
            return (Farm) house.getParent();
        }
        Person farmer = house.getAllResidents(onDate).stream()
                .filter(p -> personRequiresFarmOnDate(p, onDate))
                .max(Comparator.comparing(p -> ((Person) p).getCapitalNullSafe(onDate)).reversed())
                .orElse(null);
        if (farmer == null) {
            farmer = house.getHouseholds(onDate).stream()
                    .filter(h -> h.getHead(onDate) != null)
                    .map(h -> h.getHead(onDate))
                    .findFirst().orElse(null);
        }
        if (farmer == null) {
            log.warn(String.format("Unable to find an owner for a farm for house %d", house.getId()));
            return null;
        }

        Farm farm = new Farm();
        farm.setFoundedDate(onDate);
        if (farmer.getLastName() != null) {
            farm.setName(farmer.getLastName() + " Farm");
        } else if (!farmNames.isEmpty()) {
            // If the owner has no last name and we have some potential farm names, try to find one that is unused.
            Collections.shuffle(farmNames);
            for (String farmName : farmNames) {
                List<DwellingPlace> placesWithName = dwellingPlaceService.loadByName(farmName + " Farm");
                if (placesWithName == null || placesWithName.isEmpty()) {
                    farm.setName(farmName + " Farm");
                    break;
                }
            }
        }
        double value = WealthGenerator.getRandomLandValue(farmer.getSocialClass());
        farm.setValue(value);
        farmer.addCapital(value * -1, onDate, PersonCapitalPeriod.Reason.builtNewDwellingPlaceMessage(farm));
        personService.save(farmer);

        dwellingPlaceService.save(farm);
        if (house.getOwner(onDate) == null) {
            house.addOwner(farmer, onDate, farmer.getDeathDate(),
                    DwellingPlaceOwnerPeriod.Reason.tookUnownedHouseMessage());
        }
        Person owner = house.getOwner(onDate);
        if (owner != null) {
            farm.addOwner(owner, onDate, owner.getDeathDate(),
                    DwellingPlaceOwnerPeriod.Reason.foundedFarmForRuralHouseMessage());
        }

        house.getParent().addDwellingPlace(farm);
        dwellingPlaceService.save(house.getParent());

        house.setAttachedToParent(true);
        farm.addDwellingPlace(house);
        dwellingPlaceService.save(house);
        dwellingPlaceService.save(farm);
        log.info(String.format("Created %s for house in %s", farm.getName(), house.getParent().getFriendlyName()));
        return farm;
    }

    /**
     * Gets a move-in date for the person. If the person has one or more families, gets the first one and takes the
     * wedding date (if any). Otherwise takes the reference date.
     */
    public LocalDate getMoveInDate(@NonNull Household household, @NonNull LocalDate referenceDate) {
        Person person = household.getHead(referenceDate);
        if (person == null || person.getFamilies().isEmpty()) {
            return referenceDate;
        } else {
            Family family = person.getFamilies().iterator().next();
            return family.getWeddingDate() == null ? referenceDate : family.getWeddingDate();
        }
    }

    private Estate createEstateForHousehold(@NonNull DwellingPlace locationOfEstate,
                                           @NonNull String estateName,
                                           @NonNull String dwellingName,
                                           @NonNull Person owner,
                                           @NonNull Household ownerHousehold,
                                           @NonNull LocalDate onDate,
                                           @Nullable Title entailedToTitle) {
        Dwelling house = (Dwelling) moveGentlemanIntoEstate(locationOfEstate, owner, ownerHousehold, onDate,
                DwellingPlaceOwnerPeriod.ReasonToPurchase.MOVE_TO_PARISH.getMessage());
        Estate estate = (Estate) house.getParent();
        estate.setName(estateName);
        house.setName(dwellingName);

        if (entailedToTitle != null) {
            estate.setEntailedTitle(entailedToTitle);
            house.setEntailedTitle(entailedToTitle);
        }

        dwellingPlaceService.save(house);
        dwellingPlaceService.save(estate);

        return estate;
    }

    /**
     * Creates an estate around an existing house
     * @param house the existing house
     * @param estateName the name of the new estate
     * @param dwellingName the new name of the house
     * @param owner the owner of the new estate (should logically be the owner of the house)
     * @param ownerHousehold the household of the owner
     * @param onDate the date the estate is founded
     * @param entailedToTitle if non-null the estate will be entailed to this title
     * @return the new estate
     */
    private Estate createEstateForHouse(@NonNull Dwelling house,
                                       @NonNull String estateName,
                                       @NonNull String dwellingName,
                                       @NonNull Person owner,
                                       @NonNull Household ownerHousehold,
                                       @NonNull LocalDate onDate,
                                       @Nullable Title entailedToTitle) {

        Estate estate = new Estate();
        estate.setFoundedDate(onDate);
        estate.setName(estateName);
        estate.setValue(WealthGenerator.getRandomLandValue(owner.getSocialClass()));
        // A 100 acre estate in 1801 sold for about 3500 pounds (35 pounds per acre)
        estate.setAcres(estate.getValue() / 35.0);
        boolean isEntailed = owner.isMale() && BetweenDie.roll(1, 100) > 30;
        estate = (Estate) dwellingPlaceService.save(estate);
        estate.setEntailed(isEntailed);

        DwellingPlace dwellingPlace = house.getParent();

        dwellingPlace.addDwellingPlace(estate);
        dwellingPlaceService.save(dwellingPlace);
        dwellingPlaceService.save(estate);
        estate.addOwner(owner, onDate, owner.getDeathDate(),
                DwellingPlaceOwnerPeriod.ReasonToPurchase.CREATED_ESTATE.getMessage());
        house.setName(dwellingName);
        house.setEntailed(isEntailed);
        house.setAttachedToParent(true);
        estate.addDwellingPlace(house);
        house = (Dwelling) dwellingPlaceService.save(house);
        estate = (Estate) dwellingPlaceService.save(estate);
        log.info(String.format("Generated estate '%s' for household of %s, %s", estate.getName(),
                owner.getName(), owner.getSocialClass().getFriendlyName()));
        if (!house.equals(ownerHousehold.getDwellingPlace(onDate))) {
            house = (Dwelling) addToDwellingPlace(ownerHousehold, house, onDate, null);
            householdService.save(ownerHousehold);
        }

        if (entailedToTitle != null) {
            estate.setEntailedTitle(entailedToTitle);
            house.setEntailedTitle(entailedToTitle);
        }

        dwellingPlaceService.save(house);
        dwellingPlaceService.save(estate);

        return estate;
    }

    /**
     * Create an estate for the specified person in the specified place
     *
     * @param parentPlace the place (e.g. Parish) where the estate will be located
     * @param owner the owner of the new estate
     * @param estatePost extra configuration
     * @param onDate the date on which the estate will be created and the owner move in
     * @param entailedToTitle if non-null, the house will be entailed to this title
     * @return the new Estate
     */
    public Estate createEstateInPlace(@NonNull DwellingPlace parentPlace,
                                      @NonNull Person owner,
                                      @NonNull EstatePost estatePost,
                                      @NonNull LocalDate onDate,
                                      @Nullable Title entailedToTitle) {
        Household ownerHousehold = owner.getHousehold(onDate);
        if (ownerHousehold == null) {
            ownerHousehold = householdService.createHouseholdForHead(owner, onDate, true);
        }

        Estate estate = createEstateForHousehold(parentPlace, estatePost.getName(),
                estatePost.getDwellingName(), owner, ownerHousehold, onDate, entailedToTitle);

        if (owner.getCapitalPeriods().isEmpty()) {
            double capital = WealthGenerator.getRandomStartingCapital(owner.getSocialClass(),
                    owner.getOccupation(onDate) != null);
            owner.addCapital(capital, onDate, PersonCapitalPeriod.Reason.startingCapitalMessage());
            log.info(String.format("%d %s got starting capital of %.2f", owner.getId(), owner.getName(), capital));
            personService.save(owner);
        }

        if (estatePost.getMustPurchase() != null && estatePost.getMustPurchase()) {
            owner.addCapital(estate.getValue() * -1, onDate,
                    PersonCapitalPeriod.Reason.builtNewDwellingPlaceMessage(estate));
            personService.save(owner);
        }

        populateEstateWithEmployees(estate, onDate);

        return estate;
    }

    public Estate createEstateAroundDwelling(@NonNull Dwelling house,
                                             @NonNull EstatePost estatePost,
                                             @NonNull LocalDate onDate,
                                             @Nullable Title entailedToTitle) {
        Person owner = house.getOwner(onDate);
        if (owner == null) {
            throw new IllegalArgumentException("The house has no owner on the specified date");
        }

        Household ownerHousehold = owner.getHousehold(onDate);
        if (ownerHousehold == null) {
            ownerHousehold = householdService.createHouseholdForHead(owner, onDate, true);
        }

        Estate estate = createEstateForHouse(house, estatePost.getName(), estatePost.getDwellingName(), owner,
                ownerHousehold, onDate, entailedToTitle);

        if (estatePost.getMustPurchase() != null && estatePost.getMustPurchase()) {
            owner.addCapital(estate.getValue() * -1, onDate,
                    PersonCapitalPeriod.Reason.builtNewDwellingPlaceMessage(estate));
            personService.save(owner);
        }

        populateEstateWithEmployees(estate, onDate);

        return estate;
    }

    private void populateEstateWithEmployees(@NonNull Estate estate, @NonNull LocalDate onDate) {

        RandomFamilyParameters parameters = new RandomFamilyParameters();
        parameters.setReferenceDate(onDate);

        List<Occupation> farmLaborers = occupationService.findByIsFarmLaborer();

        Household household;
        // Add the gardeners, laborers, etc.
        int expectedNumServants = estate.getExpectedNumFarmLaborerHouseholds();
        for (int i = 0; i < expectedNumServants; i++) {
            household = parishPopulator.createHouseholdToFillOccupation(parameters, estate,
                    farmLaborers.get(i % farmLaborers.size()), false, null);
            if (household != null) {
                DwellingPlace currentPlace = household.getDwellingPlace(onDate);
                if (currentPlace != null && !currentPlace.isHouse()) {
                    moveHomelessHouseholdIntoHouse(household, onDate, onDate,
                            DwellingPlaceOwnerPeriod.ReasonToPurchase.MOVE_TO_PARISH);
                }
            }
        }

        // Add the domestic servants that live in the manor house.
        if (estate.getManorHouse() != null) {
            hireDomesticServantsForHouse(estate.getManorHouse(), onDate);
        }
    }

    /**
     * Determines whether this person has an occupation such that he needs to own a farm. (True if he is employed on
     * this date and the occupation has isFarmOwner true.
     * @param person the person to check
     * @param onDate the date to check his occupation
     * @return true if he should own a farm
     */
    private boolean personRequiresFarmOnDate(@NonNull Person person, @NonNull LocalDate onDate) {
        Occupation occ = person.getOccupation(onDate);
        return occ != null && occ.isFarmOwner();
    }

    /**
     * On the given date, figure out how many of various types of estate employees this estate is supposed to have,
     * and hire more if necessary.
     *
     * @param estate the estate
     * @param date the date on which to check the occupations
     * @param numExpectedServants the total number of servants expected
     * @param servantOccupations the list of occupations we should hire for
     */
    void hireEstateEmployees(@NonNull Estate estate,
                             @NonNull LocalDate date,
                             int numExpectedServants,
                             @NonNull List<Occupation> servantOccupations) {
        Map<Occupation, List<Person>> peopleWithOccupations = estate.getPeopleWithOccupations(date);
        int count = 0;
        for (Occupation servantOccupation : servantOccupations) {
            if (peopleWithOccupations.containsKey(servantOccupation)) {
                count += peopleWithOccupations.get(servantOccupation).size();
            }
        }
        int servantsToHire = numExpectedServants - count;
        for (int i = 0; i < servantsToHire; i++) {
            Occupation occupation = servantOccupations.get(new Die(servantOccupations.size()).roll() - 1);
            hireEstateEmployeeForOccupation(estate, occupation, date);
        }
    }

    /**
     * Given an estate and an occupation, hire a person for that occupation on that date. If no valid unemployed,
     * unmarried person can be found, one will be generated.
     */
    private void hireEstateEmployeeForOccupation(@NonNull Estate estate,
                                                 @NonNull Occupation occupation,
                                                 @NonNull LocalDate date) {
        Pair<Person, Household> personHouseholdPair = hirePersonAndPrepareHouseholdToMove(occupation, date);
        Person personToHire = personHouseholdPair.getLeft();
        Household householdToMove = personHouseholdPair.getRight();

        if (occupation.isDomesticServant()) {
            addToDwellingPlace(householdToMove, estate.getManorHouse(), date, null);
        } else {
            moveHiredFarmLaborerOntoEstate(estate, personToHire, householdToMove, date);
        }
    }

    /**
     * Used by the Calendar Service, this class finds servants who are in houses that can no longer afford them and
     * attempts to employee them in other houses who are now able to do so. If there are any left over, they lose their
     * occupation. If there remain houses that need servants, new servants will be recruited or generated. This method
     * differs from hireDomesticServants in that it assumes some houses may need to have servants removed.
     */
    void hireAndFireDomesticServants(@NonNull LocalDate onDate) {

        Map<Occupation, List<Person>> servantsToFire = new HashMap<>();
        Map<Dwelling, Map<Occupation, Integer>> servantsToHire = new HashMap<>();

        for (Dwelling house : dwellingPlaceService.loadHouses()) {
            List<Person> servants = house.getDomesticServants(onDate);
            Double highestHouseholdIncome = house.getHouseholds(onDate).stream()
                    .filter(hh -> hh.mayHireServants(onDate))
                    .mapToDouble(hh -> hh.getIncome(onDate))
                    .max()
                    .orElse(0);
            Map<Occupation, Integer> desiredServants = occupationService
                    .domesticServantsForHouseholdIncome(highestHouseholdIncome);
            for (Person servant : servants) {
                Occupation occ = servant.getOccupation(onDate);
                if (occ == null) {
                    // should never happen
                    continue;
                }
                if (!desiredServants.containsKey(occ)) {
                    log.info(String.format("%s has highest household income of %f and must fire their %s, %s",
                            house.getFriendlyName(), highestHouseholdIncome, occ.getName(), servant.getIdAndName()));
                    if (!servantsToFire.containsKey(occ)) {
                        servantsToFire.put(occ, new ArrayList<>());
                    }
                    servantsToFire.get(occ).add(servant);
                } else {
                    Integer numRequired = desiredServants.get(occ);
                    if (numRequired - 1 <= 0) {
                        desiredServants.remove(occ);
                    } else {
                        desiredServants.put(occ, numRequired - 1);
                    }
                }
            }

            if (!desiredServants.isEmpty()) {
                servantsToHire.put(house, desiredServants);
            }
        }

        // We now have a list of houses looking for servants, and a list of available servants. Hire new servants for
        // houses looking for them, starting with available servants, if any.
        for (Map.Entry<Dwelling, Map<Occupation, Integer>> houseEntry : servantsToHire.entrySet()) {
            Dwelling house = houseEntry.getKey();
            Map<Occupation, Integer> desiredServants = houseEntry.getValue();
            for (Map.Entry<Occupation, Integer> occEntry : desiredServants.entrySet()) {
                Occupation occ = occEntry.getKey();
                Integer numNeeded = occEntry.getValue();
                for (int i = 0; i < numNeeded; i++) {
                    if (servantsToFire.containsKey(occ) && !servantsToFire.get(occ).isEmpty()) {
                        // A person with this occupation is looking for a new job. Move them in.
                        Person servantToHire = servantsToFire.get(occ).remove(0);
                        if (servantsToFire.get(occ).isEmpty()) {
                            servantsToFire.remove(occ);
                        }
                        log.info(String.format("%s needs a %s and is hiring %s, who is looking for a new position",
                                house.getFriendlyName(), occ.getName(), servantToHire.getIdAndName()));
                        Household householdToMove = prepareEmployeeHouseholdToMove(servantToHire, onDate);
                        addToDwellingPlace(householdToMove, house, onDate, null);
                    } else {
                        log.info(String.format("%s needs a %s",
                                house.getFriendlyName(), occ.getName()));
                        // Hire a person
                        Pair<Person, Household> personToHire = hirePersonAndPrepareHouseholdToMove(occ, onDate);
                        // Add their household to the house
                        addToDwellingPlace(personToHire.getRight(), house, onDate, null);
                    }
                }
            }
        }

        // Any remaining servants lose their job and must move out.
        for (Map.Entry<Occupation, List<Person>> servantEntry : servantsToFire.entrySet()) {
            for (Person servant : servantEntry.getValue()) {
                moveOutOrEmigrateFormerServant(servant, servantEntry.getKey(), onDate);
            }
        }
    }

    /**
     * End the servant's employment and either move them into a new house (but not as an employee) or, if they have no
     * family ties to the parish, move them away entirely.
     *
     * @param servant the person who is losing their job
     * @param formerOccupation the job they used to hold
     * @param onDate the date on which to fire them and make them move
     */
    private void moveOutOrEmigrateFormerServant(@NonNull Person servant,
                                                @NonNull Occupation formerOccupation,
                                                @NonNull LocalDate onDate) {
        servant.quitJob(onDate);
        personService.save(servant);
        DwellingPlace currentDwelling = servant.getDwellingPlace(onDate);
        if (currentDwelling == null) {
            log.info(String.format("%s lost their job as a %s",
                    servant.getIdAndName(), formerOccupation.getName()));
            return;
        }
        // Don't keep randomly generated servants around, they can just move back to wherever they came from.
        if (servant.getFamily() == null && servant.getFamilies().isEmpty()) {
            log.info(String.format("%s lost their job as a %s and is moving away",
                    servant.getIdAndName(), formerOccupation.getName()));
            householdService.endPersonResidence(servant.getHousehold(onDate), servant, onDate);
            return;
        }

        log.info(String.format("%s lost their job as a %s and must move to a new house",
                servant.getIdAndName(), formerOccupation.getName()));
        Parish currentParish = currentDwelling.getParish();
        buyOrCreateOrMoveIntoEmptyHouse(currentParish, prepareEmployeeHouseholdToMove(servant, onDate),
                onDate, DwellingPlaceOwnerPeriod.ReasonToPurchase.EVICTION, true);
    }


    /**
     * Used by the Parish Populator, this method loads all houses with at least one person of high enough social
     * class, and hires any domestic servants that household needs to bring it up to the expectations of the class
     * and the income level.
     */
    public void hireDomesticServants(@NonNull LocalDate date) {
        // Find all dwellings that contain at least one household where there is at least one person of Yeoman class or
        // higher.
        List<Dwelling> houses = dwellingPlaceService.loadHouses().stream()
                .filter(h -> h.mayHireServants(date))
                .collect(Collectors.toList());

        for (Dwelling house : houses) {
            hireDomesticServantsForHouse(house, date);
        }
    }

    /**
     * Given a house and a date, determines whether any households have at least a Yeoman (the min social class that may
     * hire servants) and if so, based on the household income, hires any servants necessary so that the house has
     * enough servants of each type. If multiple wealthy households live in the same house, they effectively share
     * servants, though their income is not combined to determine how many servants they can have.
     *
     * @param house the house containing 0 or more households on the date
     * @param date the date to check for servants, and the date to start their service
     */
    private void hireDomesticServantsForHouse(@NonNull Dwelling house, @NonNull LocalDate date) {
        List<Household> households = house.getHouseholds(date).stream()
                .filter(hh -> hh.mayHireServants(date))
                .collect(Collectors.toList());
        Map<Occupation, List<Person>> existingServantsInHouse = house.getPeopleWithOccupations(date);
        for (Household household : households) {
            double income = household.getIncome(date);
            if (income > 0) {
                Map<Occupation, Integer> desiredServants = occupationService.domesticServantsForHouseholdIncome(income);
                for (Map.Entry<Occupation, Integer> entry : desiredServants.entrySet()) {
                    Occupation occupation = entry.getKey();
                    List<Person> servantsInHouse = existingServantsInHouse.get(occupation);
                    int numServantsInHouse = servantsInHouse == null ? 0 : servantsInHouse.size();
                    int servantsToHire = entry.getValue() - numServantsInHouse;
                    for (int i = 0; i < servantsToHire; i++) {
                        log.info(String.format("%s needs to hire a %s", house.getFriendlyName(), occupation.getName()));
                        // Hire the person
                        Pair<Person, Household> personToHire = hirePersonAndPrepareHouseholdToMove(occupation, date);
                        // Add their household to the house
                        addToDwellingPlace(personToHire.getRight(), house, date, null);
                        // Add them to the list we use to keep track so that a second household in the house does
                        // not try to hire for this position again.
                        if (!existingServantsInHouse.containsKey(occupation)) {
                            existingServantsInHouse.put(occupation, new ArrayList<>());
                        }
                        existingServantsInHouse.get(occupation).add(personToHire.getLeft());
                    }
                }
            }
        }
    }

    /**
     * Finds or generates a valid unmarried person to hold the given occupation on the given date. Also determines
     * which household to move into the eventual location, based on whether the person already lives alone in a
     * household or whether they need to move out and/or create a new household.
     *
     * @param occupation the occupation for which to find a candidate
     * @param date the date on which to find the person and hire them for the job
     * @return a pair containing the person and their (possibly new) household
     */
    private Pair<Person, Household> hirePersonAndPrepareHouseholdToMove(@NonNull Occupation occupation,
                                                                        @NonNull LocalDate date) {

        List<Person> people = personService.findUnmarriedPeopleBySocialClassAndGenderAndAge(
                SocialClass.listFromClassToClass(occupation.getMinClass(), occupation.getMaxClass()),
                occupation.getRequiredGender(), 15, 50, date).stream()
                // Filter out people who could expect to earn more from capital than labor.
                .filter(p -> (p.getCapitalNullSafe(date) * 0.04) <
                        WealthGenerator.getYearlyIncomeValueRangeForPersonWithOccupation(p, occupation).getFirst())
                .filter(p -> !p.isEmployedOnOrAfter(date))
                .collect(Collectors.toList());
        List<Person> peopleWhoAlreadyHadOccupation = people.stream()
                .filter(p -> p.getOccupations().stream().anyMatch(pop -> pop.getOccupation().getId() == occupation.getId()))
                .collect(Collectors.toList());
        if (!peopleWhoAlreadyHadOccupation.isEmpty()) {
            people = peopleWhoAlreadyHadOccupation;
        }
        Collections.shuffle(people);
        if (people.isEmpty()) {
            // Generate a new person if none could be found
            Person newPerson = personGenerator.generate(new PersonParameters(occupation, date));
            personService.save(newPerson);
            log.info(String.format("Generated %s to fill occupation of %s", newPerson.getIdAndName(),
                    occupation.getName()));
            people.add(newPerson);
        }
        Person personToHire = people.get(0);
        personToHire.addOccupation(occupation, date);
        personService.save(personToHire);
        log.info(String.format("Hired %s as %s", personToHire.getIdAndName(), occupation.getName()));
        Household householdToMove = prepareEmployeeHouseholdToMove(personToHire, date);

        return Pair.of(personToHire, householdToMove);
    }

    private Household prepareEmployeeHouseholdToMove(@NonNull Person newEmployee, @NonNull LocalDate onDate) {
        Household currentHousehold = newEmployee.getHousehold(onDate);
        Household householdToMove;
        if (currentHousehold != null) {
            Person head = currentHousehold.getHead(onDate);
            if (head != null && head.equals(newEmployee)) {
                householdToMove = currentHousehold;
            } else {
                householdService.endPersonResidence(currentHousehold, newEmployee, onDate);
                householdToMove = householdService.createHouseholdForHead(newEmployee, onDate, false);
            }
        } else {
            householdToMove = householdService.createHouseholdForHead(newEmployee, onDate, false);
        }
        return householdToMove;
    }

    private void moveHiredFarmLaborerOntoEstate(@NonNull Estate estate,
                                                @NonNull Person laborer,
                                                @NonNull Household household,
                                                @NonNull LocalDate onDate) {
        double capital = laborer.getCapitalNullSafe(onDate);
        double minValue = WealthGenerator.getHouseValueRange(laborer.getSocialClass()).getFirst();

        Dwelling buyableHouse = findBestBuyableHouseFarmOrEstate(estate, onDate, capital, minValue);
        if (buyableHouse != null) {
            buyAndMoveIntoHouse(buyableHouse, laborer, onDate,
                    DwellingPlaceOwnerPeriod.Reason.purchasedHouseUponEmploymentMessage());
            return;
        }

        Dwelling newDwelling = null;
        List<Dwelling> emptyHouses = estate.getEmptyHouses(onDate);
        if (!emptyHouses.isEmpty()) {
            Collections.shuffle(emptyHouses);
            newDwelling = emptyHouses.get(0);
        } else {
            List<DwellingPlace> allDwellings = new ArrayList<>(estate.getRecursiveDwellingPlaces(DwellingPlaceType.DWELLING));
            if (allDwellings.isEmpty()) {
                Collections.shuffle(allDwellings);
                newDwelling = (Dwelling) allDwellings.get(0);
            }
        }

        if (newDwelling != null) {
            Person houseOwner = newDwelling.getOwner(onDate);
            log.info(String.format("Moved new farm laborer %d %s into house on estate, owned by %s",
                    laborer.getId(), laborer.getName(),
                    houseOwner == null ? "nobody" : houseOwner.getName()));
            addToDwellingPlace(household, newDwelling, onDate, null);
        } else {
            moveFamilyIntoNewHouse(estate, household, onDate, null,
                    DwellingPlaceOwnerPeriod.Reason.purchasedHouseUponEmploymentMessage());
        }
    }
}
