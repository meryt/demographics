package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.generator.random.BetweenDie;

@Slf4j
@Service
public class WealthService {

    private final DwellingPlaceService dwellingPlaceService;
    private final PersonService personService;

    public WealthService(@NonNull @Autowired DwellingPlaceService dwellingPlaceService,
                         @NonNull @Autowired PersonService personService) {
        this.dwellingPlaceService = dwellingPlaceService;
        this.personService = personService;
    }

    void distributeCapital(@NonNull LocalDate onDate, double goodYearFactor) {
        List<DwellingPlace> estatesAndFarms = dwellingPlaceService.loadByType(DwellingPlaceType.ESTATE);
        estatesAndFarms.addAll(dwellingPlaceService.loadByType(DwellingPlaceType.FARM));

        for (DwellingPlace estateOrFarm : estatesAndFarms) {
            distributeRents(estateOrFarm, onDate, goodYearFactor);
        }

        for (DwellingPlace parish : dwellingPlaceService.loadByType(DwellingPlaceType.PARISH)) {
            Map<Occupation, List<Person>> peopleWithOccupations = parish.getPeopleWithOccupations(onDate);
            for (Map.Entry<Occupation, List<Person>> entry : peopleWithOccupations.entrySet()) {
                for (Person person : entry.getValue()) {
                    distributeWages(entry.getKey(), person, onDate, goodYearFactor);
                }
            }

            List<Person> gentry = parish.getAllResidents(onDate).stream()
                    .filter(p -> p.getSocialClass().getRank() >= SocialClass.YEOMAN_OR_MERCHANT.getRank()
                        && p.getOccupation(onDate) == null)
                    .collect(Collectors.toList());
            for (Person gentleman : gentry) {
                distributeInterestOnCapital(gentleman, onDate);
            }
        }
    }

    private void distributeRents(@NonNull DwellingPlace estate, @NonNull LocalDate onDate, double goodYearFactor) {
        List<Person> owners = estate.getOwners(onDate);
        if (owners.isEmpty()) {
            log.info(String.format("No rents were distributed for %d %s, as it has no owner", estate.getId(),
                    estate.getLocationString()));
            return;
        }
        // Estates have a rate of return of -5% to 10%. Farms have a rate of -10% to 10%.
        int minReturn = estate.getType() == DwellingPlaceType.FARM ? -10 : -5;
        int maxReturn = estate.getType() == DwellingPlaceType.FARM ? 10 : 10;

        double value = estate.getValue();
        double individualFactor = (new BetweenDie()).roll(minReturn * 100, maxReturn * 100) * 0.0001;
        double baseRent = value * individualFactor;
        double adjustedRent = adjustForGoodOrBadYear(baseRent, goodYearFactor);

        double individualRent = adjustedRent / owners.size();
        for (Person owner : owners) {
            owner.addCapital(individualRent, onDate);
            log.debug(String.format("%d %s received %.2f from rents on %d %s", owner.getId(), owner.getName(),
                    individualRent, estate.getId(), estate.getLocationString()));
            personService.save(owner);
        }
    }

    private void distributeWages(@NonNull Occupation occupation,
                                 @NonNull Person person,
                                 @NonNull LocalDate onDate,
                                 double goodYearFactor) {
        Pair<Integer, Integer> range = WealthGenerator.getYearlyIncomeValueRange(person.getSocialClass());
        int value = new BetweenDie().roll(range.getFirst(), range.getSecond());
        double adjustedWage = adjustForGoodOrBadYear(value, goodYearFactor);
        person.addCapital(adjustedWage, onDate);
        log.debug(String.format("%d %s received %.2f from his wages as a %s", person.getId(), person.getName(),
                adjustedWage, occupation.getName()));
        personService.save(person);
    }

    private void distributeInterestOnCapital(@NonNull Person person, @NonNull LocalDate onDate) {
        Double currentCapital = person.getCapital(onDate);
        if (currentCapital == null) {
            return;
        }

        double rateOfReturn = new BetweenDie().roll(-200, 400) * 0.0001;
        double interest = currentCapital * rateOfReturn;
        person.addCapital(interest, onDate);
        log.debug(String.format("%d %s received %.2f from interest on capital", person.getId(), person.getName(),
                interest));
        personService.save(person);
    }

    /**
     * Given a wage or a rent and a "good year" factor indicating how good or bad the year was for business, return
     * an adjusted wage or rent.
     *
     * If the wage or rent is negative but it is a good year (the good year factor is positive) the absolute value
     * of the wage or rent will be reduced, so the person takes a smaller hit.  If it's negative and it's a bad year,
     * the absolute value will be increased, making the hit even worse. The converse is true for a positive value.
     *
     * @param wageOrRent the base rage or rent received this year
     * @param goodYearFactor the factor by which the wage/rent is increased (or decreased if this value is negative) to
     *                       reflect how good or bad for business the year was.
     * @return the adjusted wage or rent
     */
    private double adjustForGoodOrBadYear(double wageOrRent, double goodYearFactor) {
        if (wageOrRent < 0 && goodYearFactor < 0) {
            return wageOrRent * (1.0 - goodYearFactor);
        } else if (wageOrRent < 0 && goodYearFactor >= 0) {
            return wageOrRent * (1.0 - goodYearFactor);
        } else if (wageOrRent >= 0 && goodYearFactor < 0) {
            return wageOrRent * (1.0 + goodYearFactor);
        } else if (wageOrRent >= 0 && goodYearFactor >= 0) {
            return wageOrRent * (1.0 + goodYearFactor);
        } else {
            return wageOrRent * (1.0 + goodYearFactor);
        }
    }

}
