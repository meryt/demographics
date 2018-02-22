package com.meryt.demographics.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NamesController {

    @RequestMapping("/names/first/random")
    public String randomFirstName() {
        return "Lemuel";
    }

    @RequestMapping("/names/last/random")
    public String randomLastName() {
        return "Fishburn";
    }

    @RequestMapping("/names/random")
    public String randomName() {
        return "Lemuel Fishburn";
    }

}
