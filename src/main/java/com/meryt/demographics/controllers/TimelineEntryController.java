package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.timeline.TimelineEntry;
import com.meryt.demographics.domain.timeline.TimelineEntryCategory;
import com.meryt.demographics.request.TimelineEntryPost;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.PersonService;
import com.meryt.demographics.service.TimelineService;

@RestController
public class TimelineEntryController {

    private final TimelineService timelineService;
    private final PersonService personService;

    public TimelineEntryController(@NonNull @Autowired TimelineService timelineService,
                                   @NonNull @Autowired PersonService personService) {
        this.timelineService = timelineService;
        this.personService = personService;
    }

    @RequestMapping(value = "/api/timeline", method = RequestMethod.GET)
    public List<TimelineEntry> getEntries(@RequestParam(required = false) List<String> category) {
        if (category == null || category.isEmpty()) {
            return timelineService.loadAll();
        } else {
            List<TimelineEntryCategory> categories = category.stream()
                    .map(TimelineEntryCategory::valueOf)
                    .collect(Collectors.toList());
            return timelineService.loadAllByCategories(categories);
        }
    }

    @RequestMapping(value = "/api/timeline", method = RequestMethod.POST)
    public TimelineEntry createEntry(@RequestBody TimelineEntryPost timelineEntry) {
        timelineEntry.validate();
        TimelineEntry entry = timelineEntry.toTimelineEntry();

        List<Person> personsWithEntry = new ArrayList<>();
        if (timelineEntry.getPersonIds() != null && !timelineEntry.getPersonIds().isEmpty()) {
            for (Long id : timelineEntry.getPersonIds()) {
                Person person = personService.load(id);
                if (person == null) {
                    throw new ResourceNotFoundException("No person found for id " + id);
                }
                if (person.getDeathDate().isBefore(entry.getFromDate())) {
                    throw new BadRequestException(String.format("%s was already dead before %s",
                            person.getIdAndName(), entry.getFromDate()));
                }
                LocalDate endDate = entry.getToDate() == null ? entry.getFromDate() : entry.getToDate();
                if (person.getBirthDate().isAfter(endDate)) {
                    throw new BadRequestException(String.format("%s was born after %s",
                            person.getIdAndName(), endDate));
                }
                 personsWithEntry.add(person);
            }
        }

        entry = timelineService.save(entry);
        for (Person person : personsWithEntry) {
            person.getTimelineEntries().add(entry);
            personService.save(person);
        }
        return entry;
    }

}
