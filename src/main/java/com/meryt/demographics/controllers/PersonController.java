package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
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
import com.meryt.demographics.domain.family.LeastCommonAncestorRelationship;
import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.person.fertility.Fertility;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.family.MatchMaker;
import com.meryt.demographics.generator.person.FertilityGenerator;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.repository.criteria.PersonCriteria;
import com.meryt.demographics.request.PersonFamilyPost;
import com.meryt.demographics.request.PersonFertilityPost;
import com.meryt.demographics.request.PersonHouseholdPost;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.request.PersonTitlePost;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.response.FertilityResponse;
import com.meryt.demographics.response.HouseholdResponseWithLocations;
import com.meryt.demographics.response.LeastCommonAncestorResponse;
import com.meryt.demographics.response.PersonCapitalResponse;
import com.meryt.demographics.response.PersonDescendantResponse;
import com.meryt.demographics.response.PersonDetailResponse;
import com.meryt.demographics.response.PersonFamilyResponse;
import com.meryt.demographics.response.PersonHeirResponse;
import com.meryt.demographics.response.PersonPotentialSpouseResponse;
import com.meryt.demographics.response.PersonResponse;
import com.meryt.demographics.response.PersonTitleResponse;
import com.meryt.demographics.response.RelatedPersonResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.AncestryService;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.FertilityService;
import com.meryt.demographics.service.HeirService;
import com.meryt.demographics.service.HouseholdService;
import com.meryt.demographics.service.PersonService;
import com.meryt.demographics.service.TitleService;

@Slf4j
@RestController
public class PersonController {

    private static final String BIRTH_DATE = "birthDate";
    private static final String DEATH_DATE = "deathDate";
    private static final String BIRTH_PLACE = "birthPlace";
    private static final String DEATH_PLACE = "deathPlace";
    private static final String SOCIAL_CLASS = "socialClass";
    private static final String LAST_NAME = "lastName";
    private static final String IS_LAST_NAME_RECURSIVE = "applyLastNameRecursively";
    private static final String FIRST_NAME = "firstName";

    private final PersonGenerator personGenerator;

    private final PersonService personService;

    private final TitleService titleService;

    private final FamilyGenerator familyGenerator;

    private final FamilyService familyService;

    private final FertilityService fertilityService;

    private final AncestryService ancestryService;

    private final HeirService heirService;

    private final ControllerHelperService controllerHelperService;

    private final HouseholdService householdService;

    private final DwellingPlaceService dwellingPlaceService;

    public PersonController(@Autowired PersonGenerator personGenerator,
                            @Autowired PersonService personService,
                            @Autowired TitleService titleService,
                            @Autowired FamilyGenerator familyGenerator,
                            @Autowired FamilyService familyService,
                            @Autowired FertilityService fertilityService,
                            @Autowired AncestryService ancestryService,
                            @Autowired HeirService heirService,
                            @Autowired ControllerHelperService controllerHelperService,
                            @Autowired HouseholdService householdService,
                            @Autowired DwellingPlaceService dwellingPlaceService) {
        this.personGenerator = personGenerator;
        this.personService = personService;
        this.titleService = titleService;
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.fertilityService = fertilityService;
        this.ancestryService = ancestryService;
        this.heirService = heirService;
        this.controllerHelperService = controllerHelperService;
        this.householdService = householdService;
        this.dwellingPlaceService = dwellingPlaceService;
    }

    @RequestMapping("/api/persons/random")
    public PersonDetailResponse randomPerson(@RequestBody PersonParameters personParameters) {
        PersonParameters params = personParameters == null ? new PersonParameters() : personParameters;
        try {
            params.validate();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        if (params.getFatherId() != null) {
            params.setFather(controllerHelperService.loadPerson(params.getFatherId()));
        }
        if (params.getMotherId() != null) {
            params.setMother(controllerHelperService.loadPerson(params.getMotherId()));
        }

        Person person = personGenerator.generate(params);
        personService.save(person);
        if (person.getFamily() != null) {
            familyService.save(person.getFamily());
            Household mothersHousehold = person.getMother().getHousehold(person.getBirthDate());
            if (mothersHousehold != null) {
                householdService.addPersonToHousehold(person, mothersHousehold, person.getBirthDate(), false);
            }
        }

        return new PersonDetailResponse(person, null, ancestryService);
    }

    @RequestMapping(value = "/api/persons/characters", method = RequestMethod.GET)
    public List<PersonResponse> getCharacters(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = (onDate != null) ? controllerHelperService.parseDate(onDate) : null;

        return personService.findStoryCharacters(date).stream()
                .map(p -> new PersonResponse(p, date))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/persons/{personId}", method = RequestMethod.GET)
    public PersonDetailResponse getPerson(@PathVariable long personId,
                                          @RequestParam(value = "onDate", required = false) String onDate) {
        Person person = controllerHelperService.loadPerson(personId);
        LocalDate date = null;
        if (onDate != null) {
            date = controllerHelperService.parseDate(onDate);
        }
        return new PersonDetailResponse(person, date, ancestryService);
    }

    @RequestMapping(value = "/api/persons", method = RequestMethod.GET)
    public Page<PersonResponse> getPeople(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "aliveOnDate", required = false) String aliveOnDate,
            @RequestParam(value = "onDate", required = false) String onDateAsString,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "storyCharacter", required = false) Boolean storyCharacter,
            @RequestParam(value = "mainCharacter", required = false) Boolean mainCharacter,
            @RequestParam(value = "minAge", required = false) Integer minAge,
            @RequestParam(value = "maxAge", required = false) Integer maxAge,
            @RequestParam(value = "hasDwellingPlace", required = false) Boolean hasDwellingPlace) {

        PersonCriteria personCriteria = new PersonCriteria();
        personCriteria.setPage(page);
        personCriteria.setPageSize(pageSize);
        personCriteria.setSortBy(sortBy);

        personCriteria.setAliveOnDate(controllerHelperService.parseDate(aliveOnDate));
        if (gender != null) {
            personCriteria.setGender(Gender.from(gender));
        }
        if (storyCharacter != null) {
            personCriteria.setIsStoryCharacter(storyCharacter);
        }
        if (mainCharacter != null) {
            personCriteria.setIsMainCharacter(mainCharacter);
        }
        if (minAge != null) {
            personCriteria.setMinAge(minAge);
        }
        if (maxAge != null) {
            personCriteria.setMaxAge(maxAge);
        }
        if (hasDwellingPlace != null) {
            personCriteria.setHasDwellingPlace(hasDwellingPlace);
        }
        
        // Set currentDate for age calculations - use onDate if provided, otherwise use "current"
        LocalDate onDate = controllerHelperService.parseDate(onDateAsString);
        personCriteria.setCurrentDate(onDate != null ? onDate : controllerHelperService.parseDate("current"));

        LocalDate referenceDate = onDate == null ? personCriteria.getAliveOnDate() : onDate;

        try {
            Page<Person> queryResult = personService.findAll(personCriteria);
            return queryResult.map(p -> referenceDate != null
                        ? new PersonDetailResponse(p, referenceDate, ancestryService)
                        : new PersonDetailResponse(p));
        } catch (InvalidDataAccessApiUsageException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/api/persons/{personId}", method = RequestMethod.DELETE)
    public PersonDetailResponse deletePerson(@PathVariable long personId) {
        Person person = controllerHelperService.loadPerson(personId);
        PersonDetailResponse response = new PersonDetailResponse(person);
        personService.delete(person);
        return response;
    }

    @RequestMapping(value = "/api/persons/{personId}/fertility", method = RequestMethod.GET)
    public FertilityResponse getPersonFertility(@PathVariable long personId) {
        Person person = controllerHelperService.loadPerson(personId);
        Fertility fertility = person.getFertility();
        if (fertility == null) {
            return null;
        }
        return new FertilityResponse(fertility);
    }

    @RequestMapping(value = "/api/persons/{personId}/fertility", method = RequestMethod.POST)
    public Fertility postPersonFertility(@PathVariable long personId, @RequestBody PersonFertilityPost post) {
        Person person = controllerHelperService.loadPerson(personId);

        if (person.getFertility() == null) {
            FertilityGenerator fertilityGenerator = new FertilityGenerator();
            if (person.isFemale()) {
                Maternity maternity = fertilityGenerator.randomMaternity(person);
                //maternity.setPersonId(person.getId());
                //maternity.setPerson(person);
                person.setMaternity(maternity);
            } else {
                person.setPaternity(fertilityGenerator.randomPaternity());
            }
            // This is broken in Hibernate 5.2.14-5.2.16 and possibly later. For now it is only possible to create
            // a fertility record at the time the person record is created.
            // https://hibernate.atlassian.net/browse/HHH-12436
            personService.save(person);
        }

        String dateString = post.getCycleToDate();
        LocalDate cycleToDate = controllerHelperService.parseDate(dateString);
        if (cycleToDate != null) {
            if (!person.isFemale()) {
                throw new BadRequestException("cycleToDate applies only to women");
            }
            fertilityService.cycleToDate(person, cycleToDate, post.getAllowMaternalDeathOrDefault());
        }
        return person.getFertility();
    }

    @RequestMapping(value = "/api/persons/{personId}/households", method = RequestMethod.POST)
    public HouseholdResponseWithLocations addPersonToHousehold(@PathVariable long personId,
                                                               @RequestBody PersonHouseholdPost post) {
        Person person = controllerHelperService.loadPerson(personId);

        if (post.getHouseholdId() == null) {
            throw new BadRequestException("householdId is required");
        }
        LocalDate fromDate;
        String fromDateString = post.getFromDate();
        if (fromDateString == null) {
            throw new BadRequestException("fromDate is required");
        }

        if (fromDateString.equals("birth")) {
            fromDate = person.getBirthDate();
        } else {
            fromDate = controllerHelperService.parseDate("current");
        }

        Household household = householdService.load(post.getHouseholdId());
        if (household == null) {
            throw new ResourceNotFoundException("No household exists for ID " + post.getHouseholdId());
        }

        person = householdService.addPersonToHousehold(person, household, fromDate, post.isHead());
        if (post.isIncludeHomelessFamilyMembers()) {
            household = householdService.addHomelessFamilyMembersToHousehold(person, household, fromDate, true);
        }
        return new HouseholdResponseWithLocations(household, fromDate, ancestryService);
    }

    @RequestMapping(value = "/api/persons/{personId}/capital", method = RequestMethod.GET)
    public List<PersonCapitalResponse> getPersonCapital(@PathVariable long personId) {
        Person person = controllerHelperService.loadPerson(personId);
        return person.getCapitalPeriods().stream()
                .sorted(Comparator.comparing(PersonCapitalPeriod::getFromDate))
                .map(PersonCapitalResponse::new)
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/persons/{personId}/living-descendants", method = RequestMethod.GET)
    public List<RelatedPersonResponse> getPersonDescendants(@PathVariable long personId,
                                                            @RequestParam(value = "minAge", required = false)
                                                                    Integer minAge,
                                                            @RequestParam(value = "aliveOnDate")
                                                                    String aliveOnDate) {
        Person person = controllerHelperService.loadPerson(personId);
        LocalDate aliveOnLocalDate = controllerHelperService.parseDate(aliveOnDate);

        List<Person> people = personService.findDescendants(person, aliveOnLocalDate);
        return people.stream()
                .filter(p -> minAge == null || p.getAgeInYears(aliveOnLocalDate) >= minAge)
                .map(p -> new RelatedPersonResponse(p, ancestryService.calculateRelationship(p, person),
                        aliveOnLocalDate))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/persons/{personId}/descendants", method = RequestMethod.GET)
    public PersonDescendantResponse getPersonDescendants(@PathVariable long personId,
                                                         @RequestParam(value = "numGenerations", required = false)
                                                            Integer numGenerations,
                                                         @RequestParam(value = "minAge", required = false)
                                                            Integer minAge,
                                                         @RequestParam(value = "bornBefore", required = false)
                                                            String bornBefore,
                                                         @RequestParam(value = "onDate", required = false)
                                                            String onDate) {
        Person person = controllerHelperService.loadPerson(personId);
        LocalDate bornBeforeDate = controllerHelperService.parseDate(bornBefore);
        LocalDate referenceDate = controllerHelperService.parseDate(onDate);

        return new PersonDescendantResponse(person, referenceDate, minAge, bornBeforeDate, 0,
                numGenerations == null ? 3 : numGenerations);
    }

    @RequestMapping(value = "/api/persons/{personId}/relatives", method = RequestMethod.GET)
    public List<RelatedPersonResponse> getPersonRelatives(@PathVariable long personId,
                                                          @RequestParam(value = "aliveOnDate", required = false)
                                                                String aliveOnDate,
                                                          @RequestParam(value = "maxDistance", required = false)
                                                                Long maxDistance,
                                                          @RequestParam(value = "maxAge", required = false)
                                                                Integer maxAge) {
        Person person = controllerHelperService.loadPerson(personId);
        LocalDate aliveOn = controllerHelperService.parseDate(aliveOnDate);

        List<Person> relatedPersons;
        if (aliveOn != null) {
            relatedPersons = personService.findLivingRelatives(person, aliveOn, maxDistance);
        } else {
            relatedPersons = personService.findRelatives(person, maxDistance);
        }

        return relatedPersons.stream()
                .filter(p -> (maxAge == null || aliveOn == null) || p.getAgeInYears(aliveOn) <= maxAge)
                .map(rp -> new RelatedPersonResponse(rp, ancestryService.calculateRelationship(rp, person, true)))
                .sorted(Comparator.comparing(RelatedPersonResponse::getDegreeOfSeparation)
                        .thenComparing(RelatedPersonResponse::getBirthDate))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "api/persons/{personId}/least-common-ancestor/{otherPersonId}", method = RequestMethod.GET)
    public LeastCommonAncestorResponse getPersonLeastCommonAncestorWith(@PathVariable long personId,
                                                                        @PathVariable long otherPersonId) {
        Person person = controllerHelperService.loadPerson(personId);
        Person otherPerson = controllerHelperService.loadPerson(otherPersonId);

        LeastCommonAncestorRelationship lcaRelationship = ancestryService.getLeastCommonAncestor(person, otherPerson);

        if (lcaRelationship == null) {
            return null;
        }
        Relationship relationship = ancestryService.calculateRelationship(otherPerson, person, false);
        if (relationship == null) {
            return null;
        }

        return new LeastCommonAncestorResponse(person, otherPerson, lcaRelationship, relationship, personService);
    }


    @RequestMapping("/api/persons/{personId}/titles")
    public List<PersonTitleResponse> getPersonTitles(@PathVariable long personId,
                                                     @RequestParam(value = "onDate", required = false) String onDate) {
        Person person = controllerHelperService.loadPerson(personId);
        LocalDate date = null;
        if (onDate != null) {
            date = controllerHelperService.parseDate(onDate);
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
        Person person = controllerHelperService.loadPerson(personId);

        final Title title = titleService.load(personTitlePost.getTitleId());
        if (title == null) {
            throw new ResourceNotFoundException("No title found for ID " + personTitlePost.getTitleId());
        }

        person.addOrUpdateTitle(title, personTitlePost.getFromDate(), personTitlePost.getToDate());

        personService.save(person);

        for (DwellingPlace place : title.getEntailedProperties()) {
            dwellingPlaceService.transferDwellingPlaceToPerson(place, person, personTitlePost.getFromDate(), false,
                    DwellingPlaceOwnerPeriod.Reason.inheritedAsTitleHolderMessage(title));
        }

        return getPersonTitles(personId, null);
    }

    @RequestMapping("/api/persons/{personId}/families")
    public List<PersonFamilyResponse> getPersonFamilies(@PathVariable long personId) {
        Person person = controllerHelperService.loadPerson(personId);
        List<PersonFamilyResponse> families = new ArrayList<>();
        for (Family family : person.getFamilies()) {
            families.add(new PersonFamilyResponse(person, family, null));
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
        Person person = controllerHelperService.loadPerson(personId);

        RandomFamilyParameters familyParameters = new RandomFamilyParameters();
        familyParameters.setMinHusbandAge(personFamilyPost.getMinHusbandAge());
        familyParameters.setMinWifeAge(personFamilyPost.getMinWifeAge());
        familyParameters.setReferenceDate(personFamilyPost.getUntilDate() == null
                ? person.getDeathDate()
                : controllerHelperService.parseDate(personFamilyPost.getUntilDate()));
        familyParameters.setPersist(personFamilyPost.isPersist());
        familyParameters.setSpouseLastName(personFamilyPost.getSpouseLastName());
        if (personFamilyPost.getSpouseId() != null) {
            familyParameters.setSpouse(controllerHelperService.loadPerson(personFamilyPost.getSpouseId()));
        }
        familyParameters.setAllowExistingSpouse(personFamilyPost.isAllowExistingSpouse());
        familyParameters.setMinSpouseSelection(personFamilyPost.getMinSpouseSelection());
        familyParameters.setTriesUntilGiveUp(personFamilyPost.getTriesUntilGiveUp());
        familyParameters.setSkipGenerateChildren(personFamilyPost.getSkipGenerateChildren());

        Family family = familyGenerator.generate(person, familyParameters);
        if (family == null) {
            return new ResponseEntity<>((PersonFamilyResponse) null, HttpStatus.NO_CONTENT);
        } else {
            if (personFamilyPost.isPersist()) {
                family = familyService.save(family);
            }

            Relationship relationship = ancestryService.calculateRelationship(person, person.isMale()
                    ? family.getWife() : family.getHusband(), true);

            return new ResponseEntity<>(new PersonFamilyResponse(person, family, relationship,
                    familyParameters.getReferenceDate()),
                    personFamilyPost.isPersist()
                            ? HttpStatus.CREATED
                            : HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/api/persons/{personId}", method = RequestMethod.PATCH)
    public PersonDetailResponse patchPerson(@PathVariable long personId, @RequestBody Map<String, Object> updates) {
        Person person = controllerHelperService.loadPerson(personId);
        if (updates.containsKey(SOCIAL_CLASS)) {
            if (updates.get(SOCIAL_CLASS) == null) {
                person.setSocialClass(null);
            } else {
                person.setSocialClass(SocialClass.fromEnumName((String) updates.get(SOCIAL_CLASS)));
            }
            updates.remove(SOCIAL_CLASS);
        }
        if (updates.containsKey(LAST_NAME)) {
            boolean recurse = false;
            if (updates.containsKey(IS_LAST_NAME_RECURSIVE)) {
                recurse = ((boolean)(updates.get(IS_LAST_NAME_RECURSIVE)));
                updates.remove(IS_LAST_NAME_RECURSIVE);
            }
            String newLastName = ((String) updates.get(LAST_NAME));
            updates.remove(LAST_NAME);

            // This method saves the person right away
            personService.updatePersonLastName(person, newLastName, recurse, false);
        }

        if (updates.containsKey(FIRST_NAME)) {
            person.setFirstName((String) updates.get(FIRST_NAME));
            updates.remove(FIRST_NAME);
        }

        if (updates.containsKey(BIRTH_PLACE)) {
            person.setBirthPlace((String) updates.get(BIRTH_PLACE));
            updates.remove(BIRTH_PLACE);
        }

        if (updates.containsKey(DEATH_PLACE)) {
            person.setDeathPlace((String) updates.get(DEATH_PLACE));
            updates.remove(DEATH_PLACE);
        }

        if (updates.containsKey(DEATH_DATE)) {
            LocalDate deathDate = controllerHelperService.parseDate((String) updates.get(DEATH_DATE));
            if (deathDate != null) {
                if (deathDate.isBefore(person.getBirthDate())) {
                    throw new BadRequestException(String.format("%s of %s is before current birth date of %s",
                            DEATH_DATE, deathDate, person.getBirthDate()));
                }
                person.setDeathDate(deathDate);
            }
            updates.remove(DEATH_DATE);
        }

        if (updates.containsKey(BIRTH_DATE)) {
            LocalDate birthDate = controllerHelperService.parseDate((String) updates.get(BIRTH_DATE));
            if (birthDate != null) {
                if (birthDate.isAfter(person.getDeathDate())) {
                    throw new BadRequestException(String.format("%s of %s is after current death date of %s",
                            BIRTH_DATE, birthDate, person.getDeathDate()));
                }
                person.setBirthDate(birthDate);
            }
            updates.remove(BIRTH_DATE);
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
                                                           @RequestParam(value = "minHusbandAge", required = true)
                                                                       Integer minHusbandAge,
                                                           @RequestParam(value = "minWifeAge", required = true)
                                                                        Integer minWifeAge,
                                                           @RequestParam(value = "maxWifeAge", required = false)
                                                                         Integer maxWifeAge,
                                                           @RequestParam(value = "minSocialClass", required = false)
                                                                         String minSocialClass,
                                                           @RequestParam(value = "maxSocialClass", required = false)
                                                                         String maxSocialClass) {
        final Person person = controllerHelperService.loadPerson(personId);
        LocalDate date = controllerHelperService.parseDate(onDate);
        LocalDate searchDate = MatchMaker.getDateToStartMarriageSearch(person, minHusbandAge, minWifeAge);
        if (date != null && date.isAfter(searchDate)) {
            searchDate = date;
        }

        final LocalDate finalSearchDate = searchDate;

        RandomFamilyParameters familyParameters = new RandomFamilyParameters();
        familyParameters.setMinWifeAge(minWifeAge);
        familyParameters.setMinHusbandAge(minHusbandAge);
        familyParameters.setMaxWifeAge(maxWifeAge);

        if (minSocialClass != null && !StringUtils.hasText(minSocialClass)) {
            SocialClass minClass = SocialClass.valueOf(minSocialClass);
            familyParameters.setMinSocialClass(minClass);
        }
        if (maxSocialClass != null && !StringUtils.hasText(maxSocialClass)) {
            SocialClass maxClass = SocialClass.valueOf(maxSocialClass);
            familyParameters.setMaxSocialClass(maxClass);
        }

        // If a specific date is given, include only people eligible on that day. Otherwise include people eligible
        // in the future.
        boolean includeFuture = onDate == null;
        return personService.findPotentialSpouseWithRelationship(person, searchDate, includeFuture, familyParameters)
                .stream()
                .map(pr -> new PersonPotentialSpouseResponse(person, pr.getPerson(), finalSearchDate,
                        pr.getRelationship()))
                .collect(Collectors.toList());
    }

    /**
     * Gets potential heirs for person, assuming that if females may inherit, then all females will inherit equally
     *
     * @param personId the person for whom to find heirs
     * @param onDate the date to use, if specified, or the person's death date if null
     * @param inheritance the inheritance style, if specified, or HEIRS_GENERAL if null
     * @return a list of 0 or more people who may inherit. There may be multiples if females may inherit but split
     * the inheritance rights equally
     */
    @RequestMapping(value = "/api/persons/{personId}/heirs", method = RequestMethod.GET)
    public List<RelatedPersonResponse> getPersonPotentialHeirs(@PathVariable long personId,
                                                               @RequestParam(value = "onDate", required = false)
                                                                  String onDate,
                                                               @RequestParam(value = "inheritance", required = false)
                                                                  String inheritance) {
        final Person person = controllerHelperService.loadPerson(personId);
        LocalDate date = controllerHelperService.parseDate(onDate);
        if (date == null) {
            date = person.getDeathDate();
        }
        TitleInheritanceStyle inheritanceStyle = TitleInheritanceStyle.HEIRS_GENERAL;
        if (!StringUtils.hasText(inheritance)) {
            try {
                inheritanceStyle = TitleInheritanceStyle.valueOf(inheritance);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid value for inheritance: " + e.getMessage());
            }
        }
        List<Person> heirs = heirService.findPotentialHeirsForPerson(person, date, inheritanceStyle, true, false);
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
        final Person person = controllerHelperService.loadPerson(personId);
        LocalDate date = controllerHelperService.parseDate(onDate);
        if (date == null) {
            date = person.getDeathDate();
        }
        TitleInheritanceStyle inheritanceStyle = TitleInheritanceStyle.HEIRS_GENERAL;
        if (!StringUtils.hasText(inheritance)) {
            try {
                inheritanceStyle = TitleInheritanceStyle.valueOf(inheritance);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid value for inheritance: " + e.getMessage());
            }
        }
        Pair<Person, LocalDate> heir = heirService.findHeirForPerson(person, date, inheritanceStyle, true,
                false);
        if (heir == null) {
            return null;
        }
        return new PersonHeirResponse(heir.getFirst(), heir.getSecond(),
                ancestryService.calculateRelationship(heir.getFirst(), person, false));
    }

}
