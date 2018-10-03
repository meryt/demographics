package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Estate;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.generator.ParishGenerator;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.request.EstatePost;
import com.meryt.demographics.response.DwellingPlaceOwnerResponse;
import com.meryt.demographics.response.DwellingPlaceReference;
import com.meryt.demographics.response.DwellingPlaceDetailResponse;
import com.meryt.demographics.response.DwellingPlaceResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.AncestryService;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.HouseholdDwellingPlaceService;
import com.meryt.demographics.service.HouseholdService;
import com.meryt.demographics.service.PersonService;
import com.meryt.demographics.service.TitleService;

@Slf4j
@RestController
public class PlacesController {

    private final DwellingPlaceService dwellingPlaceService;
    private final ControllerHelperService controllerHelperService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;
    private final TitleService titleService;
    private final PersonService personService;
    private final ParishGenerator parishGenerator;
    private final AncestryService ancestryService;
    private final HouseholdService householdService;

    public PlacesController(@Autowired DwellingPlaceService dwellingPlaceService,
                            @Autowired ControllerHelperService controllerHelperService,
                            @Autowired HouseholdDwellingPlaceService householdDwellingPlaceService,
                            @Autowired PersonService personService,
                            @Autowired ParishGenerator parishGenerator,
                            @Autowired TitleService titleService,
                            @Autowired AncestryService ancestryService,
                            @Autowired HouseholdService householdService) {
        this.dwellingPlaceService = dwellingPlaceService;
        this.controllerHelperService = controllerHelperService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.personService = personService;
        this.parishGenerator = parishGenerator;
        this.titleService = titleService;
        this.ancestryService = ancestryService;
        this.householdService = householdService;
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

    @RequestMapping(value = "/api/places", method = RequestMethod.POST)
    public DwellingPlaceDetailResponse postPlace(@RequestBody DwellingPlace place) {
        return new DwellingPlaceDetailResponse(dwellingPlaceService.save(place), null, ancestryService);
    }

    @RequestMapping(value = "/api/estates", method = RequestMethod.POST)
    public DwellingPlaceDetailResponse createEstateForHousehold(@RequestBody EstatePost estatePost) {
        if (estatePost.getParentDwellingPlaceId() == null) {
            throw new BadRequestException("parentDwellingPlaceIdIsRequired");
        }
        DwellingPlace place = dwellingPlaceService.load(estatePost.getParentDwellingPlaceId());
        Person owner = controllerHelperService.loadPerson(estatePost.getOwnerId());
        LocalDate onDate = controllerHelperService.parseDate(estatePost.getOwnerFromDate());

        Household ownerHousehold = owner.getHousehold(onDate);
        if (ownerHousehold == null) {
            ownerHousehold = householdService.createHouseholdForHead(owner, onDate, true);
        }

        Title entailedTitle = null;
        if (estatePost.getEntailedTitleId() != null) {
            entailedTitle = titleService.load(estatePost.getEntailedTitleId());
        }

        Estate estate = householdDwellingPlaceService.createEstateForHousehold(place, estatePost.getName(),
                estatePost.getDwellingName(), owner, ownerHousehold, onDate, entailedTitle);

        if (owner.getCapitalPeriods().isEmpty()) {
            double capital = WealthGenerator.getRandomStartingCapital(owner.getSocialClass(),
                    owner.getOccupation(onDate) != null);
            owner.addCapital(capital, onDate, PersonCapitalPeriod.Reason.startingCapitalMessage());
            log.info(String.format("%d %s got starting capital of %.2f", owner.getId(), owner.getName(), capital));
            personService.save(owner);
        }

        if (estatePost.getMustPurchase() != null && estatePost.getMustPurchase()) {
            owner.addCapital(estate.getValue() * -1, onDate,
                    PersonCapitalPeriod.Reason.builtNewDwellingPlaceMessage(estate));
            personService.save(owner);
        }

        parishGenerator.populateEstateWithEmployees(estate, onDate);

        return new DwellingPlaceDetailResponse(estate, onDate, ancestryService);
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
    public List<DwellingPlaceDetailResponse> getFarms(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.FARM);
        return estates.stream().map(e -> new DwellingPlaceDetailResponse(e, date, ancestryService)).collect(Collectors.toList());
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
