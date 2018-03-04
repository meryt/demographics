package com.meryt.demographics.generator;

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
        person.setLifespanInDays(lifeTableService.randomLifeExpectancy(LifeTableService.LifeTablePeriod.VICTORIAN));

        return person;
    }
}
