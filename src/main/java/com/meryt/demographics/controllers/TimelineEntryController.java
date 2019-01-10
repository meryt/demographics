package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.story.Storyline;
import com.meryt.demographics.domain.timeline.TimelineEntry;
import com.meryt.demographics.domain.timeline.TimelineEntryCategory;
import com.meryt.demographics.request.TimelineEntryPost;
import com.meryt.demographics.response.TimelineEntryResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.PersonService;
import com.meryt.demographics.service.StorylineService;
import com.meryt.demographics.service.TimelineService;

@RestController
public class TimelineEntryController {

    private final TimelineService timelineService;
    private final PersonService personService;
    private final StorylineService storylineService;

    public TimelineEntryController(@NonNull @Autowired TimelineService timelineService,
                                   @NonNull @Autowired PersonService personService,
                                   @NonNull @Autowired StorylineService storylineService) {
        this.timelineService = timelineService;
        this.personService = personService;
        this.storylineService = storylineService;
    }

    @RequestMapping(value = "/api/timeline", method = RequestMethod.GET)
    public List<TimelineEntryResponse> getEntries(@RequestParam(required = false) List<String> category,
                                                  @RequestParam(required = false) Boolean includeStorylines) {
        List<TimelineEntry> entries;
        if (category == null || category.isEmpty()) {
            entries = timelineService.loadAll();
        } else {
            List<TimelineEntryCategory> categories = category.stream()
                    .map(TimelineEntryCategory::valueOf)
                    .collect(Collectors.toList());
            entries = timelineService.loadAllByCategories(categories);
        }

        Map<Long, TimelineEntryResponse> responses = entries.stream()
                .collect(Collectors.toMap(TimelineEntry::getId, TimelineEntryResponse::new));

        if (includeStorylines != null && includeStorylines) {
            List<Storyline> storylines = storylineService.loadAll();
            for (Storyline storyline : storylines) {
                for (TimelineEntry timelineEntry : storyline.getTimelineEntries()) {
                    if (!responses.containsKey(timelineEntry.getId())) {
                        responses.put(timelineEntry.getId(), new TimelineEntryResponse(timelineEntry));
                    }
                    responses.get(timelineEntry.getId()).addStoryline(storyline);
                }
            }
        }

        return new ArrayList<>(responses.values());
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
