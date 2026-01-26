package com.meryt.demographics.controllers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.response.SocialClassResponse;

@RestController
public class SocialClassController {

    @RequestMapping(value = "/api/social-classes", method = RequestMethod.GET)
    public List<SocialClassResponse> getSocialClasses() {
        return Arrays.stream(SocialClass.values())
                .sorted(Comparator.comparingInt(SocialClass::getRank))
                .map(SocialClassResponse::new)
                .collect(Collectors.toList());
    }
}
