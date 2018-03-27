package com.meryt.demographics.controllers;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.response.FamilyResponse;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.FamilyService;

@RestController
public class FamilyController {

    private final FamilyGenerator familyGenerator;
    private final FamilyService familyService;

    public FamilyController(@Autowired FamilyGenerator familyGenerator, @Autowired FamilyService familyService) {
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
    }

    @RequestMapping("/api/families/random")
    public FamilyResponse randomFamily(@NonNull @RequestBody FamilyParameters familyParameters) {
        Family family = familyGenerator.generate(familyParameters);
        if (familyParameters.isPersist()) {
            return new FamilyResponse(familyService.save(family));
        } else {
            return new FamilyResponse(family);
        }
    }

    @RequestMapping("/api/families/{familyId}")
    public FamilyResponse getFamily(@PathVariable long familyId) {
        Family result = familyService.load(familyId);
        if (result == null) {
            throw new ResourceNotFoundException("No family found for ID " + familyId);
        }
        return new FamilyResponse(result);
    }

}
