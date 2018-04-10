package com.meryt.demographics.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.response.TitleReference;
import com.meryt.demographics.response.TitleResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.PersonService;
import com.meryt.demographics.service.TitleService;

@RestController
public class TitleController {

    private static final String INHERITANCE_ROOT = "inheritanceRoot";

    private final TitleService titleService;
    private final PersonService personService;

    public TitleController(@Autowired @NonNull TitleService titleService,
                           @Autowired @NonNull PersonService personService) {
        this.titleService = titleService;
        this.personService = personService;
    }

    @RequestMapping("/api/titles")
    public List<TitleReference> getTitles() {
        Iterable<Title> titles = titleService.findAll();
        return StreamSupport.stream(titles.spliterator(), false)
                .map(TitleReference::new)
                .collect(Collectors.toList());
    }

    @RequestMapping("/api/titles/{titleId}")
    public TitleResponse getTitle(@PathVariable long titleId) {
        Title title = loadTitle(titleId);
        return new TitleResponse(title);
    }

    @RequestMapping(value = "/api/titles/{titleId}", method = RequestMethod.PATCH)
    public TitleResponse patchTitle(@PathVariable long titleId, @RequestBody Map<String, Object> updates) {
        Title title = loadTitle(titleId);
        if (updates.containsKey(INHERITANCE_ROOT)) {
            if (updates.get(INHERITANCE_ROOT) == null) {
                title.setInheritanceRoot(null);
            } else {
                Integer personId = (Integer) updates.get(INHERITANCE_ROOT);
                Person root = personService.load(personId);
                if (root == null) {
                    throw new ResourceNotFoundException("Unable to set " + INHERITANCE_ROOT +
                            ": no person exists for id " + personId);
                }
                title.setInheritanceRoot(root);
            }
            updates.remove(INHERITANCE_ROOT);
        }

        if (!updates.isEmpty()) {
            throw new BadRequestException("No support for PATCHing key(s): " + String.join(", ", updates.keySet()));
        }

        return new TitleResponse(titleService.save(title));
    }

    @NonNull
    private Title loadTitle(Long titleId) {
        if (titleId == null) {
            throw new BadRequestException("title ID may not be null");
        }

        Title title = titleService.load(titleId);
        if (title == null) {
            throw new ResourceNotFoundException("No title found for ID " + titleId);
        }
        return title;
    }


}
