package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.person.fertility.Fertility;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.family.MatchMaker;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.request.PersonFamilyPost;
import com.meryt.demographics.request.PersonFertilityPost;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.request.PersonTitlePost;
import com.meryt.demographics.response.PersonDescendantResponse;
import com.meryt.demographics.response.PersonDetailResponse;
import com.meryt.demographics.response.PersonFamilyResponse;
import com.meryt.demographics.response.PersonHeirResponse;
import com.meryt.demographics.response.PersonPotentialSpouseResponse;
import com.meryt.demographics.response.PersonTitleResponse;
import com.meryt.demographics.response.RelatedPersonResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.AncestryService;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.FertilityService;
import com.meryt.demographics.service.InheritanceService;
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

    private final FertilityService fertilityService;

    private final AncestryService ancestryService;

    private final InheritanceService inheritanceService;

    public PersonController(@Autowired PersonGenerator personGenerator,
                            @Autowired PersonService personService,
                            @Autowired TitleService titleService,
                            @Autowired FamilyGenerator familyGenerator,
                            @Autowired FamilyService familyService,
                            @Autowired FertilityService fertilityService,
                            @Autowired AncestryService ancestryService,
                            @Autowired InheritanceService inheritanceService) {
        this.personGenerator = personGenerator;
        this.personService = personService;
        this.titleService = titleService;
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.fertilityService = fertilityService;
        this.ancestryService = ancestryService;
        this.inheritanceService = inheritanceService;
    }

    @RequestMapping("/api/persons/random")
    public PersonDetailResponse randomPerson(@RequestBody PersonParameters personParameters) {
        PersonParameters params = personParameters == null ? new PersonParameters() : personParameters;
        try {
            params.validate();
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

    @RequestMapping(value = "/api/persons/{personId}/fertility", method = RequestMethod.GET)
    public Fertility getPersonFertility(@PathVariable long personId) {
        Person person = loadPerson(personId);
        return person.getFertility();
    }

    @RequestMapping(value = "/api/persons/{personId}/fertility", method = RequestMethod.POST)
    public Fertility postPersonFertility(@PathVariable long personId, @RequestBody PersonFertilityPost post) {
        Person person = loadPerson(personId);
        LocalDate cycleToDate = post.getCycleToDateAsDate();
        if (cycleToDate != null) {
            if (!person.isFemale()) {
                throw new BadRequestException("cycleToDate applies only to women");
            }
            return fertilityService.cycleToDate(person, cycleToDate);
        }
        return person.getFertility();
    }

    @RequestMapping(value = "/api/persons/{personId}/descendants", method = RequestMethod.GET)
    public PersonDescendantResponse getPersonDescendants(@PathVariable long personId,
                                                         @RequestParam(value = "numGenerations", required = false)
                                                            Integer numGenerations,
                                                         @RequestParam(value = "minAge", required = false)
                                                            Integer minAge,
                                                         @RequestParam(value = "bornBefore", required = false)
                                                            String bornBefore) {
        Person person = loadPerson(personId);
        LocalDate bornBeforeDate = parseDate(bornBefore);
        return new PersonDescendantResponse(person, minAge, bornBeforeDate, 0,
                numGenerations == null ? 3 : numGenerations);
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

    /**
     * Attempt to create a family for a person. Depending on the parameters, this may or may not succeed in finding
     * a spouse for the person, and if it fails, no family will be created and an empty response will be returned.
     *
     * @param personId the person looking to create a family
     * @param personFamilyPost parameters used to generate the family
     * @return a response describing the new family, or an empty response if none was created
     */
    @RequestMapping(value = "/api/persons/{personId}/families", method = RequestMethod.POST)
    public ResponseEntity<PersonFamilyResponse> postPersonFamily(@PathVariable long personId,
                                                                 @RequestBody PersonFamilyPost personFamilyPost) {
        personFamilyPost.validate();
        Person person = loadPerson(personId);

        RandomFamilyParameters familyParameters = new RandomFamilyParameters();
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
        familyParameters.setAllowExistingSpouse(personFamilyPost.isAllowExistingSpouse());
        familyParameters.setMinSpouseSelection(personFamilyPost.getMinSpouseSelection());

        Family family = familyGenerator.generate(person, familyParameters);
        if (family == null) {
            return new ResponseEntity<>((PersonFamilyResponse) null, HttpStatus.NO_CONTENT);
        } else {
            if (personFamilyPost.isPersist()) {
                family = familyService.save(family);
            }

            Relationship relationship = ancestryService.calculateRelationship(person, person.isMale()
                    ? family.getWife() : family.getHusband(), true);

            return new ResponseEntity<>(new PersonFamilyResponse(person, family, relationship),
                    personFamilyPost.isPersist()
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

    /**
     * Finds potential spouses for the person.
     *
     * @param personId the person
     * @param onDate the optional search date; if provided, the results will only include potential spouses who are
     *               eligible on that exact date; otherwise may includes spouses that are not yet old enough but will
     *               become eligible in the future
     * @param minHusbandAge minimum age for men to marry
     * @param minWifeAge minimum age for women to marry
     * @param maxWifeAge maximum age for women to marry
     * @return a list of potential spouses with their relationship to the person
     */
    @RequestMapping(value = "/api/persons/{personId}/potential-spouses", method = RequestMethod.GET)
    public List<PersonPotentialSpouseResponse> getPersonPotentialSpouses(@PathVariable long personId,
                                                           @RequestParam(value = "onDate", required = false)
                                                                   String onDate,
                                                           @RequestParam(value = "minHusbandAge", required = false)
                                                                       Integer minHusbandAge,
                                                           @RequestParam(value = "minWifeAge", required = false)
                                                                        Integer minWifeAge,
                                                           @RequestParam(value = "maxWifeAge", required = false)
                                                                         Integer maxWifeAge) {
        final Person person = loadPerson(personId);
        LocalDate date = parseDate(onDate);
        LocalDate searchDate = MatchMaker.getDateToStartMarriageSearch(person, minHusbandAge, minWifeAge);
        if (date != null && date.isAfter(searchDate)) {
            searchDate = date;
        }

        final LocalDate finalSearchDate = searchDate;

        RandomFamilyParameters familyParameters = new RandomFamilyParameters();
        familyParameters.setMinWifeAge(minWifeAge);
        familyParameters.setMinHusbandAge(minHusbandAge);
        familyParameters.setMaxWifeAge(maxWifeAge);

        // If a specific date is given, include only people eligible on that day. Otherwise include people eligible
        // in the future.
        boolean includeFuture = onDate == null;
        return personService.findPotentialSpouses(person, searchDate, includeFuture, familyParameters).stream()
                .map(pr -> new PersonPotentialSpouseResponse(person, pr.getPerson(), finalSearchDate,
                        pr.getRelationship()))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/persons/{personId}/heirs", method = RequestMethod.GET)
    public List<RelatedPersonResponse> getPersonPotentialHeirs(@PathVariable long personId,
                                                               @RequestParam(value = "onDate", required = false)
                                                                  String onDate,
                                                               @RequestParam(value = "inheritance", required = false)
                                                                  String inheritance) {
        final Person person = loadPerson(personId);
        LocalDate date = parseDate(onDate);
        if (date == null) {
            date = person.getDeathDate();
        }
        TitleInheritanceStyle inheritanceStyle = TitleInheritanceStyle.HEIRS_GENERAL;
        if (!StringUtils.isEmpty(inheritance)) {
            try {
                inheritanceStyle = TitleInheritanceStyle.valueOf(inheritance);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid value for inheritance: " + e.getMessage());
            }
        }
        List<Person> heirs = inheritanceService.findPotentialHeirsForPerson(person, date, inheritanceStyle);
        return heirs.stream()
                .map(p -> new RelatedPersonResponse(p, ancestryService.calculateRelationship(p, person, false)))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/persons/{personId}/heir", method = RequestMethod.GET)
    public PersonHeirResponse getPersonHeir(@PathVariable long personId,
                                            @RequestParam(value = "onDate", required = false)
                                                    String onDate,
                                            @RequestParam(value = "inheritance", required = false)
                                                    String inheritance) {
        final Person person = loadPerson(personId);
        LocalDate date = parseDate(onDate);
        if (date == null) {
            date = person.getDeathDate();
        }
        TitleInheritanceStyle inheritanceStyle = TitleInheritanceStyle.HEIRS_GENERAL;
        if (!StringUtils.isEmpty(inheritance)) {
            try {
                inheritanceStyle = TitleInheritanceStyle.valueOf(inheritance);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid value for inheritance: " + e.getMessage());
            }
        }
        Pair<Person, LocalDate> heir = inheritanceService.findHeirForPerson(person, date, inheritanceStyle);
        if (heir == null) {
            return null;
        }
        return new PersonHeirResponse(heir.getFirst(), heir.getSecond(),
                ancestryService.calculateRelationship(heir.getFirst(), person, false));
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
        if (StringUtils.isEmpty(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date: " + e.getMessage());
        }
    }

}
