package com.meryt.demographics.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.service.ConfigurationService;

@RestController
public class ConfigurationController {

    private final ConfigurationService configurationService;

    public ConfigurationController(@Autowired ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @RequestMapping(value = "/api/configuration", method = RequestMethod.GET)
    public Map<String, String> getConfiguration() {
        return configurationService.getAllConfiguration();
    }
}