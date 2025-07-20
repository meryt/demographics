package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.place.Town;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.repository.OccupationRepository;

@Service
public class OccupationService {

    // The number of servants per household increases up to this income. Afterwards it is assumed people just have
    // more households.
    private static final int MAX_INCOME_FOR_SERVANT_CALCULATION = 4000;

    private final OccupationRepository occupationRepository;

    public OccupationService(@Autowired OccupationRepository occupationRepository) {
        this.occupationRepository = occupationRepository;
    }

    List<Occupation> findByIsDomesticServant() {
        return occupationRepository.findByIsDomesticServantIsTrue();
    }

    List<Occupation> findByIsFarmLaborer() {
        return occupationRepository.findByIsFarmLaborerIsTrue();
    }

    /**
     * Returns a list with the expected occupation slots for a town of this size. There is some randomness involved.
     *
     * @return a list which may contain duplicates if there is more than one slot for a given occupation.
     */
    public List<Occupation> occupationsForTownPopulation(long population) {
        List<Occupation> results = new ArrayList<>();
        for (Occupation occupation : findAll()) {
            // The result of this calc will be a double like 3.56
            double totalOfType = occupation.getSupportFactor() * population;
            // So there are at least 3 of this type in the population
            int wholeNumber = (int) Math.floor(totalOfType);
            // And there is a 56% chance of another one
            double percent = totalOfType - wholeNumber;
            if (PercentDie.roll() <= percent) {
                wholeNumber++;
            }
            // Add the occupation to the list once per time it appears
            for (int i = 0; i < wholeNumber; i++) {
                results.add(occupation);
            }
        }

        return results;
    }

    /**
     * Gets a map of occupations to the minimum yearly income required to support at least 1 servant of this type
     *
     * @param income the household's net yearly income in the past year
     * @return a map of occupations to numbers
     */
    Map<Occupation, Integer> domesticServantsForHouseholdIncome(double income) {
        Map<Occupation, Integer> results = new HashMap<>();
        for (Occupation occ : findByIsDomesticServant()) {
            if (occ.getMinIncomeRequired() != null && occ.getMinIncomeRequired() <= income) {
                double minIncome = occ.getMinIncomeRequired();
                // If the income is greater than the max, just use the max.
                double householdIncome = Math.min(income, MAX_INCOME_FOR_SERVANT_CALCULATION);

                int roundedTotalOfType;
                if (householdIncome == MAX_INCOME_FOR_SERVANT_CALCULATION) {
                    // Avoid a divide by 0 error
                    roundedTotalOfType = occ.getMaxPerHousehold();
                } else {
                    // We range from 1 to N but the % calc needs to use from 0 to N-1. We will add 1 at the end.
                    int numAtMax = occ.getMaxPerHousehold() - 1;

                    double percentOfMax = ((householdIncome - minIncome) / (MAX_INCOME_FOR_SERVANT_CALCULATION - minIncome));

                    double totalOfType = 1 + (percentOfMax * numAtMax);

                    roundedTotalOfType = Long.valueOf(Math.round(totalOfType)).intValue();
                }

                results.put(occ, roundedTotalOfType);
            }
        }
        return results;
    }

    @Nullable
    Occupation findAvailableOccupationForPerson(@NonNull Person person,
                                                @NonNull DwellingPlace householdDwellingPlace,
                                                @NonNull LocalDate onDate) {
        if (!person.isLiving(onDate) || person.getOccupation(onDate) != null) {
            return null;
        }

        while (householdDwellingPlace != null && householdDwellingPlace.getType() != DwellingPlaceType.TOWN
                && householdDwellingPlace.getType() != DwellingPlaceType.PARISH) {
            householdDwellingPlace = householdDwellingPlace.getParent();
        }
        if (!(householdDwellingPlace instanceof Town || householdDwellingPlace instanceof Parish)) {
            return null;
        }

        long population = (householdDwellingPlace instanceof Parish)
                ? ((Parish) householdDwellingPlace).getRuralPopulation(onDate)
                : householdDwellingPlace.getPopulation(onDate);
        List<Occupation> occupationsForGender = person.isMale()
                ? occupationRepository.findByAllowMaleIsTrue()
                : occupationRepository.findByAllowFemaleIsTrue();

        boolean shouldBeRural = householdDwellingPlace instanceof Parish;
        boolean personIsMarried = person.isMarried(onDate);
        List<Occupation> occupationsForSocialClass = occupationsForGender.stream()
                .filter(o -> o.getMinClass().getRank() <= person.getSocialClass().getRank()
                        && o.getMaxClass().getRank() >= person.getSocialClass().getRank()
                        && o.isRural() == shouldBeRural
                        && (!personIsMarried || o.isMayMarry()))
                .collect(Collectors.toList());

        Collections.shuffle(occupationsForSocialClass);

        // If the parent had an occupation (or occupations) put them at the front of the list regardless of social
        // class.
        Person parent = person.isMale() ? person.getFather() : person.getMother();
        List<Occupation> parentsOccupation = parent == null ? null :
                parent.getOccupations().stream()
                    .map(PersonOccupationPeriod::getOccupation)
                    .collect(Collectors.toList());
        if (parentsOccupation != null) {
            parentsOccupation.addAll(occupationsForSocialClass);
            occupationsForSocialClass = parentsOccupation;
        }

        Map<Occupation, List<Person>> existingOccupationsInTown = householdDwellingPlace.getPeopleWithOccupations(onDate);
        for (Occupation occupation : occupationsForSocialClass) {
            List<Person> peopleWithOccupation = existingOccupationsInTown.get(occupation);
            int peopleWithThisOccupation = (peopleWithOccupation == null) ? 0 : peopleWithOccupation.size();
            double totalOfType = occupation.getSupportFactor() * population;
            int wholeNumberSupportedByTown = (int) Math.floor(totalOfType);
            if (wholeNumberSupportedByTown > peopleWithThisOccupation) {
                return occupation;
            } else if (wholeNumberSupportedByTown == peopleWithThisOccupation
                    && PercentDie.roll() <= (totalOfType - wholeNumberSupportedByTown)) {
                // If there are 3.2 required and exactly 3 at present, there is a 20% chance of getting another one.
                return occupation;
            }
            // No "else". If there are 3.2 required and 4 or more at present, then we return null.
        }

        return null;
    }

    private Iterable<Occupation> findAll() {
        return occupationRepository.findAll();
    }
}
