package com.meryt.demographics.controllers;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.response.DwellingPlaceResponse;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.DwellingPlaceService;

@RestController
public class PlacesController {

    private final DwellingPlaceService dwellingPlaceService;

    public PlacesController(@Autowired DwellingPlaceService dwellingPlaceService) {
        this.dwellingPlaceService = dwellingPlaceService;
    }

    @RequestMapping("/api/places/{placeId}")
    public DwellingPlaceResponse getPlace(@PathVariable long placeId,
                                          @RequestParam(value = "onDate", required = false) String onDate) {
        DwellingPlace place = dwellingPlaceService.load(placeId);

        if (place == null) {
            throw new ResourceNotFoundException("No place found for ID " + placeId);
        } else {
            LocalDate date = null;
            if (onDate != null) {
                date = LocalDate.parse(onDate);
            }
            return new DwellingPlaceResponse(place, date);
        }
    }

}
