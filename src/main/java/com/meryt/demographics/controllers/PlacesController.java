package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Estate;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.request.EstatePost;
import com.meryt.demographics.response.DwellingPlaceResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.HouseholdDwellingPlaceService;

@RestController
public class PlacesController {

    private final DwellingPlaceService dwellingPlaceService;
    private final ControllerHelperService controllerHelperService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;

    public PlacesController(@Autowired DwellingPlaceService dwellingPlaceService,
                            @Autowired ControllerHelperService controllerHelperService,
                            @Autowired HouseholdDwellingPlaceService householdDwellingPlaceService) {
        this.dwellingPlaceService = dwellingPlaceService;
        this.controllerHelperService = controllerHelperService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
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

    @RequestMapping(value = "/api/estates", method = RequestMethod.POST)
    public DwellingPlaceResponse postEstateForHousehold(@RequestBody EstatePost estatePost) {
        if (estatePost.getParentDwellingPlaceId() == null) {
            throw new BadRequestException("parentDwellingPlaceIdIsRequired");
        }
        DwellingPlace place = dwellingPlaceService.load(estatePost.getParentDwellingPlaceId());
        Person owner = controllerHelperService.loadPerson(estatePost.getOwnerId());
        LocalDate onDate = controllerHelperService.parseDate(estatePost.getOwnerFromDate());

        Dwelling house = (Dwelling) householdDwellingPlaceService.moveGentlemanIntoEstate(place, owner,
                owner.getHousehold(onDate), onDate);
        Estate estate = (Estate) house.getParent();
        estate.setName(estatePost.getName());
        house.setName(estatePost.getDwellingName());
        dwellingPlaceService.save(house);
        dwellingPlaceService.save(estate);
        return new DwellingPlaceResponse(estate, onDate);
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

    @RequestMapping(value = "/api/places/houses/empty", method = RequestMethod.GET)
    public List<DwellingPlaceResponse> getEmptyHouses(
            @RequestParam(value = "onDate") String onDate) {
        LocalDate date = parseDate(onDate);
        List<Dwelling> houses = new ArrayList<>();
        for (DwellingPlace parish : dwellingPlaceService.loadByType(DwellingPlaceType.PARISH)) {
            houses.addAll(parish.getEmptyHouses(date));
        }
        return houses.stream()
                .map(h -> new DwellingPlaceResponse(h, date))
                .collect(Collectors.toList());
    }

    @Nullable
    private LocalDate parseDate(@Nullable String date) {
        if (StringUtils.isEmpty(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date: " + e.getMessage());
        }
    }
}
