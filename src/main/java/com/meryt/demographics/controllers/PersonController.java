package com.meryt.demographics.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.PersonService;

@RestController
public class PersonController {

    private final PersonGenerator personGenerator;

    private final PersonService personService;

    public PersonController(@Autowired PersonGenerator personGenerator,
                            @Autowired PersonService personService) {
        this.personGenerator = personGenerator;
        this.personService = personService;
    }

    @RequestMapping("/persons/random")
    public Person randomPerson(@RequestBody PersonParameters personParameters) {
        return personGenerator.generate(personParameters == null ? new PersonParameters() : personParameters);
    }

    @RequestMapping("/persons/{personId}")
    public Person getPerson(@PathVariable long personId) {
        Person result = personService.load(personId);
        if (result == null) {
            throw new ResourceNotFoundException("No person found for ID " + personId);
        }
        return result;
    }

}
