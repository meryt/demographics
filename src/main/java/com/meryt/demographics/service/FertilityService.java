package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.generator.family.PregnancyChecker;
import com.meryt.demographics.generator.person.FertilityGenerator;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.repository.MaternityRepository;
import com.meryt.demographics.response.calendar.CalendarDayEvent;

@Service
public class FertilityService {

    private final FamilyService familyService;
    private final PersonService personService;
    private final PersonGenerator personGenerator;
    private final MaternityRepository maternityRepository;
    private final HouseholdService householdService;

    public FertilityService(@Autowired @NonNull FamilyService familyService,
                            @Autowired @NonNull PersonService personService,
                            @Autowired @NonNull PersonGenerator personGenerator,
                            @Autowired @NonNull MaternityRepository maternityRepository,
                            @Autowired @NonNull HouseholdService householdService) {
        this.familyService = familyService;
        this.personService = personService;
        this.personGenerator = personGenerator;
        this.maternityRepository = maternityRepository;
        this.householdService = householdService;
    }

    public List<CalendarDayEvent> cycleToDate(@NonNull Person woman, @NonNull LocalDate toDate) {
        if (!woman.isFemale()) {
            throw new IllegalArgumentException("cycleToDate can only operate on women");
        }
        if (woman.getFertility() == null) {
            woman.setMaternity(generateMaternity(woman));
            woman = personService.save(woman);
        }

        List<CalendarDayEvent> results = new ArrayList<>();
        final Maternity maternity = (Maternity) woman.getFertility();

        if (maternity.getLastCheckDate() == null) {
            maternity.setLastCheckDate(maternity.getLastCycleDate());
        }
        if (toDate.isBefore(maternity.getLastCheckDate())) {
            return results;
        }

        Family family;
        // Track the number of births. If this number changes, we should save the family since children have been born.
        int currentNumBirths = maternity.getNumBirths();
        if (maternity.getFather() != null) {
            Optional<Family> familyOptional = woman.getFamilies().stream()
                    .filter(f -> f.getHusband() != null && f.getHusband().getId() == maternity.getFather().getId())
                    .findFirst();
            if (!familyOptional.isPresent()) {
                family = new Family();
                family.setHusband(maternity.getFather());
                family.setWife(woman);
            } else {
                family = familyOptional.get();
            }
        } else {
            family = new Family();
            family.setWife(woman);
        }

        PregnancyChecker pregnancyChecker = new PregnancyChecker(personGenerator, family, true);
        results.addAll(pregnancyChecker.checkDateRange(maternity.getLastCheckDate(), toDate));
        // Save the woman which saves the maternity record
        maternity.setPerson(woman);
        for (Person child : family.getChildren()) {
            if (child.getId() == 0) {
                Household motherHousehold = woman.getHousehold(child.getBirthDate());
                child = personService.save(child);
                if (motherHousehold != null && child.isLiving(child.getBirthDate().plusDays(1))) {
                    householdService.addPersonToHousehold(child, motherHousehold, child.getBirthDate(), false);
                    personService.save(child);
                }
            }
        }
        if (maternity.getNumBirths() != currentNumBirths) {
            familyService.save(family);
        }

        // Remove father if the woman is not pregnant and he is dead. This can be used to optimize which women are
        // loaded for cycling to date.
        if (!maternity.isPregnant(toDate)) {
            Person father = maternity.getFather();
            if (father != null && !father.isLiving(toDate)) {
                maternity.setFather(null);
            }
        }

        woman.setMaternity(maternityRepository.save(maternity));
        personService.save(woman);
        return results;
    }

    private Maternity generateMaternity(@NonNull Person woman) {
        FertilityGenerator fertilityGenerator = new FertilityGenerator();
        return fertilityGenerator.randomMaternity(woman);
    }

}
