package com.meryt.demographics.controllers;

import java.time.LocalDate;

import com.meryt.demographics.domain.person.FirstName;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.LastName;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.NameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NamesController {

    private final NameService nameService;
    private final ControllerHelperService controllerHelperService;

    public NamesController(@Autowired NameService nameService,
                           @Autowired ControllerHelperService controllerHelperService) {
        this.nameService = nameService;
        this.controllerHelperService = controllerHelperService;
    }

    @RequestMapping("/api/names/first/random")
    public FirstName randomFirstName(@RequestParam(required = false) String gender,
                                  @RequestParam(required = false) String onDate,
                                  @RequestParam(required = false) String culture) {
        LocalDate date = controllerHelperService.parseDate(onDate);
        return nameService.randomFirstName(gender == null ? Gender.MALE : Gender.from(gender), null, date, culture);
    }

    @RequestMapping("/api/names/last/random")
    public LastName randomLastName(@RequestParam(required = false) String culture) {
        return nameService.randomLastName(culture);
    }

    @RequestMapping("/api/names/random")
    public String randomName(@RequestParam(required = false) String gender,
                             @RequestParam(required = false) String onDate,
                             @RequestParam(required = false) String culture) {
        LocalDate date = controllerHelperService.parseDate(onDate);
        return nameService.randomName(gender == null ? Gender.MALE : Gender.from(gender), date, culture);
    }

}
