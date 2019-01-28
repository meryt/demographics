package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdInhabitantPeriod;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.request.EstatePost;
import com.meryt.demographics.response.DwellingPlaceDetailResponse;
import com.meryt.demographics.response.DwellingPlaceOwnerResponse;
import com.meryt.demographics.response.DwellingPlaceReference;
import com.meryt.demographics.response.DwellingPlaceResponse;
import com.meryt.demographics.response.PersonReference;
import com.meryt.demographics.response.PersonResidencePeriodResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.AncestryService;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.HouseholdDwellingPlaceService;
import com.meryt.demographics.service.TitleService;
import com.meryt.demographics.time.DateRange;
import com.meryt.demographics.time.LocalDateComparator;

@Slf4j
@RestController
public class PlacesController {

    private final DwellingPlaceService dwellingPlaceService;
    private final ControllerHelperService controllerHelperService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;
    private final AncestryService ancestryService;
    private final TitleService titleService;

    public PlacesController(@Autowired DwellingPlaceService dwellingPlaceService,
                            @Autowired ControllerHelperService controllerHelperService,
                            @Autowired HouseholdDwellingPlaceService householdDwellingPlaceService,
                            @Autowired AncestryService ancestryService,
                            @Autowired TitleService titleService) {
        this.dwellingPlaceService = dwellingPlaceService;
        this.controllerHelperService = controllerHelperService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.ancestryService = ancestryService;
        this.titleService = titleService;
    }

    @RequestMapping("/api/places/{placeId}")
    public DwellingPlaceDetailResponse getPlace(@PathVariable long placeId,
                                                @RequestParam(value = "onDate", required = false) String onDate) {
        DwellingPlace place = dwellingPlaceService.load(placeId);

        if (place == null) {
            throw new ResourceNotFoundException("No place found for ID " + placeId);
        } else {
            LocalDate date = controllerHelperService.parseDate(onDate);
            return new DwellingPlaceDetailResponse(place, date, ancestryService);
        }
    }

    @RequestMapping("/api/places/{placeId}/owners")
    public List<DwellingPlaceOwnerResponse> getPlaceOwners(@PathVariable long placeId) {
        DwellingPlace place = dwellingPlaceService.load(placeId);

        if (place == null) {
            throw new ResourceNotFoundException("No place found for ID " + placeId);
        }

        return place.getOwnerPeriods().stream()
                .sorted(Comparator.comparing(DwellingPlaceOwnerPeriod::getFromDate)
                        .thenComparing(DwellingPlaceOwnerPeriod::getPersonId))
                .map(DwellingPlaceOwnerResponse::new)
                .collect(Collectors.toList());
    }

    @RequestMapping("/api/places/{placeId}/residents")
    @SuppressWarnings("unchecked")
    public List<PersonResidencePeriodResponse> getPlaceResidents(@PathVariable long placeId) {
        DwellingPlace place = dwellingPlaceService.load(placeId);

        if (place == null) {
            throw new ResourceNotFoundException("No place found for ID " + placeId);
        }

        Map<Long, List<PersonResidencePeriodResponse>> personPeriods = new HashMap<>();
        for (HouseholdLocationPeriod householdPeriod : place.getHouseholdPeriods()) {
            // householdPeriod specifies a household and the date range that it lived in this location.
            Household hold = householdPeriod.getHousehold();
            List<HouseholdInhabitantPeriod> overlappingPeriods =
                    (List<HouseholdInhabitantPeriod>) LocalDateComparator.getRangesWithinRange(
                    hold.getInhabitantPeriods(), householdPeriod.getFromDate(), householdPeriod.getToDate());
            for (HouseholdInhabitantPeriod personPeriod : overlappingPeriods) {
                PersonResidencePeriodResponse resp = new PersonResidencePeriodResponse();
                if (personPeriod.getFromDate().isBefore(householdPeriod.getFromDate())) {
                    resp.setFromDate(householdPeriod.getFromDate());
                } else {
                    resp.setFromDate(personPeriod.getFromDate());
                }
                LocalDate personPeriodEndDate = personPeriod.getToDate() == null
                        ? personPeriod.getPerson().getDeathDate()
                        : personPeriod.getToDate();
                if (householdPeriod.getToDate() == null) {
                    resp.setToDate(personPeriodEndDate);
                } else if (householdPeriod.getToDate().isBefore(personPeriodEndDate)) {
                    resp.setToDate(householdPeriod.getToDate());
                } else {
                    resp.setToDate(personPeriodEndDate);
                }
                resp.setPerson(new PersonReference(personPeriod.getPerson()));
                if (!personPeriods.containsKey(personPeriod.getPersonId())) {
                    personPeriods.put(personPeriod.getPersonId(), new ArrayList<>());
                }
                // Omit any cases where the residence "lasted" a day; these may be cases where a person changed
                // households and moved on the same day.
                if (!resp.getFromDate().equals(resp.getToDate())) {
                    personPeriods.get(personPeriod.getPersonId()).add(resp);
                }
            }
        }

        return personPeriods.values().stream()
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(DateRange::getFromDate))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/places", method = RequestMethod.POST)
    public DwellingPlaceDetailResponse postPlace(@RequestBody DwellingPlace place) {
        return new DwellingPlaceDetailResponse(dwellingPlaceService.save(place), null, ancestryService);
    }

    @RequestMapping(value = "/api/estates", method = RequestMethod.POST)
    public DwellingPlaceDetailResponse createEstateForHousehold(@RequestBody EstatePost estatePost) {
        if (estatePost.getParentDwellingPlaceId() == null && estatePost.getExistingHouseId() == null) {
            throw new BadRequestException("Either parentDwellingPlaceId or existingHouseId is required");
        }
        if (estatePost.getParentDwellingPlaceId() != null && estatePost.getExistingHouseId() != null) {
            throw new BadRequestException("Only one of parentDwellingPlaceId and existingHouseId may be non-null");
        }
        Person owner = controllerHelperService.loadPerson(estatePost.getOwnerId());
        LocalDate onDate = controllerHelperService.parseDate(estatePost.getOwnerFromDate());

        Title entailedToTitle = null;
        if (estatePost.getEntailedTitleId() != null) {
            entailedToTitle = titleService.load(estatePost.getEntailedTitleId());
        }

        if (estatePost.getParentDwellingPlaceId() != null) {
            DwellingPlace place = dwellingPlaceService.load(estatePost.getParentDwellingPlaceId());
            return new DwellingPlaceDetailResponse(householdDwellingPlaceService.createEstateInPlace(place, owner,
                    estatePost, onDate, entailedToTitle), onDate, ancestryService);
        } else {
            DwellingPlace house = dwellingPlaceService.load(estatePost.getExistingHouseId());
            if (!(house instanceof  Dwelling)) {
                throw new IllegalArgumentException("existingHouseId " + estatePost.getExistingHouseId()
                        + " does not correspond to a Dwelling");
            }
            return new DwellingPlaceDetailResponse(householdDwellingPlaceService.createEstateAroundDwelling(
                    (Dwelling) house, estatePost, onDate, entailedToTitle), onDate, ancestryService);
        }

    }

    @RequestMapping("/api/places/estates")
    public List<DwellingPlaceReference> getEstates(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.ESTATE).stream()
                .sorted(Comparator.comparing(DwellingPlace::getParentIdOrZero).thenComparing(DwellingPlace::getName))
                .collect(Collectors.toList());
        if (date != null) {
            return estates.stream().map(e -> new DwellingPlaceResponse(e, date)).collect(Collectors.toList());
        } else {
            return estates.stream().map(DwellingPlaceReference::new).collect(Collectors.toList());
        }
    }

    @RequestMapping("/api/places/parishes")
    public List<DwellingPlaceDetailResponse> getParishes(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.PARISH);
        return estates.stream().map(e -> new DwellingPlaceDetailResponse(e, date, ancestryService)).collect(Collectors.toList());
    }

    @RequestMapping("/api/places/towns")
    public List<DwellingPlaceDetailResponse> getTowns(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.TOWN);
        return estates.stream().map(e -> new DwellingPlaceDetailResponse(e, date, ancestryService)).collect(Collectors.toList());
    }

    @RequestMapping("/api/places/farms")
    public List<DwellingPlaceReference> getFarms(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.FARM);
        if (date != null) {
            return estates.stream().map(e -> new DwellingPlaceResponse(e, date)).collect(Collectors.toList());
        } else {
            return estates.stream().map(DwellingPlaceReference::new).collect(Collectors.toList());
        }
    }

    @RequestMapping(value = "/api/places/houses/empty", method = RequestMethod.GET)
    public List<DwellingPlaceDetailResponse> getEmptyHouses(@RequestParam(value = "onDate") String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);
        List<Dwelling> houses = new ArrayList<>();
        for (DwellingPlace parish : dwellingPlaceService.loadByType(DwellingPlaceType.PARISH)) {
            houses.addAll(parish.getEmptyHouses(date));
        }
        return houses.stream()
                .map(h -> new DwellingPlaceDetailResponse(h, date, ancestryService))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/places/unowned", method = RequestMethod.GET)
    public List<DwellingPlaceDetailResponse> getUnownedHousesEstatesAndFarms(@RequestParam(value = "onDate") String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);
        List<DwellingPlace> houses = dwellingPlaceService.getUnownedHousesEstatesAndFarms(date);
        return houses.stream()
                .map(h -> new DwellingPlaceDetailResponse(h, date, ancestryService))
                .collect(Collectors.toList());
    }
}
