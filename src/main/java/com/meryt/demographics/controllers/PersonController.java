package com.meryt.demographics.controllers;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.person.fertility.Fertility;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.request.PersonFamilyPost;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.request.PersonTitlePost;
import com.meryt.demographics.response.PersonDescendantResponse;
import com.meryt.demographics.response.PersonDetailResponse;
import com.meryt.demographics.response.PersonFamilyResponse;
import com.meryt.demographics.response.PersonPotentialSpouseResponse;
import com.meryt.demographics.response.PersonTitleResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.AncestryService;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.PersonService;
import com.meryt.demographics.service.TitleService;

@RestController
public class PersonController {

    private static final String SOCIAL_CLASS = "socialClass";
    private static final String LAST_NAME = "lastName";

    private final PersonGenerator personGenerator;

    private final PersonService personService;

    private final TitleService titleService;

    private final FamilyGenerator familyGenerator;

    private final FamilyService familyService;

    private final AncestryService ancestryService;

    public PersonController(@Autowired PersonGenerator personGenerator,
                            @Autowired PersonService personService,
                            @Autowired TitleService titleService,
                            @Autowired FamilyGenerator familyGenerator,
                            @Autowired FamilyService familyService,
                            @Autowired AncestryService ancestryService) {
        this.personGenerator = personGenerator;
        this.personService = personService;
        this.titleService = titleService;
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.ancestryService = ancestryService;
    }

    @RequestMapping("/api/persons/random")
    public PersonDetailResponse randomPerson(@RequestBody PersonParameters personParameters) {
        PersonParameters params = personParameters == null ? new PersonParameters() : personParameters;
        try {
            personParameters.validate();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        return new PersonDetailResponse(personGenerator.generate(params), null);
    }

    @RequestMapping(value = "/api/persons/{personId}", method = RequestMethod.GET)
    public PersonDetailResponse getPerson(@PathVariable long personId,
                                          @RequestParam(value = "onDate", required = false) String onDate) {
        Person person = loadPerson(personId);
        LocalDate date = null;
        if (onDate != null) {
            date = LocalDate.parse(onDate);
        }
        return new PersonDetailResponse(person, date);
    }

    @RequestMapping("/api/persons/{personId}/fertility")
    public Fertility getPersonFertility(@PathVariable long personId) {
        Person person = loadPerson(personId);
        return person.getFertility();
    }

    @RequestMapping(value = "/api/persons/{personId}/descendants", method = RequestMethod.GET)
    public PersonDescendantResponse getPersonDescendants(@PathVariable long personId,
                                                         @RequestParam(value = "numGenerations", required = false)
                                                                 Integer numGenerations) {
        Person person = loadPerson(personId);
        return new PersonDescendantResponse(person, 0, numGenerations == null ? 3 : numGenerations);
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

    @RequestMapping("/api/persons/{personId}/families")
    public List<PersonFamilyResponse> getPersonFamilies(@PathVariable long personId) {
        Person person = loadPerson(personId);
        List<PersonFamilyResponse> families = new ArrayList<>();
        for (Family family : person.getFamilies()) {
            families.add(new PersonFamilyResponse(person, family));
        }
        return families;
    }

    @RequestMapping(value = "/api/persons/{personId}/families", method = RequestMethod.POST)
    public ResponseEntity<PersonFamilyResponse> postPersonFamily(@PathVariable long personId,
                                                                 @RequestBody PersonFamilyPost personFamilyPost) {
        personFamilyPost.validate();
        Person person = loadPerson(personId);

        FamilyParameters familyParameters = new FamilyParameters();
        familyParameters.setMinHusbandAge(personFamilyPost.getMinHusbandAge());
        familyParameters.setMinWifeAge(personFamilyPost.getMinWifeAge());
        familyParameters.setReferenceDate(personFamilyPost.getUntilDate() == null
                ? person.getDeathDate()
                : personFamilyPost.getUntilDate());
        familyParameters.setPersist(personFamilyPost.isPersist());
        familyParameters.setSpouseLastName(personFamilyPost.getSpouseLastName());
        if (personFamilyPost.getSpouseId() != null) {
            familyParameters.setSpouse(loadPerson(personFamilyPost.getSpouseId()));
        }
        Family family = familyGenerator.generate(person, familyParameters);
        if (family == null) {
            return new ResponseEntity<>((PersonFamilyResponse) null, HttpStatus.NO_CONTENT);
        } else {
            if (personFamilyPost.isPersist()) {
                family = familyService.save(family);
            }
            return new ResponseEntity<>(new PersonFamilyResponse(person, family), personFamilyPost.isPersist()
                    ? HttpStatus.CREATED
                    : HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/api/persons/{personId}", method = RequestMethod.PATCH)
    public PersonDetailResponse patchPerson(@PathVariable long personId, @RequestBody Map<String, Object> updates) {
        Person person = loadPerson(personId);
        if (updates.containsKey(SOCIAL_CLASS)) {
            if (updates.get(SOCIAL_CLASS) == null) {
                person.setSocialClass(null);
            } else {
                person.setSocialClass(SocialClass.fromEnumName((String) updates.get(SOCIAL_CLASS)));
            }
            updates.remove(SOCIAL_CLASS);
        }
        if (updates.containsKey(LAST_NAME)) {
            person.setLastName((String) updates.get(LAST_NAME));
            updates.remove(LAST_NAME);
        }

        if (!updates.isEmpty()) {
            throw new BadRequestException("No support for PATCHing key(s): " + String.join(", ", updates.keySet()));
        }

        return new PersonDetailResponse(personService.save(person));
    }

    @RequestMapping(value = "/api/persons/{personId}/potential-spouses", method = RequestMethod.GET)
    public List<PersonPotentialSpouseResponse> getPersonPotentialSpouses(@PathVariable long personId,
                                                           @RequestParam(value = "onDate", required = false)
                                                                   String onDate,
                                                           @RequestParam(value = "minHusbandAge", required = false)
                                                                       Integer minHusbandAge,
                                                           @RequestParam(value = "minWifeAge", required = false)
                                                                        Integer minWifeAge) {
        final Person person = loadPerson(personId);
        LocalDate date = parseDate(onDate);
        minHusbandAge = minHusbandAge == null ? FamilyParameters.DEFAULT_MIN_HUSBAND_AGE : minHusbandAge;
        minWifeAge = minWifeAge == null ? FamilyParameters.DEFAULT_MIN_WIFE_AGE : minWifeAge;
        return personService.findPotentialSpouses(person, date, minHusbandAge, minWifeAge).stream()
                .map(spouse -> new PersonPotentialSpouseResponse(person, spouse,
                        ancestryService.calculateRelationship(spouse, person)))
                .filter(resp -> resp.getRelationship() == null || resp.getRelationship().getDegreeOfSeparation() > 4)
                .collect(Collectors.toList());
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

    @Nullable
    private LocalDate parseDate(@Nullable String date) {
        if (date == null) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date: " + e.getMessage());
        }
    }

}
