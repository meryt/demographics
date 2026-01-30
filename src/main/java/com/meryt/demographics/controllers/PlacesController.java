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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Farm;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdInhabitantPeriod;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.domain.place.Township;
import com.meryt.demographics.domain.place.Region;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.request.DwellingPlacePost;
import com.meryt.demographics.request.EstatePost;
import com.meryt.demographics.request.FarmPost;
import com.meryt.demographics.request.HousePurchasePost;
import com.meryt.demographics.response.DwellingPlaceDetailResponse;
import com.meryt.demographics.response.DwellingPlaceOwnerResponse;
import com.meryt.demographics.response.DwellingPlaceReference;
import com.meryt.demographics.response.DwellingPlaceResponse;
import com.meryt.demographics.response.HouseholdResponseWithLocations;
import com.meryt.demographics.response.PersonReference;
import com.meryt.demographics.response.PersonResidencePeriodResponse;
import com.meryt.demographics.response.calendar.PropertyTransferEvent;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ResourceNotFoundException;
import com.meryt.demographics.service.AncestryService;
import com.meryt.demographics.service.ControllerHelperService;
import com.meryt.demographics.service.DwellingPlaceService;
import com.meryt.demographics.service.HouseholdDwellingPlaceService;
import com.meryt.demographics.service.PersonService;
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
    private final PersonService personService;

    public PlacesController(@Autowired DwellingPlaceService dwellingPlaceService,
                            @Autowired ControllerHelperService controllerHelperService,
                            @Autowired HouseholdDwellingPlaceService householdDwellingPlaceService,
                            @Autowired AncestryService ancestryService,
                            @Autowired TitleService titleService,
                            @Autowired PersonService personService) {
        this.dwellingPlaceService = dwellingPlaceService;
        this.controllerHelperService = controllerHelperService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.ancestryService = ancestryService;
        this.titleService = titleService;
        this.personService = personService;
    }

    @RequestMapping("/api/places/{placeId}")
    public DwellingPlaceDetailResponse getPlace(@PathVariable long placeId,
                                                @RequestParam(value = "onDate", required = false) String onDate) {
        DwellingPlace place = controllerHelperService.loadDwellingPlace(placeId);
        LocalDate date = controllerHelperService.parseDate(onDate);
        return new DwellingPlaceDetailResponse(place, date, ancestryService);
    }

    @RequestMapping("/api/places/{placeId}/owners")
    public List<DwellingPlaceOwnerResponse> getPlaceOwners(@PathVariable long placeId) {
        DwellingPlace place = controllerHelperService.loadDwellingPlace(placeId);

        return place.getOwnerPeriods().stream()
                .sorted(Comparator.comparing(DwellingPlaceOwnerPeriod::getFromDate)
                        .thenComparing(DwellingPlaceOwnerPeriod::getPersonId))
                .map(DwellingPlaceOwnerResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Gets the recursive residents of a place as a list of households with their locations
     */
    @RequestMapping("/api/places/{placeId}/households")
    public List<HouseholdResponseWithLocations> getPlaceHouseholds(@PathVariable long placeId,
                                                                   @RequestParam(value = "onDate") String onDate) {
        DwellingPlace place = controllerHelperService.loadDwellingPlace(placeId);
        LocalDate date = controllerHelperService.parseDate(onDate);
        if (date == null) {
            throw new BadRequestException("onDate is required and must be a valid date");
        }
        return place.getRecursiveHouseholds(date).stream()
                .map(hh -> new HouseholdResponseWithLocations(hh, date, ancestryService))
                .collect(Collectors.toList());
    }

    /**
     * Gets the non-recursive residents of a place over time as individual persons.
     */
    @RequestMapping("/api/places/{placeId}/residents-timeline")
    @SuppressWarnings("unchecked")
    public List<PersonResidencePeriodResponse> getPlaceResidentsTimeline(@PathVariable long placeId) {
        DwellingPlace place = controllerHelperService.loadDwellingPlace(placeId);

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

    /*
    @RequestMapping(value = "/api/places", method = RequestMethod.POST)
    public DwellingPlaceDetailResponse postPlace(@RequestBody DwellingPlace place) {
        return new DwellingPlaceDetailResponse(dwellingPlaceService.save(place), null, ancestryService);
    }
    */

    @RequestMapping(value = "/api/places", method = RequestMethod.POST)
    public DwellingPlaceDetailResponse postPlace(@RequestBody DwellingPlacePost placePost) {
        DwellingPlace place;
        if (placePost.getId() != null) {
            place = dwellingPlaceService.load(placePost.getId());
            if (place == null) {
                throw new ResourceNotFoundException("No dwelling place found for id " + placePost.getId());
            }
        } else {
            if (placePost.getType() == null) {
                throw new BadRequestException("type is required for new places");
            }
            switch (placePost.getType()) {
                case "REGION":
                    place = new Region();
                    break;
                case "TOWNSHIP":
                    place = new Township();
                    break;
                case "FARM":
                    place = new Farm();
                    break;
                case "DWELLING":
                    place = new Dwelling();
                    break;
                default:
                    throw new BadRequestException("Place type " + placePost.getType() + " is invalid or not yet implemented");
            }    
        }
        if (placePost.getName() != null) {
            place.setName(placePost.getName());
        }
        if (placePost.getParentId() != null) {
            DwellingPlace parent = dwellingPlaceService.load(placePost.getParentId());
            if (parent == null) {
                throw new ResourceNotFoundException("No dwelling place found for parent ID " + placePost.getParentId());
            }
            place.setParent(parent);
        }
        LocalDate foundedDate = controllerHelperService.parseDate(placePost.getFoundedDate());
        if (foundedDate != null) {
            place.setFoundedDate(foundedDate);
        }
        if (placePost.getOwnerId() != null) {
            Person owner = personService.load(placePost.getOwnerId());
            if (owner == null) {
                throw new ResourceNotFoundException("No person found for ownerId " + placePost.getOwnerId());   
            }
            if (placePost.getOwnerFromDate() == null) {
                throw new BadRequestException("If ownerId is provided, ownerFromDate must be provided");
            }
            LocalDate ownerFromDate = controllerHelperService.parseDate(placePost.getOwnerFromDate());
            if (!owner.isLiving(ownerFromDate)) {
                throw new BadRequestException("Owner " + owner.getName() + " is not alive on requested owner from date " + placePost.getOwnerFromDate());
            }
            if (placePost.getOwnerReason() == null) {
                throw new BadRequestException("If ownerId is provided, ownerReason must be provided");
            }
            place.addOwner(owner, ownerFromDate, null, placePost.getOwnerReason());
            if (place.getType() == DwellingPlaceType.DWELLING && place.getName() == null) {
                place.setName("House of " + owner.getName());
            }
        }
        if (placePost.getAcres() != null) {
            place.setAcres(placePost.getAcres());
        }
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



    @RequestMapping(value = "/api/farms", method = RequestMethod.POST)
    public DwellingPlaceDetailResponse createFarmForHousehold(@RequestBody FarmPost farmPost) {
        if (farmPost.getParentDwellingPlaceId() == null) {
            throw new BadRequestException("parentDwellingPlaceId is required");
        }
        Person owner = controllerHelperService.loadPerson(farmPost.getOwnerId());
        LocalDate onDate = controllerHelperService.parseDate(farmPost.getOwnerFromDate());
        DwellingPlace parentPlace = dwellingPlaceService.load(farmPost.getParentDwellingPlaceId());
        return new DwellingPlaceDetailResponse(householdDwellingPlaceService.createFarmForPerson(parentPlace, owner,
                farmPost, onDate), onDate, ancestryService);
    }

    @RequestMapping(value = "/api/houses/{houseId}/purchase")
    public List<PropertyTransferEvent> purchaseHouse(@PathVariable long houseId,
                                                     @RequestBody HousePurchasePost housePurchaseRequest) {
        DwellingPlace place = controllerHelperService.loadDwellingPlace(houseId);
        if (!place.isHouse()) {
            throw new IllegalArgumentException("Can only purchase a house. If the house has a Farm or Estate " +
                    "attached, this will be purchased along with the house.");
        }
        Dwelling house = (Dwelling) place;

        LocalDate onDate = controllerHelperService.parseDate(housePurchaseRequest.getOnDate());

        Person buyer = controllerHelperService.loadPerson(housePurchaseRequest.getNewOwnerId());
        if (!buyer.isLiving(onDate)) {
            throw new IllegalArgumentException(String.format("%s is not living on %s", buyer.getIdAndName(),
                    onDate));
        }

        double value = house.getNullSafeValueIncludingAttachedParent();
        if (value > buyer.getCapitalNullSafe(onDate)) {
            throw new IllegalArgumentException(String.format(
                    "%s does not have the %f in cash necessary to buy this house", buyer.getIdAndName(), value));
        }

        return householdDwellingPlaceService.buyAndMoveIntoHouse(house, buyer, onDate, "Purchased");
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

    @RequestMapping("/api/places/top-level")
    public List<DwellingPlaceDetailResponse> getTopLevelPlaces(@RequestParam(value = "onDate", required = false) String onDate) {
        final LocalDate date = controllerHelperService.parseDate(onDate);

        List<DwellingPlace> places = dwellingPlaceService.loadByNoParent();
        return places.stream().map(e -> new DwellingPlaceDetailResponse(e, date, ancestryService)).collect(Collectors.toList());
    }

    @RequestMapping("/api/places")
    public Page<DwellingPlaceDetailResponse> getPlaces(@RequestParam(value = "canContainType", required = false) String canContainType,
                                                       @RequestParam(value = "onDate", required = false) String onDate,
                                                       @RequestParam(value = "page", required = false) Integer page,
                                                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                       @RequestParam(value = "sortBy", required = false) String sortBy) {
        final LocalDate date = controllerHelperService.parseDate(onDate);
        DwellingPlaceType canContainTypeEnum = null;
        if (canContainType != null && !canContainType.isBlank()) {
            try {
                canContainTypeEnum = DwellingPlaceType.valueOf(canContainType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid canContainType: " + canContainType
                        + ". Must be one of: " + java.util.Arrays.toString(DwellingPlaceType.values()));
            }
        }
        int pageNum = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 20;
        Sort sort = StringUtils.hasText(sortBy) ? Sort.by(Sort.Direction.ASC, sortBy) : Sort.by(Sort.Direction.ASC, "id");
        Pageable pageable = PageRequest.of(pageNum, size, sort);
        Page<DwellingPlace> places = dwellingPlaceService.getPlaces(canContainTypeEnum, pageable);
        return places.map(e -> new DwellingPlaceDetailResponse(e, date, ancestryService));
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
