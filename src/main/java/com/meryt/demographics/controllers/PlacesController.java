package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
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

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Estate;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.generator.ParishGenerator;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.request.EstatePost;
import com.meryt.demographics.response.DwellingPlaceResponse;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.HouseholdDwellingPlaceService;
import com.meryt.demographics.service.PersonService;

@Slf4j
@RestController
public class PlacesController {

    private final DwellingPlaceService dwellingPlaceService;
    private final ControllerHelperService controllerHelperService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;
    private final PersonService personService;
    private final ParishGenerator parishGenerator;

    public PlacesController(@Autowired DwellingPlaceService dwellingPlaceService,
                            @Autowired ControllerHelperService controllerHelperService,
                            @Autowired HouseholdDwellingPlaceService householdDwellingPlaceService,
                            @Autowired PersonService personService,
                            @Autowired ParishGenerator parishGenerator) {
        this.dwellingPlaceService = dwellingPlaceService;
        this.controllerHelperService = controllerHelperService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.personService = personService;
        this.parishGenerator = parishGenerator;
    }

    @RequestMapping("/api/places/{placeId}")
    public DwellingPlaceResponse getPlace(@PathVariable long placeId,
                                          @RequestParam(value = "onDate", required = false) String onDate) {
        DwellingPlace place = dwellingPlaceService.load(placeId);

        if (place == null) {
            throw new ResourceNotFoundException("No place found for ID " + placeId);
        } else {
            LocalDate date = controllerHelperService.parseDate(onDate);
            return new DwellingPlaceResponse(place, date);
        }
    }

    @RequestMapping(value = "/api/places", method = RequestMethod.POST)
    public DwellingPlaceResponse postPlace(@RequestBody DwellingPlace place) {
        return new DwellingPlaceResponse(dwellingPlaceService.save(place), null);
    }

    @RequestMapping(value = "/api/estates", method = RequestMethod.POST)
    public DwellingPlaceResponse createEstateForHousehold(@RequestBody EstatePost estatePost) {
        if (estatePost.getParentDwellingPlaceId() == null) {
            throw new BadRequestException("parentDwellingPlaceIdIsRequired");
        }
        DwellingPlace place = dwellingPlaceService.load(estatePost.getParentDwellingPlaceId());
        Person owner = controllerHelperService.loadPerson(estatePost.getOwnerId());
        LocalDate onDate = controllerHelperService.parseDate(estatePost.getOwnerFromDate());

        Household ownerHousehold = owner.getHousehold(onDate);
        if (ownerHousehold == null) {
            throw new BadRequestException(String.format("%d %s is not a member of a household on %s",
                    owner.getId(), owner.getName(), onDate));
        }

        Title entailedTitle = null;
        if (estatePost.getEntailedTitleId() != null) {
            entailedTitle = owner.getTitles(onDate).stream()
                    .map(PersonTitlePeriod::getTitle)
                    .filter(t -> t.getId() == estatePost.getEntailedTitleId())
                    .findFirst().orElse(null);
            if (entailedTitle == null) {
                throw new BadRequestException(String.format("%d %s does not have the title %d on date %s",
                        owner.getId(), owner.getName(), estatePost.getEntailedTitleId(), onDate));
            }
        }

        Estate estate = householdDwellingPlaceService.createEstateForHousehold(place, estatePost.getName(),
                estatePost.getDwellingName(), owner, ownerHousehold, onDate, entailedTitle);

        if (owner.getCapitalPeriods().isEmpty()) {
            double capital = WealthGenerator.getRandomStartingCapital(owner.getSocialClass(),
                    owner.getOccupation(onDate) != null);
            owner.addCapital(capital, onDate);
            log.info(String.format("%d %s got starting capital of %.2f", owner.getId(), owner.getName(), capital));
            personService.save(owner);
        }

        parishGenerator.populateEstateWithEmployees(estate, onDate);

        return new DwellingPlaceResponse(estate, onDate);
    }

    @RequestMapping("/api/places/estates")
    public List<DwellingPlaceResponse> getEstates(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.ESTATE);
        return estates.stream().map(e -> new DwellingPlaceResponse(e, date)).collect(Collectors.toList());
    }

    @RequestMapping("/api/places/parishes")
    public List<DwellingPlaceResponse> getParishes(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.PARISH);
        return estates.stream().map(e -> new DwellingPlaceResponse(e, date)).collect(Collectors.toList());
    }

    @RequestMapping("/api/places/towns")
    public List<DwellingPlaceResponse> getTowns(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.TOWN);
        return estates.stream().map(e -> new DwellingPlaceResponse(e, date)).collect(Collectors.toList());
    }

    @RequestMapping("/api/places/farms")
    public List<DwellingPlaceResponse> getFarms(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);

        List<DwellingPlace> estates = dwellingPlaceService.loadByType(DwellingPlaceType.FARM);
        return estates.stream().map(e -> new DwellingPlaceResponse(e, date)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/places/houses/empty", method = RequestMethod.GET)
    public List<DwellingPlaceResponse> getEmptyHouses(@RequestParam(value = "onDate") String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);
        List<Dwelling> houses = new ArrayList<>();
        for (DwellingPlace parish : dwellingPlaceService.loadByType(DwellingPlaceType.PARISH)) {
            houses.addAll(parish.getEmptyHouses(date));
        }
        return houses.stream()
                .map(h -> new DwellingPlaceResponse(h, date))
                .collect(Collectors.toList());
    }
}
