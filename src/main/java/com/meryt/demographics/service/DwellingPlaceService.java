package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.repository.DwellingPlaceRepository;
import com.meryt.demographics.response.calendar.PropertyTransferEvent;

@Service
@Slf4j
public class DwellingPlaceService {

    private final DwellingPlaceRepository dwellingPlaceRepository;
    private final PersonService personService;

    public DwellingPlaceService(@Autowired DwellingPlaceRepository dwellingPlaceRepository,
                                @Autowired @NonNull PersonService personService) {
        this.dwellingPlaceRepository = dwellingPlaceRepository;
        this.personService = personService;
    }

    public DwellingPlace save(@NonNull DwellingPlace dwellingPlace) {
        return dwellingPlaceRepository.save(dwellingPlace);
    }

    /**
     * Finds a place by ID or returns null if none found
     */
    @Nullable
    public DwellingPlace load(long placeId) {
        return dwellingPlaceRepository.findById(placeId).orElse(null);
    }

    public List<DwellingPlace> loadByType(DwellingPlaceType type) {
        return dwellingPlaceRepository.findByType(type);
    }

    List<DwellingPlace> loadByName(@NonNull String name) {
        return dwellingPlaceRepository.findByName(name);
    }

    public List<DwellingPlace> getUnownedHousesEstatesAndFarms(@NonNull LocalDate onDate) {
        List<DwellingPlace> houses = new ArrayList<>();
        for (DwellingPlace parish : loadByType(DwellingPlaceType.PARISH)) {
            houses.addAll(parish.getUnownedHousesEstatesAndFarms(onDate));
        }
        houses.sort(Comparator.comparing(DwellingPlace::getId));
        return houses;
    }

    private List<DwellingPlace> getPlacesAttachedToParents() {
        return dwellingPlaceRepository.findByParentIsNotNullAndAttachedToParentIsTrue();
    }

    List<DwellingPlace> getPlacesSeparatedFromParents(@NonNull LocalDate onDate) {
        List<DwellingPlace> results = new ArrayList<>();
        for (DwellingPlace place : getPlacesAttachedToParents()) {
            if (place.getOwners(onDate) != null && place.getParent().getOwners(onDate) == null) {
                results.add(place);
            } else if (place.getOwners(onDate) == null && place.getParent().getOwners(onDate) != null) {
                results.add(place);
            } else if (!place.getOwners(onDate).containsAll(place.getParent().getOwners(onDate))) {
                results.add(place);
            }
        }
        return results;
    }

    /**
     * Sells a dwelling place. The previous owner or owners get the value of the dwelling place from the buyer.
     *
     * @param place the place for sale
     * @param buyer the buyer (must be alive and have enough capital to buy)
     * @param onDate the date on which the transfer of property and money takes place
     */
    void buyDwellingPlace(@NonNull DwellingPlace place,
                          @NonNull Person buyer,
                          @NonNull LocalDate onDate,
                          @NonNull String reason) {
        if (!buyer.isLiving(onDate)) {
            throw new IllegalArgumentException(String.format("%d %s cannot buy %s %d because he is not alive on %s",
                    buyer.getId(), buyer.getName(), place.getType().getFriendlyName(), place.getId(), onDate));
        }
        if (buyer.getCapital(onDate) < place.getValue()) {
            throw new IllegalArgumentException(String.format("%d %s cannot buy %s %d because he cannot afford it.",
                    buyer.getId(), buyer.getName(), place.getType().getFriendlyName(), place.getId()));
        }

        transferDwellingPlaceToPerson(place, buyer, onDate, true, reason);

    }

    public PropertyTransferEvent transferDwellingPlaceToPerson(@NonNull DwellingPlace place,
                                                               @NonNull Person newOwner,
                                                               @NonNull LocalDate onDate,
                                                               boolean transferCapital,
                                                               @NonNull String reason) {
        if (!newOwner.isLiving(onDate)) {
            throw new IllegalArgumentException(String.format("%d %s cannot obtain %s %d because he is not alive on %s",
                    newOwner.getId(), newOwner.getName(), place.getType().getFriendlyName(), place.getId(), onDate));
        }

        if (place.getOwners(onDate).contains(newOwner)) {
            log.info(String.format("%d %s already owns %d %s, not transferring property", newOwner.getId(),
                    newOwner.getName(), place.getId(), place.getFriendlyName()));
            return null;
        }

        List<Person> currentOwners = place.getOwners(onDate);
        for (DwellingPlaceOwnerPeriod period : place.getOwnerPeriods()) {
            if (period.contains(onDate)) {
                period.setToDate(onDate);
            }
        }
        place.addOwner(newOwner, onDate, newOwner.getDeathDate(), reason);
        save(place);

        if (transferCapital) {
            double amountPerOwner = place.getValue() / currentOwners.size();
            for (Person previousOwner : currentOwners) {
                previousOwner.addCapital(amountPerOwner, onDate,
                        PersonCapitalPeriod.Reason.soldPropertyMessage(place, newOwner, amountPerOwner));
                personService.save(previousOwner);
            }

            String capitalReason = PersonCapitalPeriod.Reason.purchasedPropertyMessage(
                    place,
                    currentOwners.isEmpty() ? null : currentOwners.get(0),
                    place.getValue());

            newOwner.addCapital(place.getValue() * -1, onDate, capitalReason);
            personService.save(newOwner);
        }

        return new PropertyTransferEvent(onDate, place);
    }
}
