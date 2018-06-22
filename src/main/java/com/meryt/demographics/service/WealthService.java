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
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.repository.PersonCapitalRepository;

@Slf4j
@Service
public class WealthService {

    private final DwellingPlaceService dwellingPlaceService;
    private final PersonService personService;
    private final PersonCapitalRepository personCapitalRepository;

    public WealthService(@NonNull @Autowired DwellingPlaceService dwellingPlaceService,
                         @NonNull @Autowired PersonService personService,
                         @NonNull @Autowired PersonCapitalRepository personCapitalRepository) {
        this.dwellingPlaceService = dwellingPlaceService;
        this.personService = personService;
        this.personCapitalRepository = personCapitalRepository;
    }

    public void distributeCapital(@NonNull LocalDate onDate, double goodYearFactor) {
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
        // Estates have a rate of return of -5% to 5%. Farms have a rate of -10% to 10%.
        int minReturn = estate.getType() == DwellingPlaceType.FARM ? -10 : -5;
        int maxReturn = estate.getType() == DwellingPlaceType.FARM ? 10 : 5;

        double value = estate.getValue();
        double individualFactor = (new BetweenDie()).roll(minReturn, maxReturn) * 0.01;
        double baseRent = value * individualFactor;
        double adjustedRent = baseRent * (1.0 + goodYearFactor);

        double individualRent = adjustedRent / owners.size();
        for (Person owner : owners) {
            owner.addCapital(individualRent, onDate);
            log.info(String.format("%d %s received %.2f from rents on %d %s", owner.getId(), owner.getName(),
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
        double adjustedWage = value * (1.0 + goodYearFactor);
        person.addCapital(adjustedWage, onDate);
        log.info(String.format("%d %s received %.2f from his wages as a %s", person.getId(), person.getName(),
                adjustedWage, occupation.getName()));
        personService.save(person);
    }

    private void distributeInterestOnCapital(@NonNull Person person, @NonNull LocalDate onDate) {
        Double currentCapital = person.getCapital(onDate);
        if (currentCapital == null) {
            return;
        }

        double rateOfReturn = new BetweenDie().roll(-2, 4) * 0.01;
        double interest = currentCapital * rateOfReturn;
        person.addCapital(interest, onDate);
        log.info(String.format("%d %s received %.2f from interest on capital", person.getId(), person.getName(),
                interest));
        personService.save(person);
    }

}
