package com.meryt.demographics.controllers;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.response.HouseholdResponse;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.HouseholdService;

@Slf4j
@RestController
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(@Autowired HouseholdService householdService) {
        this.householdService = householdService;
    }

    @RequestMapping("/households/{householdId}")
    public HouseholdResponse getHousehold(@PathVariable long householdId,
                                          @RequestParam(value = "onDate", required = false) String onDate) {
        Household result = householdService.load(householdId);
        if (result == null) {
            throw new ResourceNotFoundException("No household found for ID " + householdId);
        }
        LocalDate date = null;
        if (onDate != null) {
            date = LocalDate.parse(onDate);
        }
        return new HouseholdResponse(result, date);
    }
}