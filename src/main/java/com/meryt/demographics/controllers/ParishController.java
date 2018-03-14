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
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.HouseholdService;
import com.meryt.demographics.service.OccupationService;
import com.meryt.demographics.service.PersonService;

@RestController
public class ParishController {

    private final OccupationService occupationService;
    private final DwellingPlaceService dwellingPlaceService;
    private final FamilyGenerator familyGenerator;
    private final FamilyService familyService;
    private final PersonService personService;
    private final HouseholdService householdService;

    public ParishController(@Autowired OccupationService occupationService,
                            @Autowired DwellingPlaceService dwellingPlaceService,
                            @Autowired FamilyGenerator familyGenerator,
                            @Autowired FamilyService familyService,
                            @Autowired PersonService personService,
                            @Autowired HouseholdService householdService) {
        this.occupationService = occupationService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.personService = personService;
        this.householdService = householdService;
    }

    @RequestMapping("/parishes/random")
    public Parish randomParish(@RequestBody ParishParameters parishParameters) {
        ParishGenerator generator = new ParishGenerator(occupationService, familyGenerator, familyService,
                personService, householdService, dwellingPlaceService);
        Parish parish = generator.generateParish(parishParameters);
        if (parishParameters.isPersist()) {
            return (Parish) dwellingPlaceService.save(parish);
        } else {
            return parish;
        }
    }

}
