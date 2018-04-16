package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.generator.family.PregnancyChecker;
import com.meryt.demographics.generator.person.FertilityGenerator;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.repository.MaternityRepository;

@Service
public class FertilityService {

    private final FamilyService familyService;
    private final PersonService personService;
    private final PersonGenerator personGenerator;
    private final MaternityRepository maternityRepository;

    public FertilityService(@Autowired @NonNull FamilyService familyService,
                            @Autowired @NonNull PersonService personService,
                            @Autowired @NonNull PersonGenerator personGenerator,
                            @Autowired @NonNull MaternityRepository maternityRepository) {
        this.familyService = familyService;
        this.personService = personService;
        this.personGenerator = personGenerator;
        this.maternityRepository = maternityRepository;
    }

    public Maternity cycleToDate(@NonNull Person woman, @NonNull LocalDate toDate) {
        if (!woman.isFemale()) {
            throw new IllegalArgumentException("cycleToDate can only operate on women");
        }
        if (woman.getFertility() == null) {
            woman.setMaternity(generateMaternity(woman));
            woman = personService.save(woman);
        }

        final Maternity maternity = (Maternity) woman.getFertility();

        if (maternity.getLastCheckDate() == null) {
            maternity.setLastCheckDate(maternity.getLastCycleDate());
        }
        if (toDate.isBefore(maternity.getLastCheckDate())) {
            return maternity;
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
        pregnancyChecker.checkDateRange(maternity.getLastCheckDate(), toDate);
        // Save the woman which saves the maternity record
        maternity.setPerson(woman);
        for (Person child : family.getChildren()) {
            if (child.getId() == 0) {
                personService.save(child);
            }
        }
        if (maternity.getNumBirths() != currentNumBirths) {
            familyService.save(family);
        }
        woman.setMaternity(maternityRepository.save(maternity));
        personService.save(woman);
        return woman.getMaternity();
    }

    private Maternity generateMaternity(@NonNull Person woman) {
        FertilityGenerator fertilityGenerator = new FertilityGenerator();
        return fertilityGenerator.randomMaternity(woman);
    }

}
