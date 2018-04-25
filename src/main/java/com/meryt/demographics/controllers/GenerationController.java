package com.meryt.demographics.controllers;

import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.request.GenerationPost;
import com.meryt.demographics.request.InitialGenerationPost;
import com.meryt.demographics.response.FamilyResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.service.GenerationService;

@RestController
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(@Autowired @NonNull GenerationService generationService) {
        this.generationService = generationService;
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


}
