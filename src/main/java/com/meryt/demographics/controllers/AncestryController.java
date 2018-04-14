package com.meryt.demographics.controllers;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.service.AncestryService;

@RestController
public class AncestryController {

    private final AncestryService ancestryService;

    public AncestryController(@Autowired @NonNull AncestryService ancestryService) {
        this.ancestryService = ancestryService;
    }

    @RequestMapping(value = "/api/ancestry/", method = RequestMethod.POST)
    public void updateAncestryTable() {
        ancestryService.updateAncestryTable();
    }

}
