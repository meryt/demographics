package com.meryt.demographics.service;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.request.InitialGenerationPost;
import com.meryt.demographics.request.PersonFamilyPost;

/**
 * Service used for processing entire generations of randomly generated people
 */
public class GenerationService {

    private final PersonService personService;
    private final FamilyService familyService;
    private final FamilyGenerator familyGenerator;

    public GenerationService(@Autowired @NonNull PersonService personService,
                             @Autowired @NonNull FamilyService familyService,
                             @Autowired @NonNull FamilyGenerator familyGenerator) {
        this.personService = personService;
        this.familyService = familyService;
        this.familyGenerator = familyGenerator;
    }

    public List<Family> seedInitialGeneration(@NonNull InitialGenerationPost generationPost) {
        List<String> lastNames = generationPost.getLastNames();
        if (lastNames == null) {
            lastNames = new ArrayList<>();
        }
        FamilyParameters familyParameters = generationPost.getFamilyParameters();
        int numFamilies = generationPost.getNumFamilies();
        List<Family> result = new ArrayList<>();
        for (int i = 0; i < numFamilies; i++) {
            if (!lastNames.isEmpty()) {
                int index = new Die(lastNames.size()).roll() - 1;
                String lastName = lastNames.remove(index);
                familyParameters.setFounderLastName(lastName);
            } else {
                familyParameters.setFounderLastName(null);
            }
            Family family = familyGenerator.generate(familyParameters);
            if (family == null) {
                continue;
            }
            if (familyParameters.isPersist()) {
                family = familyService.save(family);
            }
            result.add(family);
        }
        return result;
    }

    public List<Family> processGeneration(@NonNull PersonFamilyPost personFamilyPost) {
        personFamilyPost.validate();
        if (!personFamilyPost.isPersist()) {
            throw new IllegalArgumentException("Cannot process generation unless persist is true");
        }

        List<Person> unfinishedAdultMales = personService.loadUnfinishedMales();

        List<Family> results = new ArrayList<>();
        for (Person person : unfinishedAdultMales) {
            FamilyParameters familyParameters = new FamilyParameters();
            familyParameters.setMinHusbandAge(personFamilyPost.getMinHusbandAge());
            familyParameters.setMinWifeAge(personFamilyPost.getMinWifeAge());
            familyParameters.setReferenceDate(personFamilyPost.getUntilDate() == null
                    ? person.getDeathDate()
                    : personFamilyPost.getUntilDate());
            familyParameters.setPersist(personFamilyPost.isPersist());
            familyParameters.setAllowExistingSpouse(personFamilyPost.isAllowExistingSpouse());
            familyParameters.setMinSpouseSelection(personFamilyPost.getMinSpouseSelection());
            Family family = familyGenerator.generate(person, familyParameters);
            if (family == null) {
                person.setFinishedGeneration(true);
                personService.save(person);
            } else {
                if (family.getWife() != null && family.getWife().getDeathDate().isAfter(person.getDeathDate())) {
                    person.setFinishedGeneration(true);
                    personService.save(person);
                }
                family = familyService.save(family);
                results.add(family);
            }
        }
        return results;
    }

}
