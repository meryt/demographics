package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.RandomFamilyParameters;

@Slf4j
@Service
public class InheritanceService {

    private final FamilyGenerator familyGenerator;
    private final FamilyService familyService;
    private final PersonService personService;
    private final AncestryService ancestryService;
    private final HeirService heirService;

    public InheritanceService(@NonNull FamilyGenerator familyGenerator,
                              @NonNull FamilyService familyService,
                              @NonNull PersonService personService,
                              @NonNull AncestryService ancestryService,
                              @NonNull HeirService heirService) {
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.personService = personService;
        this.ancestryService = ancestryService;
        this.heirService = heirService;
    }



    void processDeath(@NonNull Person person) {
        LocalDate date = person.getDeathDate();
        distributeCashToHeirs(person, date);
        distributeRealEstateToHeirs(person, date);
    }


    private void distributeCashToHeirs(@NonNull Person person, @NonNull LocalDate onDate) {

        PersonCapitalPeriod period = person.getCapitalPeriod(onDate.minusDays(1));
        if (period == null) {
            return;
        }
        period.setToDate(onDate);
        personService.save(person);

        double cash = period.getCapital();
        List<Person> heirs = heirService.findHeirsForCashInheritance(person, onDate);

        if (heirs.isEmpty()) {
            log.info("Failed to find any heirs. Inheritance will be lost.");
            return;
        }

        double cashPerPerson = cash / heirs.size();
        for (Person heir : heirs) {
            Relationship relationship = ancestryService.calculateRelationship(heir, person);
            log.info(String.format("%d %s (%s) received %.2f on %s", heir.getId(), heir.getName(),
                    (relationship == null ? "no relation" : relationship.getName()), cashPerPerson, onDate));
            heir.addCapital(cashPerPerson, onDate);
            personService.save(heir);
        }
    }

    private void distributeRealEstateToHeirs(@NonNull Person person, @NonNull LocalDate onDate) {
        List<DwellingPlace> realEstate = person.getOwnedDwellingPlaces(onDate.minusDays(1));
        if (realEstate.isEmpty()) {
            return;
        }

        boolean ownsEntailedProperty = realEstate.stream().anyMatch(DwellingPlace::isEntailed);
        Person maleHeirForEntailments = null;
        if (ownsEntailedProperty) {
            maleHeirForEntailments = heirService.findMaleHeirForEntailments(person, onDate);
            if (maleHeirForEntailments == null) {
                log.info(String.format(
                        "No male heir found for %s. Entailed dwelling place will go to random new person.",
                        person.getName()));
                maleHeirForEntailments = generateNewOwnerForEntailedDwelling(person, onDate);
            }
        }

        List<DwellingPlace> entailedPlaces = realEstate.stream()
                .filter(DwellingPlace::isEntailed)
                .collect(Collectors.toList());

        List<DwellingPlace> unentailedEstates = realEstate.stream()
                .filter(dp -> !dp.isEntailed() &&
                        (dp.getType() == DwellingPlaceType.FARM || dp.getType() == DwellingPlaceType.ESTATE))
                .sorted(Comparator.comparing(DwellingPlace::getValue).reversed())
                .collect(Collectors.toList());

        List<DwellingPlace> unentailedHouses = realEstate.stream()
                .filter(dp -> !dp.isEntailed() && dp.getType() == DwellingPlaceType.DWELLING)
                .sorted(Comparator.comparing(DwellingPlace::getValue).reversed())
                .collect(Collectors.toList());

        for (DwellingPlace dwelling : entailedPlaces) {
            if (dwelling.isEntailed() && maleHeirForEntailments != null) {
                log.info(String.format("%s is entailed. Giving to male heir %d %s.", dwelling.getFriendlyName(),
                        maleHeirForEntailments.getId(), maleHeirForEntailments.getName()));
                dwelling.addOwner(maleHeirForEntailments, onDate, maleHeirForEntailments.getDeathDate());
                log.info(String.format("%d %s inherits %s %s on %s", maleHeirForEntailments.getId(),
                        maleHeirForEntailments.getName(), dwelling.getType().getFriendlyName(),
                        dwelling.getLocationString(), onDate));
                personService.save(maleHeirForEntailments);
            }
        }

        List<Person> heirs = heirService.findHeirsForRealEstate(person, onDate);
        int i = 0;
        for (DwellingPlace estateOrFarm : unentailedEstates) {
            List<DwellingPlace> places = estateOrFarm.getDwellingPlaces().stream()
                    .filter(dp -> dp.getOwners(onDate.minusDays(1)).contains(person))
                    .collect(Collectors.toList());
            unentailedHouses.removeAll(places);
            Person heir;
            if (heirs.isEmpty()) {
                heir = heirService.findPossibleHeirForDwellingPlace(estateOrFarm, onDate.plusDays(1));
                if (heir == null) {
                    heir = generateNewOwnerForEntailedDwelling(person, onDate);
                }
            } else {
                if (i == heirs.size()) {
                    i = 0;
                }
                heir = heirs.get(i++);
            }
            // Make him the owner of the estate as well as the given places.
            estateOrFarm.addOwner(heir, onDate, heir.getDeathDate());
            log.info(String.format("%d %s inherits %s %s on %s", heir.getId(), heir.getName(),
                    estateOrFarm.getType().getFriendlyName(),
                    estateOrFarm.getLocationString(), onDate));
            for (DwellingPlace estateBuilding : places) {
                log.info(String.format("%d %s inherits %s %s on %s", heir.getId(), heir.getName(),
                        estateBuilding.getType().getFriendlyName(),
                        estateBuilding.getLocationString(), onDate));
                estateBuilding.addOwner(heir, onDate, heir.getDeathDate());
            }
            personService.save(heir);
        }

        for (DwellingPlace house : unentailedHouses) {
            Person heir;
            if (heirs.isEmpty()) {
                heir = heirService.findPossibleHeirForDwellingPlace(house, onDate.plusDays(1));
                if (heir == null) {
                    heir = generateNewOwnerForEntailedDwelling(person, onDate);
                }
            } else {
                if (i == heirs.size()) {
                    i = 0;
                }
                heir = heirs.get(i++);
            }
            log.info(String.format("%d %s inherits %s in %s on %s", heir.getId(), heir.getName(),
                    house.getType().getFriendlyName(),
                    house.getLocationString(), onDate));
            house.addOwner(heir, onDate, heir.getDeathDate());
            personService.save(heir);
        }
    }

    @NonNull
    private Person generateNewOwnerForEntailedDwelling(@NonNull Person formerOwner, @NonNull LocalDate onDate) {
        RandomFamilyParameters familyParameters = new RandomFamilyParameters();
        familyParameters.setReferenceDate(onDate);
        familyParameters.setPercentMaleFounders(1.0);
        familyParameters.setAllowExistingSpouse(true);
        familyParameters.setChanceGeneratedSpouse(0.9);
        familyParameters.setAllowMaternalDeath(true);
        familyParameters.setCycleToDeath(false);
        familyParameters.setMaxSocialClass(formerOwner.getSocialClass().plusOne());
        familyParameters.setMinSocialClass(formerOwner.getSocialClass().minusOne());
        familyParameters.setPersist(true);

        Family family = familyGenerator.generate(familyParameters);
        familyService.save(family);

        return family.getHusband();
    }
}
