package com.meryt.demographics.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.service.ConfigurationService;

@RestController
public class ConfigurationController {

    private static final String LAST_CHECK_DATE_KEY = "last_check_date";

    private final ConfigurationService configurationService;

    public ConfigurationController(@Autowired ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @RequestMapping(value = "/api/configuration", method = RequestMethod.GET)
    public Map<String, String> getConfiguration() {
        return configurationService.getAllConfiguration();
    }

    @RequestMapping(value = "/api/configuration", method = RequestMethod.PATCH)
    public ResponseEntity<Map<String, String>> patchConfiguration(@RequestBody Map<String, Object> updates) {
        if (updates != null && !updates.isEmpty()) {
            if (updates.containsKey(LAST_CHECK_DATE_KEY)) {
                throw new BadRequestException("Editing '" + LAST_CHECK_DATE_KEY + "' via PATCH is not allowed");
            }
            Map<String, String> stringUpdates = new HashMap<>();
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                Object v = entry.getValue();
                stringUpdates.put(entry.getKey(), v == null ? null : v.toString());
            }
            configurationService.upsertConfiguration(stringUpdates);
        }
        return ResponseEntity.ok(configurationService.getAllConfiguration());
    }
}