package com.meryt.demographics.controllers;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.service.NameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NamesController {

    private final NameService nameService;

    public NamesController(@Autowired NameService nameService) {
        this.nameService = nameService;
    }

    @RequestMapping("/names/first/random")
    public String randomFirstName(@RequestParam(required = false) String gender) {
        return nameService.randomFirstName(gender == null ? Gender.MALE : Gender.from(gender));
    }

    @RequestMapping("/names/last/random")
    public String randomLastName() {
        return nameService.randomLastName();
    }

    @RequestMapping("/names/random")
    public String randomName(@RequestParam(required = false) String gender) {
        return nameService.randomName(gender == null ? Gender.MALE : Gender.from(gender));
    }

}
