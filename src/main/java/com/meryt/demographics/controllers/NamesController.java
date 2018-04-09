package com.meryt.demographics.controllers;

import java.time.LocalDate;

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

    @RequestMapping("/api/names/first/random")
    public String randomFirstName(@RequestParam(required = false) String gender,
                                  @RequestParam(required = false) String onDate) {
        LocalDate date = null;
        if (onDate != null) {
            date = LocalDate.parse(onDate);
        }
        return nameService.randomFirstName(gender == null ? Gender.MALE : Gender.from(gender), null, date);
    }

    @RequestMapping("/api/names/last/random")
    public String randomLastName() {
        return nameService.randomLastName();
    }

    @RequestMapping("/api/names/random")
    public String randomName(@RequestParam(required = false) String gender) {
        return nameService.randomName(gender == null ? Gender.MALE : Gender.from(gender));
    }

}
