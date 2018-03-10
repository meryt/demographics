package com.meryt.demographics.controllers;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.service.FamilyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FamilyController {

    private final FamilyGenerator familyGenerator;
    private final FamilyService familyService;

    public FamilyController(@Autowired FamilyGenerator familyGenerator, @Autowired FamilyService familyService) {
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
    }

    @RequestMapping("/families/random")
    public Family randomFamily(@RequestBody FamilyParameters familyParameters) {
        Family family = familyGenerator.generate(familyParameters == null ? new FamilyParameters() : familyParameters);
        if (familyParameters.isPersist()) {
            return familyService.save(family);
        } else {
            return family;
        }
    }


}
