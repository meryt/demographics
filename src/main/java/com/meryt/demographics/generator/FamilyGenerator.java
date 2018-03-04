package com.meryt.demographics.generator;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.request.PersonParameters;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class FamilyGenerator {

    private final PersonGenerator personGenerator;

    public FamilyGenerator(@Autowired PersonGenerator personGenerator) {
        this.personGenerator = personGenerator;
    }

    public Family generate(@NonNull FamilyParameters familyParameters) {
        Family family = new Family();

        family.setHusband(generateHusband(familyParameters));
        family.setWife(generateWife(family.getHusband(), familyParameters));

        return family;
    }

    private Person generateHusband(FamilyParameters familyParameters) {
        validate(familyParameters);

        LocalDate targetDate = familyParameters.getReferenceDate();

        int minAge = familyParameters.getMinHusbandAgeOrDefault();
        int maxAge = familyParameters.getMaxHusbandAgeOrDefault();

        PersonParameters personParameters = new PersonParameters();
        personParameters.setGender(Gender.MALE);
        personParameters.setAliveOnDate(targetDate);
        personParameters.setMinAge(minAge);
        personParameters.setMaxAge(maxAge);

        return personGenerator.generate(personParameters);
    }

    private Person generateWife(@NonNull Person husband, @NonNull FamilyParameters familyParameters) {
        int minAge = familyParameters.getMinWifeAgeOrDefault();
        int maxAge = familyParameters.getMaxWifeAgeOrDefault(husband.getAgeInYears(familyParameters.getReferenceDate()));

        PersonParameters personParameters = new PersonParameters();
        personParameters.setGender(Gender.FEMALE);
        personParameters.setAliveOnDate(familyParameters.getReferenceDate());
        personParameters.setMinAge(minAge);
        personParameters.setMaxAge(maxAge);

        return personGenerator.generate(personParameters);
    }

    private void validate(FamilyParameters familyParameters) {
        if (familyParameters.getReferenceDate() == null) {
            throw new IllegalArgumentException("referenceDate is required for generating a family.");
        }
    }
}
