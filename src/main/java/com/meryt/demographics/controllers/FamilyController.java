package com.meryt.demographics.controllers;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.service.FamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(@Autowired FamilyService familyService) {
        this.familyService = familyService;
    }

    @RequestMapping("/families/random")
    public Family randomFamily(@RequestBody FamilyParameters familyParameters) {
        return familyService.generateFamily(familyParameters == null ? new FamilyParameters() : familyParameters);
    }


}
