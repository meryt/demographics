package com.meryt.demographics.generator;

import java.time.LocalDate;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.service.LifeTableService;
import com.meryt.demographics.service.NameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PersonGenerator {

    private final NameService nameService;
    private final LifeTableService lifeTableService;

    public PersonGenerator(@Autowired NameService nameService, @Autowired LifeTableService lifeTableService) {
        this.nameService = nameService;
        this.lifeTableService = lifeTableService;
    }

    public Person generate(PersonParameters personParameters) {
        Person person = new Person();
        person.setGender(personParameters.getGender() == null ? Gender.random() : personParameters.getGender());
        person.setFirstName(nameService.randomFirstName(person.getGender()));
        person.setLastName(nameService.randomLastName());
        person.setSocialClass(SocialClass.random());

        LocalDate aliveOnDate = personParameters.getAliveOnDate();
        Integer minAge = personParameters.getMinAge() == null ? 0 : personParameters.getMinAge();
        if (aliveOnDate != null) {
            // Get a random age such that the person is at least minAge / at most maxAge on this reference date.
            // From this we get a birth date (not the actual lifespan).
            long ageAtReference = lifeTableService.randomLifeExpectancy(LifeTableService.LifeTablePeriod.VICTORIAN,
                    minAge, personParameters.getMaxAge());
            person.setBirthDate(aliveOnDate.minusDays(ageAtReference));

            // Now get a lifespan at least as old as he was determined to be at the reference date
            long lifespan = lifeTableService.randomLifeExpectancy(LifeTableService.LifeTablePeriod.VICTORIAN,
                    (int) Math.ceil(ageAtReference / 365.0), null);
            LocalDate deathDate = person.getBirthDate().plusDays(lifespan);
            // From this we can set a death date and the actual lifespan.
            person.setDeathDate(deathDate);
            person.setLifespanInDays(lifespan);

        } else {
            person.setLifespanInDays(lifeTableService.randomLifeExpectancy(LifeTableService.LifeTablePeriod.VICTORIAN,
                    minAge, personParameters.getMaxAge()));
        }


        return person;
    }
}
