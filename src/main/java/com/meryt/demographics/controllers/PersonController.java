package com.meryt.demographics.controllers;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.response.PersonDetailResponse;
import com.meryt.demographics.response.PersonResponse;
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

    @RequestMapping("/api/persons/random")
    public PersonDetailResponse randomPerson(@RequestBody PersonParameters personParameters) {
        PersonParameters params = personParameters == null ? new PersonParameters() : personParameters;
        return new PersonDetailResponse(personGenerator.generate(params), null);
    }

    @RequestMapping("/api/persons/{personId}")
    public PersonDetailResponse getPerson(@PathVariable long personId,
                                    @RequestParam(value = "onDate", required = false) String onDate) {
        Person result = personService.load(personId);
        if (result == null) {
            throw new ResourceNotFoundException("No person found for ID " + personId);
        }
        LocalDate date = null;
        if (onDate != null) {
            date = LocalDate.parse(onDate);
        }
        return new PersonDetailResponse(result, date);
    }

}
