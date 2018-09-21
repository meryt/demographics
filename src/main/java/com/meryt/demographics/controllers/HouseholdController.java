package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
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
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.request.HouseholdPlacePost;
import com.meryt.demographics.request.HouseholdPost;
import com.meryt.demographics.response.HouseholdResponseWithLocations;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.HouseholdDwellingPlaceService;
import com.meryt.demographics.service.HouseholdService;
import com.meryt.demographics.service.PersonService;

@Slf4j
@RestController
public class HouseholdController {

    private final HouseholdService householdService;
    private final PersonService personService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;
    private final DwellingPlaceService dwellingPlaceService;
    private final ControllerHelperService controllerHelperService;

    public HouseholdController(@Autowired HouseholdService householdService,
                               @Autowired PersonService personService,
                               @Autowired DwellingPlaceService dwellingPlaceService,
                               @Autowired HouseholdDwellingPlaceService householdDwellingPlaceService,
                               @Autowired ControllerHelperService controllerHelperService) {
        this.householdService = householdService;
        this.personService = personService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.controllerHelperService = controllerHelperService;
    }

    @RequestMapping("/api/households/{householdId}")
    public HouseholdResponseWithLocations getHousehold(@PathVariable long householdId,
                                                       @RequestParam(value = "onDate", required = false) String onDate) {
        Household household = loadHousehold(householdId);
        LocalDate date;
        if (onDate != null) {
            date = controllerHelperService.parseDate(onDate);
        } else {
            date = controllerHelperService.parseDate("current");
        }
        return new HouseholdResponseWithLocations(household, date);
    }

    @RequestMapping("/api/households/homeless")
    public List<HouseholdResponseWithLocations> getHouseholdsWithoutHouses(@RequestParam(value = "onDate") String onDate) {
        if (onDate == null) {
            throw new BadRequestException("onDate cannot be null");
        }
        final LocalDate date = controllerHelperService.parseDate(onDate);

        return Stream.concat(householdService.loadHouseholdsWithoutHouses(date).stream(),
                householdService.loadHouseholdsWithoutLocations(date).stream())
                .map(h -> new HouseholdResponseWithLocations(h, date))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/households", method = RequestMethod.POST)
    public HouseholdResponseWithLocations postHousehold(@RequestBody HouseholdPost householdPost) {
        if (householdPost.getAsOfDate() == null) {
            throw new BadRequestException("asOfDate cannot be null");
        }

        if (householdPost.getHeadId() == null) {
            throw new BadRequestException("headId cannot be null");
        }

        LocalDate onDate = controllerHelperService.parseDate(householdPost.getAsOfDate());

        Person person = personService.load(householdPost.getHeadId());
        if (person == null) {
            throw new ResourceNotFoundException("No person found for ID " + householdPost.getHeadId());
        }
        if (!person.isLiving(onDate)) {
            throw new BadRequestException(String.format("%d %s was not living on %s", person.getId(), person.getName(),
                    householdPost.getAsOfDate()));
        }

        Household household = householdService.createHouseholdForHead(person, onDate,
                householdPost.getIncludeHomelessFamilyMembers());
        return new HouseholdResponseWithLocations(household, onDate);
    }

    /**
     * Can be used to move a household to a specific new house, or to find or create a new house for them.
     *
     * @param householdPlacePost the request body
     * @param householdId the household that is moving
     * @return info about the household after the move
     */
    @RequestMapping(value = "/api/households/{householdId}/places", method = RequestMethod.POST)
    public HouseholdResponseWithLocations postHouseholdPlace(@RequestBody HouseholdPlacePost householdPlacePost,
                                                             @PathVariable long householdId) {
        Household household = loadHousehold(householdId);
        if (householdPlacePost.getOnDate() == null) {
            throw new BadRequestException("onDate is required");
        }

        LocalDate onDate = controllerHelperService.parseDate(householdPlacePost.getOnDate());

        DwellingPlace specificPlace = householdPlacePost.getDwellingPlaceId() == null
                ? null
                : dwellingPlaceService.load(householdPlacePost.getDwellingPlaceId());
        if (specificPlace == null) {
            // Need to find a place for them to live.
            DwellingPlace currentDwelling = household.getDwellingPlace(onDate);
            if (currentDwelling != null) {
                specificPlace = currentDwelling.getParish();
            } else {
                List<DwellingPlace> parishes = dwellingPlaceService.loadByType(DwellingPlaceType.PARISH);
                if (parishes.isEmpty()) {
                    throw new BadRequestException("Unable to pick a random parish: no parishes exist");
                }
                Collections.shuffle(parishes);
                specificPlace = parishes.get(0);
            }
        }

        if (specificPlace instanceof Parish) {
            householdDwellingPlaceService.buyOrCreateOrMoveIntoEmptyHouse((Parish) specificPlace, household,
                    onDate, DwellingPlaceOwnerPeriod.ReasonToPurchase.MOVE_TO_PARISH);
        } else if (specificPlace instanceof Dwelling) {
            Parish parish = specificPlace.getParish();
            if (householdPlacePost.getEvictCurrentResidents()) {
                for (Household evictedHousehold : specificPlace.getHouseholds(onDate)) {
                    List<CalendarDayEvent> events = householdDwellingPlaceService.buyOrCreateOrMoveIntoEmptyHouse(parish,
                            evictedHousehold, onDate, DwellingPlaceOwnerPeriod.ReasonToPurchase.EVICTION);
                    if (!events.isEmpty()) {
                        events.forEach(e -> log.info(e.toLogMessage()));
                    }
                }
            }
            householdDwellingPlaceService.addToDwellingPlace(household, specificPlace, onDate, null);
        } else {
            throw new BadRequestException("dwellingPlaceId must be either a Parish, a Dwelling, or null");
        }

        return new HouseholdResponseWithLocations(household, onDate);
    }

    @NonNull
    private Household loadHousehold(long householdId) {
        Household result = householdService.load(householdId);
        if (result == null) {
            throw new ResourceNotFoundException("No household found for ID " + householdId);
        }
        return result;
    }
}
