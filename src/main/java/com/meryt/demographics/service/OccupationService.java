package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonOccupationPeriod;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.place.Town;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.repository.OccupationRepository;

@Service
public class OccupationService {

    private final OccupationRepository occupationRepository;

    public OccupationService(@Autowired OccupationRepository occupationRepository) {
        this.occupationRepository = occupationRepository;
    }

    /**
     * Returns a list with the expected occupation slots for a town of this size. There is some randomness involved.
     *
     * @return a list which may contain duplicates if there is more than one slot for a given occupation.
     */
    public List<Occupation> occupationsForTownPopulation(long population) {
        PercentDie percentDie = new PercentDie();
        List<Occupation> results = new ArrayList<>();
        for (Occupation occupation : findAll()) {
            // The result of this calc will be a double like 3.56
            double totalOfType = occupation.getSupportFactor() * population;
            // So there are at least 3 of this type in the population
            int wholeNumber = (int) Math.floor(totalOfType);
            // And there is a 56% chance of another one
            double percent = totalOfType - wholeNumber;
            if (percentDie.roll() <= percent) {
                wholeNumber++;
            }
            // Add the occupation to the list once per time it appears
            for (int i = 0; i < wholeNumber; i++) {
                results.add(occupation);
            }
        }

        return results;
    }

    @Nullable
    public Occupation findAvailableOccupationForPerson(@NonNull Person person, @NonNull LocalDate onDate) {
        Household household = person.getHousehold(onDate);
        if (!person.isLiving(onDate) || person.getOccupation(onDate) != null || household == null
                || household.getDwellingPlace(onDate) == null) {
            return null;
        }
        DwellingPlace householdDwellingPlace = household.getDwellingPlace(onDate);
        while (householdDwellingPlace != null && householdDwellingPlace.getType() != DwellingPlaceType.TOWN
                && householdDwellingPlace.getType() != DwellingPlaceType.PARISH) {
            householdDwellingPlace = householdDwellingPlace.getParent();
        }
        if (!(householdDwellingPlace instanceof Town || householdDwellingPlace instanceof Parish)) {
            return null;
        }

        long townPopulation = householdDwellingPlace.getPopulation(onDate);
        List<Occupation> occupationsForGender = person.isMale()
                ? occupationRepository.findByAllowMaleIsTrue()
                : occupationRepository.findByAllowFemaleIsTrue();

        boolean shouldBeRural = householdDwellingPlace instanceof Parish;
        List<Occupation> occupationsForSocialClass = occupationsForGender.stream()
                .filter(o -> o.getMinClass().getRank() <= person.getSocialClass().getRank()
                        && o.getMaxClass().getRank() >= person.getSocialClass().getRank()
                        && o.isRural() == shouldBeRural)
                .collect(Collectors.toList());

        Collections.shuffle(occupationsForSocialClass);

        // If the father had an occupation (or occupations) put them at the front of the list regardless of social
        // class.
        List<Occupation> fathersOccupation = person.getFather() == null ? null :
                person.getFather().getOccupations().stream()
                    .map(PersonOccupationPeriod::getOccupation)
                    .collect(Collectors.toList());
        if (fathersOccupation != null) {
            fathersOccupation.addAll(occupationsForSocialClass);
            occupationsForSocialClass = fathersOccupation;
        }

        PercentDie percentDie = new PercentDie();
        Map<Occupation, List<Person>> existingOccupationsInTown = householdDwellingPlace.getPeopleWithOccupations(onDate);
        for (Occupation occupation : occupationsForSocialClass) {
            List<Person> peopleWithOccupation = existingOccupationsInTown.get(occupation);
            int peopleWithThisOccupation = (peopleWithOccupation == null) ? 0 : peopleWithOccupation.size();
            double totalOfType = occupation.getSupportFactor() * townPopulation;
            int wholeNumberSupportedByTown = (int) Math.floor(totalOfType);
            if (wholeNumberSupportedByTown > peopleWithThisOccupation) {
                return occupation;
            } else if (wholeNumberSupportedByTown == peopleWithThisOccupation
                    && percentDie.roll() <= (totalOfType - wholeNumberSupportedByTown)) {
                // If there are 3.2 required and exactly 3 at present, there is a 20% chance of getting another one.
                return occupation;
            }
            // No "else". If there are 3.2 required and 4 or more at present, then we return null.
        }

        return null;
    }

    public Iterable<Occupation> findAll() {
        return occupationRepository.findAll();
    }
}
