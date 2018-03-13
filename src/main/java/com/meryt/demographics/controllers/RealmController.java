package com.meryt.demographics.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.place.Realm;
import com.meryt.demographics.generator.ParishGenerator;
import com.meryt.demographics.generator.RealmGenerator;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.RealmParameters;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.OccupationService;

@RestController
public class RealmController {

    private final OccupationService occupationService;
    private final DwellingPlaceService dwellingPlaceService;
    private final FamilyGenerator familyGenerator;

    public RealmController(@Autowired OccupationService occupationService,
                           @Autowired DwellingPlaceService dwellingPlaceService,
                           @Autowired FamilyGenerator familyGenerator) {
        this.occupationService = occupationService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.familyGenerator = familyGenerator;
    }

    @RequestMapping("/realms/random")
    public Realm randomRealm(@RequestBody RealmParameters realmParameters) {
        RealmGenerator generator = new RealmGenerator(occupationService);
        return generator.generate(realmParameters);
    }

    @RequestMapping("/parishes/random")
    public Parish randomParish(@RequestBody RealmParameters realmParameters) {
        ParishGenerator generator = new ParishGenerator(occupationService, familyGenerator);
        Parish parish = generator.generateParish(realmParameters);
        if (realmParameters.isPersist()) {
            return (Parish) dwellingPlaceService.save(parish);
        } else {
            return parish;
        }
    }

}
