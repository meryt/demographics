package com.meryt.demographics.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.generator.ParishGenerator;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.ParishParameters;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.OccupationService;

@RestController
public class ParishController {

    private final OccupationService occupationService;
    private final DwellingPlaceService dwellingPlaceService;
    private final FamilyGenerator familyGenerator;

    public ParishController(@Autowired OccupationService occupationService,
                            @Autowired DwellingPlaceService dwellingPlaceService,
                            @Autowired FamilyGenerator familyGenerator) {
        this.occupationService = occupationService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.familyGenerator = familyGenerator;
    }

    @RequestMapping("/parishes/random")
    public Parish randomParish(@RequestBody ParishParameters parishParameters) {
        ParishGenerator generator = new ParishGenerator(occupationService, familyGenerator);
        Parish parish = generator.generateParish(parishParameters);
        if (parishParameters.isPersist()) {
            return (Parish) dwellingPlaceService.save(parish);
        } else {
            return parish;
        }
    }

}
