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
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Estate;
import com.meryt.demographics.domain.place.Farm;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.Die;
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

    public HouseholdDwellingPlaceService(@Autowired @NonNull HouseholdService householdService,
                                         @Autowired @NonNull DwellingPlaceService dwellingPlaceService,
                                         @Autowired @NonNull PersonService personService) {
        this.householdService = householdService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.personService = personService;
    }

    public DwellingPlace addHouseholdToDwellingPlaceOnDate(@NonNull DwellingPlace dwellingPlace,
                                                           @NonNull Household household,
                                                           @NonNull LocalDate moveInDate) {
        if (dwellingPlace.getId() <= 0) {
            throw new IllegalStateException(String.format("%s %s should be saved before calling this method",
                    dwellingPlace.getType().name(), dwellingPlace.getName()));
        }
        Person headOfHousehold = household.getHead(moveInDate);
        Occupation occupationOnDate = headOfHousehold == null ? null : headOfHousehold.getOccupation(moveInDate);
        if (headOfHousehold != null && headOfHousehold.getSocialClass().getRank() >= SocialClass.GENTLEMAN.getRank()
                && headOfHousehold.getOccupations().isEmpty()) {
            if (dwellingPlace instanceof Dwelling) {
                return addToDwellingPlace(household, dwellingPlace, moveInDate, null);
            } else {
                // An unemployed gentleman or better moves into an estate rather than directly into the town or parish.
                return moveGentlemanIntoEstate(dwellingPlace, headOfHousehold, household, moveInDate);
            }
        } else if (headOfHousehold != null &&
                (headOfHousehold.getSocialClass().getRank() >= SocialClass.YEOMAN_OR_MERCHANT.getRank()
                        || (occupationOnDate != null &&
                        occupationOnDate.getMinClass().getRank() >= SocialClass.YEOMAN_OR_MERCHANT.getRank()))) {
            // An employed gentleman or a yeoman/merchant moves into a house
            if (dwellingPlace instanceof Dwelling) {
                return addToDwellingPlace(household, dwellingPlace, moveInDate, null);
            } else {
                return moveFamilyIntoNewHouse(dwellingPlace, household, moveInDate);
            }
        } else if (headOfHousehold != null && occupationOnDate != null && occupationOnDate.isFarmOwner() &&
                !(dwellingPlace instanceof Dwelling)) {
            log.info("Moving farmer onto farm");
            // Farm-owners move into a house on a farm
            return moveFarmerOntoFarm(dwellingPlace, headOfHousehold, household, moveInDate);
        } else if (headOfHousehold != null & occupationOnDate != null && occupationOnDate.isRural() &&
                !(dwellingPlace instanceof Dwelling)) {
            // Rural non-farm-owners get a house on an existing farm or estate, if possible, otherwise just a house in
            // the area.
            return moveRuralLaborerOntoEstateOrFarm(dwellingPlace, headOfHousehold, household,
                    moveInDate);
        } else {
            return addToDwellingPlace(household, dwellingPlace, moveInDate, null);
        }
    }

    public DwellingPlace addToDwellingPlace(@NonNull Household household,
                                            @NonNull DwellingPlace dwellingPlace,
                                            @NonNull LocalDate fromDate,
                                            LocalDate toDate) {
        List<HouseholdLocationPeriod> periodsToDelete = new ArrayList<>();
        for (HouseholdLocationPeriod period : household.getDwellingPlaces()) {
            if (period.getFromDate().isBefore(fromDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(fromDate))) {
                period.setToDate(fromDate);
                period = householdService.save(period);
                household = householdService.save(household);
                period.setDwellingPlace(dwellingPlaceService.save(period.getDwellingPlace()));
            }  else if (period.getFromDate().equals(fromDate) && (
                    (period.getToDate() == null && toDate == null)
                            || (period.getToDate().equals(toDate)))) {
                // If the periods are identical, just change the dwelling place.
                DwellingPlace oldDwellingPlace = period.getDwellingPlace();
                if (oldDwellingPlace.getId() != dwellingPlace.getId()) {
                    oldDwellingPlace.getHouseholdPeriods().remove(period);
                    dwellingPlaceService.save(oldDwellingPlace);
                    period.setDwellingPlace(dwellingPlace);
                    //householdLocationRepository.save(period);
                    householdService.save(household);
                    householdService.save(period);
                }
                return dwellingPlace;
            } else if (toDate == null && period.getFromDate().isAfter(fromDate)) {
                // If this is an open-ended date range, we should delete any future locations for this household
                periodsToDelete.add(period);
            }
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
                                                        @NonNull LocalDate moveInDate) {
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
                return moveDomesticServantIntoHouse(currentLocation, head, household, moveInDate);
            } else if (occupation.isFarmLaborer()) {
                log.info("Moving farm laborer onto farm");
                return moveFarmLaborerOntoFarm(currentLocation, household, moveInDate);
            } else {
                // All other occupations just get a house
                log.info("Moving household into a new house");
                return moveFamilyIntoNewHouse(currentLocation, household, moveInDate);
            }
        }

        if (head.getSocialClass() == SocialClass.PAUPER) {
            // Move into a random house
            log.info("Moving pauper into random house");
            return movePauperIntoHouse(currentLocation, household, moveInDate);
        } else {
            // Anyone not a pauper and not employed gets a house
            log.info("Moving household into a new house");
            return moveFamilyIntoNewHouse(currentLocation, household, moveInDate);
        }
    }

    public List<CalendarDayEvent> buyOrCreateHouseForHousehold(@NonNull Parish parish,
                                                         @NonNull Household household,
                                                         @NonNull LocalDate date) {
        List<CalendarDayEvent> results = new ArrayList<>();

        Person head = household.getHead(date);
        if (head == null) {
            throw new IllegalArgumentException("The household had no head on the date");
        }

        double capital = head.getCapital(date) == null ? 0.0 : head.getCapital(date);

        List<DwellingPlace> buyableHouses = parish.getEmptyHouses(date).stream()
                .filter(h -> h.getNullSafeValueIncludingAttachedParent() < capital)
                .collect(Collectors.toList());
        if (buyableHouses.isEmpty()) {
            // Find a dwelling place in the parish. May purchase if there is an empty one, or build a new
            // house, or...?
            List<DwellingPlace> towns = new ArrayList<>(parish.getRecursiveDwellingPlaces(
                    DwellingPlaceType.TOWN));
            DwellingPlace townOrParish;
            if (towns.isEmpty()) {
                townOrParish = parish;
            } else {
                int whichTown = new BetweenDie().roll(1, towns.size() + 1);
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
            DwellingPlace newHouse = moveHomelessHouseholdIntoHouse(household,
                    date, date);
            if (newHouse != null) {
                List<Person> owners = newHouse.getOwners(date);
                if (newHouse instanceof Dwelling && owners != null && owners.contains(head)) {
                    results.add(new NewHouseEvent(date, newHouse));
                    return results;
                }
            }
        } else {
            DwellingPlace house = buyableHouses.get(0);
            if (house.isAttachedToParent() && house.getParent() != null) {
                DwellingPlace parent = house.getParent();
                dwellingPlaceService.buyDwellingPlace(parent, head, date);
                results.add(new PropertyTransferEvent(date, parent, parent.getOwners(date.minusDays(1))));
            }
            dwellingPlaceService.buyDwellingPlace(house, head, date);
            house = addToDwellingPlace(household, house, date, null);
            results.add(new PropertyTransferEvent(date, house, house.getOwners(date.minusDays(1))));
        }

        return results;
    }

    /**
     * Creates a house in a dwelling place and moves a household into it
     *
     * @param dwellingPlace the place, such as a town, parish, or estate
     * @param household the household
     * @param moveInDate the date they begin to inhabit the place
     */
    private DwellingPlace moveFamilyIntoNewHouse(@NonNull DwellingPlace dwellingPlace,
                                                 @NonNull Household household,
                                                 @NonNull LocalDate moveInDate) {
        Dwelling house = new Dwelling();
        Person head = household.getHead(moveInDate);
        if (head == null) {
            head = household.getInhabitants(moveInDate).stream()
                    .max(Comparator.comparing(Person::getSocialClassRank).reversed())
                    .orElse(null);
        }
        if (head != null) {
            house.setValue(WealthGenerator.getRandomHouseValue(head.getSocialClass()));
        } else {
            house.setValue(WealthGenerator.getRandomHouseValue(SocialClass.LABORER));
        }
        house = (Dwelling) dwellingPlaceService.save(house);
        dwellingPlace.addDwellingPlace(house);
        dwellingPlaceService.save(dwellingPlace);
        house = (Dwelling) dwellingPlaceService.save(house);
        house = (Dwelling) addToDwellingPlace(household, house, moveInDate, null);
        if (head != null) {
            house.addOwner(head, moveInDate, head.getDeathDate());
        }
        log.info(String.format("Moved %s into new house in %s", household.getFriendlyName(moveInDate),
                (dwellingPlace.getName() == null ? "a " + dwellingPlace.getType() : dwellingPlace.getName())));
        return house;
    }

    /**
     * Create a house on an estate, put the estate into the provided dwelling place, and add the household to the house.
     *
     * @param dwellingPlace the place (such as a parish or town) where the estate should be created
     * @param headOfHousehold the head of the household
     * @param household the household
     * @param moveInDate the date on which the inhabitants should move in
     */
    private DwellingPlace moveGentlemanIntoEstate(@NonNull DwellingPlace dwellingPlace,
                                                 @NonNull Person headOfHousehold,
                                                 @NonNull Household household,
                                                 @NonNull LocalDate moveInDate) {
        Estate estate = new Estate();
        estate.setValue(WealthGenerator.getRandomLandValue(headOfHousehold.getSocialClass()));
        boolean isEntailed = headOfHousehold.isMale() && new BetweenDie().roll(1, 100) > 30;
        estate = (Estate) dwellingPlaceService.save(estate);
        estate.setEntailed(isEntailed);
        dwellingPlace.addDwellingPlace(estate);
        dwellingPlaceService.save(dwellingPlace);
        dwellingPlaceService.save(estate);
        estate.addOwner(headOfHousehold, moveInDate, headOfHousehold.getDeathDate());
        Dwelling manorHouse = new Dwelling();
        manorHouse.setName(estate.getName());
        manorHouse.setValue(WealthGenerator.getRandomHouseValue(headOfHousehold.getSocialClass()));
        manorHouse.setEntailed(isEntailed);
        manorHouse.setAttachedToParent(true);
        estate.addDwellingPlace(manorHouse);
        manorHouse = (Dwelling) dwellingPlaceService.save(manorHouse);
        estate = (Estate) dwellingPlaceService.save(estate);
        manorHouse.addOwner(headOfHousehold, moveInDate, headOfHousehold.getDeathDate());
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
                                                       @NonNull LocalDate moveInDate) {
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
            return moveFamilyIntoNewHouse(currentLocation, household, moveInDate);
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
            return moveFamilyIntoNewHouse(employeeHouseholdParentPlace, household, moveInDate);
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
                                         @NonNull LocalDate moveInDate) {
        return moveHouseholdIntoRandomDwellingPlaceOfType(currentLocation, DwellingPlaceType.FARM, household, moveInDate);
    }

    /**
     * Find a random house is this area to move into. The household will move into the house.
     */
    private DwellingPlace movePauperIntoHouse(@NonNull DwellingPlace currentLocation,
                                     @NonNull Household household,
                                     @NonNull LocalDate moveInDate) {
        return moveHouseholdIntoRandomDwellingPlaceOfType(currentLocation, DwellingPlaceType.DWELLING, household, moveInDate);
    }


    /**
     * Finds a random dwelling place of the specified type for this household to move into. Starts at the current
     * location of the household and broadens the search until at least one random dwelling is found.
     */
    private DwellingPlace moveHouseholdIntoRandomDwellingPlaceOfType(@NonNull DwellingPlace currentLocation,
                                                                     @NonNull DwellingPlaceType type,
                                                                     @NonNull Household household,
                                                                     @NonNull LocalDate moveInDate) {
        DwellingPlace parent = currentLocation;

        List<DwellingPlace> placesOfType;
        do {
            placesOfType = new ArrayList<>(parent.getRecursiveDwellingPlaces(type));
        } while (placesOfType.isEmpty() && (parent = parent.getParent()) != null);

        if (placesOfType.isEmpty()) {
            // There were no places of this type anywhere, just move the household into a house.
            return moveFamilyIntoNewHouse(currentLocation, household, moveInDate);
        }

        Collections.shuffle(placesOfType);
        if (type == DwellingPlaceType.DWELLING) {
            // Just move the household into the house (you can't put a house inside a house)
            DwellingPlace house = placesOfType.get(0);
            List<Person> houseOwner = house.getOwners(moveInDate);
            log.info(String.format("Moved pauper %s into house of %s", household.getFriendlyName(moveInDate),
                    houseOwner.isEmpty() ? "id " + house.getId() : houseOwner.get(0).getName()));
            return addToDwellingPlace(household, house, moveInDate, null);
        } else {
            // Create a house for the family and place it in the random dwelling place
            return moveFamilyIntoNewHouse(placesOfType.get(0), household, moveInDate);
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
    Farm convertRuralHouseToFarm(@NonNull Dwelling house, @NonNull LocalDate onDate) {
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
        farm.setName(farmer.getLastName() + " Farm");
        double value = WealthGenerator.getRandomLandValue(farmer.getSocialClass());
        farm.setValue(value);
        farmer.addCapital(value * -1, onDate);
        personService.save(farmer);

        dwellingPlaceService.save(farm);
        farm.addOwner(farmer, onDate, farmer.getDeathDate());
        house.getParent().addDwellingPlace(farm);
        dwellingPlaceService.save(house.getParent());

        house.setAttachedToParent(true);
        farm.addDwellingPlace(house);
        dwellingPlaceService.save(house);
        dwellingPlaceService.save(farm);
        log.info(String.format("Created %s for house in %s", farm.getName(), house.getParent().getFriendlyName()));
        return farm;
    }

    private DwellingPlace moveFarmerOntoFarm(@NonNull DwellingPlace dwellingPlace,
                                             @NonNull Person headOfHousehold,
                                             @NonNull Household household,
                                             @NonNull LocalDate moveInDate) {
        Farm farm = new Farm();
        farm.setName(headOfHousehold.getLastName() + " Farm");
        farm.setValue(WealthGenerator.getRandomLandValue(headOfHousehold.getSocialClass()));
        dwellingPlaceService.save(farm);
        farm.addOwner(headOfHousehold, moveInDate, headOfHousehold.getDeathDate());
        dwellingPlace.addDwellingPlace(farm);
        dwellingPlaceService.save(dwellingPlace);
        Dwelling farmHouse = new Dwelling();
        farmHouse.setAttachedToParent(true);
        farmHouse.setValue(WealthGenerator.getRandomHouseValue(headOfHousehold.getSocialClass()));
        farm.addDwellingPlace(farmHouse);
        dwellingPlaceService.save(farmHouse);
        dwellingPlaceService.save(farm);
        farmHouse.addOwner(headOfHousehold, moveInDate, headOfHousehold.getDeathDate());
        farmHouse = (Dwelling) addToDwellingPlace(household, farmHouse, moveInDate, null);
        householdService.save(household);
        log.info(String.format("Created %s in %s", farm.getName(), dwellingPlace.getFriendlyName()));
        return farmHouse;
    }

    private DwellingPlace moveRuralLaborerOntoEstateOrFarm(@NonNull DwellingPlace dwellingPlace,
                                                           @NonNull Person headOfHousehold,
                                                           @NonNull Household household,
                                                           @NonNull LocalDate moveInDate) {
        List<DwellingPlace> farmsInPlace = new ArrayList<>(dwellingPlace.getRecursiveDwellingPlaces(
                DwellingPlaceType.FARM));
        farmsInPlace.addAll(dwellingPlace.getRecursiveDwellingPlaces(DwellingPlaceType.ESTATE));

        Dwelling house = new Dwelling();
        house.setValue(WealthGenerator.getRandomHouseValue(headOfHousehold.getSocialClass()));
        house = (Dwelling) dwellingPlaceService.save(house);
        house = (Dwelling) addToDwellingPlace(household, house, moveInDate, null);
        house.addOwner(headOfHousehold, moveInDate, headOfHousehold.getDeathDate());

        if (farmsInPlace.isEmpty()) {
            dwellingPlace.addDwellingPlace(house);
            dwellingPlaceService.save(dwellingPlace);
            house = (Dwelling) dwellingPlaceService.save(house);
            log.info(String.format("%s could not find a farm so moved into a new house in %s",
                    household.getFriendlyName(moveInDate), dwellingPlace.getFriendlyName()));
        } else {
            Collections.shuffle(farmsInPlace);
            DwellingPlace farmOrEstate = farmsInPlace.get(0);
            farmOrEstate.addDwellingPlace(house);
            dwellingPlaceService.save(farmOrEstate);
            house = (Dwelling) dwellingPlaceService.save(house);
            log.info(String.format("Moved %s into new house on %s", household.getFriendlyName(moveInDate),
                    farmOrEstate.getFriendlyName()));
        }
        return house;
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

    public Estate createEstateForHousehold(@NonNull DwellingPlace locationOfEstate,
                                           @NonNull String estateName,
                                           @NonNull String dwellingName,
                                           @NonNull Person owner,
                                           @NonNull Household ownerHousehold,
                                           @NonNull LocalDate onDate,
                                           @Nullable Title enatailedToTitle) {
        Dwelling house = (Dwelling) moveGentlemanIntoEstate(locationOfEstate, owner, ownerHousehold, onDate);
        Estate estate = (Estate) house.getParent();
        estate.setName(estateName);
        house.setName(dwellingName);

        if (enatailedToTitle != null) {
            estate.setEntailedTitle(enatailedToTitle);
            house.setEntailedTitle(enatailedToTitle);
        }

        dwellingPlaceService.save(house);
        dwellingPlaceService.save(estate);

        return estate;
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
}
