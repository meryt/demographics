package com.meryt.demographics.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @RequestMapping("/places/{placeId}")
    public DwellingPlaceResponse getPlace(@PathVariable long placeId) {
        DwellingPlace place = dwellingPlaceService.load(placeId);
        if (place == null) {
            throw new ResourceNotFoundException("No place found for ID " + placeId);
        } else {
            return new DwellingPlaceResponse(place);
        }
    }

}
