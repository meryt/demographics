package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
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

    @RequestMapping(value = "/api/places", method = RequestMethod.POST)
    public DwellingPlaceResponse postPlace(@RequestBody DwellingPlace place) {
        return new DwellingPlaceResponse(dwellingPlaceService.save(place), null);
    }

    @RequestMapping("/api/places/estates")
    public List<DwellingPlaceResponse> getEstates(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = (onDate != null) ? LocalDate.parse(onDate) : null;

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.ESTATE);
        return estates.stream().map(e -> new DwellingPlaceResponse(e, date)).collect(Collectors.toList());
    }

    @RequestMapping("/api/places/parishes")
    public List<DwellingPlaceResponse> getParishes(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = (onDate != null) ? LocalDate.parse(onDate) : null;

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.PARISH);
        return estates.stream().map(e -> new DwellingPlaceResponse(e, date)).collect(Collectors.toList());
    }

    @RequestMapping("/api/places/towns")
    public List<DwellingPlaceResponse> getTowns(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = (onDate != null) ? LocalDate.parse(onDate) : null;

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.TOWN);
        return estates.stream().map(e -> new DwellingPlaceResponse(e, date)).collect(Collectors.toList());
    }

    @RequestMapping("/api/places/farms")
    public List<DwellingPlaceResponse> getFarms(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = (onDate != null) ? LocalDate.parse(onDate) : null;

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.FARM);
        return estates.stream().map(e -> new DwellingPlaceResponse(e, date)).collect(Collectors.toList());
    }

}
