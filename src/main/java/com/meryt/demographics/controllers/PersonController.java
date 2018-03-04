package com.meryt.demographics.controllers;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.generator.PersonGenerator;
import com.meryt.demographics.request.PersonParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PersonController {

    private final PersonGenerator personGenerator;

    public PersonController(@Autowired PersonGenerator personGenerator) {
        this.personGenerator = personGenerator;
    }

    @RequestMapping("/persons/random")
    public Person randomPerson(@RequestBody PersonParameters personParameters) {
        return personGenerator.generate(personParameters == null ? new PersonParameters() : personParameters);
    }

}