package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;
import com.meryt.demographics.request.RandomTitleParameters;
import com.meryt.demographics.request.TitlePost;
import com.meryt.demographics.response.RelatedPersonResponse;
import com.meryt.demographics.response.TitleReference;
import com.meryt.demographics.response.TitleResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.AncestryService;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.PersonService;
import com.meryt.demographics.service.TitleService;

@RestController
public class TitleController {

    private static final String INHERITANCE_ROOT = "inheritanceRoot";
    private static final String INHERITANCE_STYLE = "inheritanceStyle";
    private static final String EXTINCT = "extinct";

    private final TitleService titleService;
    private final PersonService personService;
    private final AncestryService ancestryService;
    private final ControllerHelperService controllerHelperService;

    public TitleController(@Autowired @NonNull TitleService titleService,
                           @Autowired @NonNull PersonService personService,
                           @Autowired @NonNull AncestryService ancestryService,
                           @Autowired @NonNull ControllerHelperService controllerHelperService) {
        this.titleService = titleService;
        this.personService = personService;
        this.ancestryService = ancestryService;
        this.controllerHelperService = controllerHelperService;
    }

    @RequestMapping("/api/titles")
    public List<TitleReference> getTitles(@RequestParam(value = "extinct", required = false) String isExtinct,
                                          @RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);
        Boolean extinct = null;
        if (StringUtils.hasText(isExtinct)) {
            try {
                extinct = Boolean.parseBoolean(isExtinct);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unable to parse boolean from 'extinct' value of '" + isExtinct + "'");
            }
        }
        final Boolean filterExtinct = extinct;
        Iterable<Title> titles = titleService.findAllOrderByName();
        
        return StreamSupport.stream(titles.spliterator(), false)
                .filter(t -> filterExtinct == null || filterExtinct.equals(t.isExtinct()))
                .sorted(
                    Comparator.comparing((Title t) -> t.getSocialClass().getRank()).reversed()
                        .thenComparing(t -> t.getName().toLowerCase())
                )
                .map(t -> new TitleReference(t, date))
                .collect(Collectors.toList());
    }

    @RequestMapping("/api/titles/{titleId}")
    public TitleResponse getTitle(@PathVariable long titleId) {
        Title title = loadTitle(titleId);
        List<RelatedPersonResponse> heirs = getTitleHeirs(title);
        return new TitleResponse(title, ancestryService, heirs);
    }

    @RequestMapping(value = "/api/titles", method = RequestMethod.POST)
    public TitleResponse createTitle(@RequestBody TitlePost titlePost) {
        Title title = titlePost.toTitle(personService);
        return new TitleResponse(titleService.save(title), ancestryService, null);
    }

    @RequestMapping(value = "/api/random-title", method = RequestMethod.POST)
    public TitleResponse createTitle(@RequestBody RandomTitleParameters titlePost) {
        Title newTitle = titleService.createRandomTitle(titlePost, controllerHelperService.parseDate("current"));
        return new TitleResponse(newTitle, ancestryService, null);
    }

    @RequestMapping(value = "/api/titles/{titleId}", method = RequestMethod.PATCH)
    public TitleReference patchTitle(@PathVariable long titleId, @RequestBody Map<String, Object> updates) {
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
        if (updates.containsKey(INHERITANCE_STYLE)) {
            if (updates.get(INHERITANCE_STYLE) == null) {
                title.setInheritance(null);
            } else {
                title.setInheritance(TitleInheritanceStyle.valueOf((String) updates.get(INHERITANCE_STYLE)));
            }
            updates.remove(INHERITANCE_STYLE);
        }

        if (updates.containsKey(EXTINCT)) {
            if (updates.get(EXTINCT) != null) {
                title.setExtinct((boolean) updates.get(EXTINCT));
            }
            updates.remove(EXTINCT);
        }

        if (!updates.isEmpty()) {
            throw new BadRequestException("No support for PATCHing key(s): " + String.join(", ", updates.keySet()));
        }

        return new TitleReference(titleService.save(title));
    }

    @RequestMapping(value = "api/titles/{titleId}/heirs", method = RequestMethod.GET)
    public List<RelatedPersonResponse> getTitleHeirs(@PathVariable long titleId) {
        Title title = loadTitle(titleId);
        return getTitleHeirs(title);
    }

    private List<RelatedPersonResponse> getTitleHeirs(@NonNull Title title) {
        Pair<LocalDate, List<Person>> heirs = titleService.getTitleHeirs(title);
        if (heirs == null || heirs.getSecond().isEmpty()) {
            return new ArrayList<>();
        }
        Person currentHolder = titleService.getLatestHolder(title);
        return heirs.getSecond().stream()
                .map(p -> new RelatedPersonResponse(p, ancestryService.calculateRelationship(p, currentHolder, false)))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "api/titles/{titleId}/heirs", method = RequestMethod.POST)
    public TitleResponse postTitleHeirs(@PathVariable long titleId) {
        Title title = loadTitle(titleId);
        titleService.checkForSingleTitleHeir(title, controllerHelperService.parseDate("current"), null);
        return new TitleResponse(title, ancestryService, getTitleHeirs(title));
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
