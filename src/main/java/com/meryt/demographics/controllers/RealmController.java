package com.meryt.demographics.controllers;

import com.meryt.demographics.domain.place.Realm;
import com.meryt.demographics.generator.RealmGenerator;
import com.meryt.demographics.request.RealmParameters;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RealmController {

    @RequestMapping("/realm/random")
    public Realm randomRealm(@RequestBody RealmParameters realmParameters) {
        RealmGenerator generator = new RealmGenerator();
        return generator.generate(realmParameters);
    }

}
