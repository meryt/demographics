package com.meryt.demographics.controllers;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.FamilyParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FamilyController {

    private final FamilyGenerator familyGenerator;

    public FamilyController(@Autowired FamilyGenerator familyGenerator) {
        this.familyGenerator = familyGenerator;
    }

    @RequestMapping("/families/random")
    public Family randomFamily(@RequestBody FamilyParameters familyParameters) {
        return familyGenerator.generate(familyParameters == null ? new FamilyParameters() : familyParameters);
    }


}
