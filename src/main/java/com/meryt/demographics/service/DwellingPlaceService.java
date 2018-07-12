package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.repository.DwellingPlaceRepository;

@Service
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

    /**
     * Sells a dwelling place. The previous owner or owners get the value of the dwelling place from the buyer.
     *
     * @param place the place for sale
     * @param buyer the buyer (must be alive and have enough capital to buy)
     * @param onDate the date on which the transfer of property and money takes place
     */
    void buyDwellingPlace(@NonNull DwellingPlace place, @NonNull Person buyer, @NonNull LocalDate onDate) {
        if (!buyer.isLiving(onDate)) {
            throw new IllegalArgumentException(String.format("%d %s cannot buy %s %d because he is not alive on %s",
                    buyer.getId(), buyer.getName(), place.getType().getFriendlyName(), place.getId(), onDate));
        }
        if (buyer.getCapital(onDate) < place.getValue()) {
            throw new IllegalArgumentException(String.format("%d %s cannot buy %s %d because he cannot afford it.",
                    buyer.getId(), buyer.getName(), place.getType().getFriendlyName(), place.getId()));
        }

        List<Person> currentOwners = place.getOwners(onDate);
        for (DwellingPlaceOwnerPeriod period : place.getOwnerPeriods()) {
            if (period.contains(onDate)) {
                period.setToDate(onDate);
            }
        }
        place.addOwner(buyer, onDate, buyer.getDeathDate());
        save(place);

        for (Person previousOwner : currentOwners) {
            previousOwner.addCapital(place.getValue() / currentOwners.size(), onDate);
            personService.save(previousOwner);
        }

        buyer.addCapital(place.getValue() * -1, onDate);
        personService.save(buyer);
    }

}
