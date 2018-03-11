package com.meryt.demographics.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.place.Realm;
import com.meryt.demographics.generator.RealmGenerator;
import com.meryt.demographics.request.RealmParameters;
import com.meryt.demographics.service.OccupationService;

@RestController
public class RealmController {

    private final OccupationService occupationService;

    public RealmController(@Autowired OccupationService occupationService) {
        this.occupationService = occupationService;
    }

    @RequestMapping("/realms/random")
    public Realm randomRealm(@RequestBody RealmParameters realmParameters) {
        RealmGenerator generator = new RealmGenerator(occupationService);
        return generator.generate(realmParameters);
    }

    @RequestMapping("/parishes/random")
    public Parish randomParish(@RequestBody RealmParameters realmParameters) {
        RealmGenerator generator = new RealmGenerator(occupationService);
        return generator.generateParish(realmParameters);
    }

}
