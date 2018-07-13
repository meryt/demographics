package com.meryt.demographics.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.request.GenerationPost;
import com.meryt.demographics.request.InitialGenerationPost;
import com.meryt.demographics.request.OutputToFilePost;
import com.meryt.demographics.response.FamilyResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.GenerationService;

@RestController
public class GenerationController {

    private final GenerationService generationService;
    private final ControllerHelperService controllerHelperService;

    public GenerationController(@Autowired @NonNull GenerationService generationService,
                                @Autowired @NonNull ControllerHelperService controllerHelperService) {
        this.generationService = generationService;
        this.controllerHelperService = controllerHelperService;
    }

    @RequestMapping(value = "/api/generations/initial", method = RequestMethod.POST)
    public List<FamilyResponse> postInitialGeneration(@RequestBody InitialGenerationPost initialGenerationPost) {
        try {
            initialGenerationPost.validate();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        return generationService.seedInitialGeneration(initialGenerationPost).stream()
                .map(FamilyResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Advance one generation. Takes all living male adults whose generation is not finished and attempts to create
     * families for them. If no family is created or their new spouse dies before them, they are marked as finished
     * generation.
     *
     * @param generationPost parameters used to generate the families
     * @return a response describing the new family, or an empty response if none was created
     */
    @RequestMapping(value = "/api/generations/", method = RequestMethod.POST)
    public List<FamilyResponse> postGeneration(@RequestBody GenerationPost generationPost) {

        return generationService.processGeneration(generationPost).stream()
                .map(FamilyResponse::new)
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/generations/person/{personId}", method = RequestMethod.POST)
    public List<FamilyResponse> postPersonGeneration(@RequestBody GenerationPost generationPost,
                                                     @PathVariable long personId) {
        Person person = controllerHelperService.loadPerson(personId);
        List<Person> unfinishedPersons = new ArrayList<>();
        unfinishedPersons.add(person);
        return generationService.processGeneration(unfinishedPersons, generationPost).stream()
                .map(FamilyResponse::new)
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/generations/output", method = RequestMethod.POST)
    public void regenerateOutputFile(@RequestBody OutputToFilePost outputToFilePost) {
        if (outputToFilePost.getOutputToFile() != null) {
            generationService.writeGenerationsToFile(outputToFilePost.getOutputToFile());
        }
    }
}
