package com.meryt.demographics.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.fertility.Fertility;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.generator.person.FertilityGenerator;
import com.meryt.demographics.request.PersonFertilityPost;
import com.meryt.demographics.response.FertilityPostResponse;
import com.meryt.demographics.response.FertilityResponse;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ConflictException;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.FertilityService;
import com.meryt.demographics.service.PersonService;

@RestController
public class PersonFertilityController {

    private static final String FERTILITY_FACTOR = "fertilityFactor";
    private static final String HAVING_RELATIONS = "havingRelations";

    private final ControllerHelperService controllerHelperService;
    private final FertilityService fertilityService;
    private final PersonService personService;

    public PersonFertilityController(@Autowired ControllerHelperService controllerHelperService,
                                     @Autowired FertilityService fertilityService,
                                     @Autowired PersonService personService) {
        this.controllerHelperService = controllerHelperService;
        this.fertilityService = fertilityService;
        this.personService = personService;
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
    public FertilityPostResponse postPersonFertility(@PathVariable long personId, @RequestBody PersonFertilityPost post) {
        Person person = controllerHelperService.loadPerson(personId);

        if (person.getFertility() == null) {
            FertilityGenerator fertilityGenerator = new FertilityGenerator();
            if (person.isFemale()) {
                Maternity maternity = fertilityGenerator.randomMaternity(person);
                person.setMaternity(maternity);
            } else {
                person.setPaternity(fertilityGenerator.randomPaternity());
            }
            // This is broken in Hibernate 5.2.14-5.2.16 and possibly later. For now it is only possible to create
            // a fertility record at the time the person record is created.
            // https://hibernate.atlassian.net/browse/HHH-12436
            personService.save(person);
        }

        List<CalendarDayEvent> events = new ArrayList<>();
        String dateString = post.getCycleToDate();
        java.time.LocalDate cycleToDate = controllerHelperService.parseDate(dateString);
        if (cycleToDate != null) {
            if (!person.isFemale()) {
                throw new BadRequestException("cycleToDate applies only to women");
            }
            events.addAll(fertilityService.cycleToDate(person, cycleToDate, post.getAllowMaternalDeathOrDefault()));
        }
        return new FertilityPostResponse(person.getFertility(), events);
    }

    @RequestMapping(value = "/api/persons/{personId}/fertility", method = RequestMethod.PATCH)
    public FertilityResponse patchPersonFertility(@PathVariable long personId, @RequestBody Map<String, Object> updates) {
        Person person = controllerHelperService.loadPerson(personId);
        Fertility fertility = person.getFertility();
        
        if (fertility == null) {
            throw new ConflictException("Person does not have a fertility record. Use POST to create one first.");
        }

        // For males, only allow fertilityFactor updates
        if (person.isMale()) {
            for (String key : updates.keySet()) {
                if (!FERTILITY_FACTOR.equals(key)) {
                    throw new BadRequestException("Only fertilityFactor can be updated for male persons. Attempted to update: " + key);
                }
            }
        }

        if (updates.containsKey(FERTILITY_FACTOR)) {
            Object value = updates.get(FERTILITY_FACTOR);
            if (value == null) {
                throw new BadRequestException("fertilityFactor cannot be null");
            }
            double fertilityFactor;
            if (value instanceof Number) {
                fertilityFactor = ((Number) value).doubleValue();
            } else if (value instanceof String) {
                try {
                    fertilityFactor = Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("fertilityFactor must be a number: " + value);
                }
            } else {
                throw new BadRequestException("fertilityFactor must be a number: " + value);
            }
            fertility.setFertilityFactor(fertilityFactor);
            updates.remove(FERTILITY_FACTOR);
        }

        if (updates.containsKey(HAVING_RELATIONS)) {
            if (!(fertility instanceof Maternity)) {
                throw new ConflictException("Person does not have a maternity record");
            }
            Object value = updates.get(HAVING_RELATIONS);
            if (value == null) {
                throw new BadRequestException("havingRelations cannot be null");
            }
            boolean havingRelations;
            if (value instanceof Boolean) {
                havingRelations = (Boolean) value;
            } else if (value instanceof String) {
                havingRelations = Boolean.parseBoolean((String) value);
            } else {
                throw new BadRequestException("havingRelations must be a boolean: " + value);
            }
            ((Maternity) fertility).setHavingRelations(havingRelations);
            updates.remove(HAVING_RELATIONS);
        }

        if (!updates.isEmpty()) {
            throw new BadRequestException("No support for PATCHing key(s): " + String.join(", ", updates.keySet()));
        }

        return new FertilityResponse(personService.save(person).getFertility());
    }
}
