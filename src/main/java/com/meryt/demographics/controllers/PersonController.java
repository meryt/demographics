package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.request.PersonTitlePost;
import com.meryt.demographics.response.PersonDetailResponse;
import com.meryt.demographics.response.PersonTitleResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.PersonService;
import com.meryt.demographics.service.TitleService;

@RestController
public class PersonController {

    private final PersonGenerator personGenerator;

    private final PersonService personService;

    private final TitleService titleService;

    public PersonController(@Autowired PersonGenerator personGenerator,
                            @Autowired PersonService personService,
                            @Autowired TitleService titleService) {
        this.personGenerator = personGenerator;
        this.personService = personService;
        this.titleService = titleService;
    }

    @RequestMapping("/api/persons/random")
    public PersonDetailResponse randomPerson(@RequestBody PersonParameters personParameters) {
        PersonParameters params = personParameters == null ? new PersonParameters() : personParameters;
        return new PersonDetailResponse(personGenerator.generate(params), null);
    }

    @RequestMapping("/api/persons/{personId}")
    public PersonDetailResponse getPerson(@PathVariable long personId,
                                    @RequestParam(value = "onDate", required = false) String onDate) {
        Person person = loadPerson(personId);
        LocalDate date = null;
        if (onDate != null) {
            date = LocalDate.parse(onDate);
        }
        return new PersonDetailResponse(person, date);
    }

    @RequestMapping("/api/persons/{personId}/titles")
    public List<PersonTitleResponse> getPersonTitles(@PathVariable long personId,
                                                     @RequestParam(value = "onDate", required = false) String onDate) {
        Person person = loadPerson(personId);
        LocalDate date = null;
        if (onDate != null) {
            date = LocalDate.parse(onDate);
        }
        List<PersonTitlePeriod> titles;
        if (date != null) {
            titles = person.getTitles(date);
        } else {
            titles = person.getTitles();
        }

        return titles.stream()
                .map(PersonTitleResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Adds or updates a title to a person
     * @param personId the person
     * @param personTitlePost the request containing the title ID, from, and optionally to date
     * @return the list of the person's titles, after the title is added/updated
     */
    @RequestMapping(value = "/api/persons/{personId}/titles", method = RequestMethod.POST)
    public List<PersonTitleResponse> postPersonTitle(@PathVariable long personId,
                                                     @RequestBody PersonTitlePost personTitlePost) {
        personTitlePost.validate();
        Person person = loadPerson(personId);

        final Title title = titleService.load(personTitlePost.getTitleId());
        if (title == null) {
            throw new ResourceNotFoundException("No title found for ID " + personTitlePost.getTitleId());
        }

        person.addOrUpdateTitle(title, personTitlePost.getFromDate(), personTitlePost.getToDate());

        personService.save(person);

        return getPersonTitles(personId, null);
    }

    @NonNull
    private Person loadPerson(Long personId) {
        if (personId == null) {
            throw new BadRequestException("person ID may not be null");
        }

        Person person = personService.load(personId);
        if (person == null) {
            throw new ResourceNotFoundException("No person found for ID " + personId);
        }
        return person;
    }

}
