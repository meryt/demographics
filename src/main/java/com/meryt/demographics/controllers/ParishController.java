package com.meryt.demographics.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.generator.ParishGenerator;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.ParishParameters;
import com.meryt.demographics.response.DwellingPlaceResponse;
import com.meryt.demographics.service.AncestryService;
import com.meryt.demographics.service.CalendarService;
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
    private final AncestryService ancestryService;
    private final CalendarService calendarService;

    public ParishController(@Autowired OccupationService occupationService,
                            @Autowired DwellingPlaceService dwellingPlaceService,
                            @Autowired FamilyGenerator familyGenerator,
                            @Autowired FamilyService familyService,
                            @Autowired PersonService personService,
                            @Autowired HouseholdService householdService,
                            @Autowired AncestryService ancestryService,
                            @Autowired CalendarService calendarService) {
        this.occupationService = occupationService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
        this.personService = personService;
        this.householdService = householdService;
        this.ancestryService = ancestryService;
        this.calendarService = calendarService;
    }

    @RequestMapping("/api/parishes/random")
    public DwellingPlaceResponse randomParish(@RequestBody ParishParameters parishParameters) {
        ParishGenerator generator = new ParishGenerator(occupationService, familyGenerator, familyService,
                personService, householdService, dwellingPlaceService, ancestryService);
        Parish parish = generator.generateParish(parishParameters);
        calendarService.setCurrentDate(parishParameters.getFamilyParameters().getReferenceDate());
        if (parishParameters.isPersist()) {
            return new DwellingPlaceResponse(dwellingPlaceService.save(parish),
                    parishParameters.getFamilyParameters().getReferenceDate());
        } else {
            return new DwellingPlaceResponse(parish, parishParameters.getFamilyParameters().getReferenceDate());
        }
    }
}
