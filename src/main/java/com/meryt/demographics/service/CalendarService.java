package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Farm;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.request.AdvanceToDatePost;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.response.calendar.BirthEvent;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.response.calendar.CalendarEventType;
import com.meryt.demographics.response.calendar.DeathEvent;
import com.meryt.demographics.response.calendar.EmploymentEvent;
import com.meryt.demographics.response.calendar.MarriageEvent;
import com.meryt.demographics.response.calendar.NewFarmEvent;
import com.meryt.demographics.response.calendar.PropertyTransferEvent;
import com.meryt.demographics.rest.BadRequestException;

@Slf4j
@Service
public class CalendarService {

    private final ConfigurationService configurationService;
    private final PersonService personService;
    private final FamilyGenerator familyGenerator;
    private final FertilityService fertilityService;
    private final FamilyService familyService;
    private final InheritanceService inheritanceService;
    private final OccupationService occupationService;
    private final WealthService wealthService;
    private final DwellingPlaceService dwellingPlaceService;
    private final ImmigrationService immigrationService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;
    private final TitleService titleService;
    private final HouseholdService householdService;

    public CalendarService(@Autowired @NonNull ConfigurationService configurationService,
                           @Autowired @NonNull PersonService personService,
                           @Autowired @NonNull FamilyGenerator familyGenerator,
                           @Autowired @NonNull FertilityService fertilityService,
                           @Autowired @NonNull FamilyService familyService,
                           @Autowired @NonNull InheritanceService inheritanceService,
                           @Autowired @NonNull OccupationService occupationService,
                           @Autowired @NonNull WealthService wealthService,
                           @Autowired @NonNull DwellingPlaceService dwellingPlaceService,
                           @Autowired @NonNull ImmigrationService immigrationService,
                           @Autowired @NonNull HouseholdDwellingPlaceService householdDwellingPlaceService,
                           @Autowired @NonNull TitleService titleService,
                           @Autowired @NonNull HouseholdService householdService) {
        this.configurationService = configurationService;
        this.personService = personService;
        this.familyGenerator = familyGenerator;
        this.fertilityService = fertilityService;
        this.familyService = familyService;
        this.inheritanceService = inheritanceService;
        this.occupationService = occupationService;
        this.wealthService = wealthService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.immigrationService = immigrationService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.titleService = titleService;
        this.householdService = householdService;
    }

    /**
     * Perform checks on current date up to given date.
     *
     * @param toDate the end date (inclusive)
     * @return a list of things that happened on this day
     */
    public Map<LocalDate, List<CalendarDayEvent>> advanceToDay(@NonNull LocalDate toDate,
                                                               @NonNull AdvanceToDatePost nextDatePost) {
        LocalDate currentDate = configurationService.getCurrentDate();
        if (currentDate == null) {
            throw new IllegalStateException("Current date is null");
        }

        configurationService.unpauseCheck();

        RandomFamilyParameters familyParameters = nextDatePost.getFamilyParameters();

        Map<LocalDate, List<CalendarDayEvent>> results = new TreeMap<>();
        int i = 0;
        int matBatchSize = nextDatePost.getMaternityNumDaysOrDefault();
        if (matBatchSize <= 0) {
            throw new BadRequestException("maternityNumDays must be a positive integer (defaults to 1 if not specified)");
        }
        for (LocalDate date = currentDate.plusDays(1); !date.isAfter(toDate); date = date.plusDays(1)) {
            log.debug(String.format("Checking for events on %s", date));

            if (configurationService.isPauseCheck()) {
                log.info("Checking is paused; exiting loop.");
                break;
            }

            Map<LocalDate, List<CalendarDayEvent>> marriageEvents = generateMarriagesToDate(date, familyParameters,
                    nextDatePost.getFarmNamesOrDefault());
            results = mergeMaps(results, marriageEvents);

            // Say the batch size is 7. If so, we don't check on the 0th through 5th date, but do on the 6th.
            // Or, in case the number of days is such that less than a full 7 days fits in at the end, we always
            // check on the last day of the iteration.
            if (i % matBatchSize == (matBatchSize - 1) || date.equals(toDate)) {
                // Allow the maternity check to rebuild ancestry only if:
                // - the number of days between ancestry rebuilds is left null, or
                // - the number of days to check is less than the number of days between rebuilds (e.g. we are only
                //   checking one day at a time, we don't want to rebuild ancestry every time, we only want to do it
                //   if there were births)
                Map<LocalDate, List<CalendarDayEvent>> maternityEvents = advanceMaternitiesToDay(date);
                results = mergeMaps(results, maternityEvents);
            }

            Map<LocalDate, List<CalendarDayEvent>> deathEvents = processDeathsOnDay(date);
            results = mergeMaps(results, deathEvents);

            Map<LocalDate, List<CalendarDayEvent>> immigrantEvents = processImmigrants(date, nextDatePost);
            results = mergeMaps(results, immigrantEvents);

            Map<LocalDate, List<CalendarDayEvent>> titleEvents = processTitlesInAbeyance(date);
            results = mergeMaps(results, titleEvents);

            if (date.getMonthValue() == nextDatePost.getFirstMonthOfYearOrDefault()
                    && date.getDayOfMonth() == nextDatePost.getFirstDayOfYearOrDefault()) {
                distributeCapital(date);
            }
            configurationService.setCurrentDate(date);
            i++;
        }

        checkForErrors(configurationService.getCurrentDate());

        filterOutEventTypes(results, nextDatePost);

        log.info("Finished advancing calendar");

        return results;
    }

    private Map<LocalDate, List<CalendarDayEvent>> generateMarriagesToDate(@NonNull LocalDate date,
                                                                           @NonNull RandomFamilyParameters familyParameters,
                                                                           @NonNull List<String> farmNames) {
        List<Person> unmarriedPeople = personService.findUnmarriedPeople(date,
                familyParameters.getMinHusbandAgeOrDefault(),
                familyParameters.getMaxHusbandAgeOrDefault(),
                familyParameters.getMinWifeAgeOrDefault(),
                familyParameters.getMaxWifeAgeOrDefault(),
                false, // residentsOnly
                null); // gender (i.e. find both genders)
        Map<LocalDate, List<CalendarDayEvent>> results = new TreeMap<>();
        List<CalendarDayEvent> dayResults = new ArrayList<>();

        for (Person person : unmarriedPeople) {
            Family family = familyGenerator.attemptToFindSpouse(date, date, person, familyParameters);
            if (family != null) {
                familyService.save(family);
                dayResults.add(new MarriageEvent(date, family));
                logMarriage(family, date);
                family = familyService.setupMarriage(family, family.getWeddingDate(), true);
                family.getWife().getMaternity().setHavingRelations(false);
                // If the woman is randomly generated her last check date is in the past. Bring her up to yesterday so
                // that in the next step when we advance maternities, she will start with her wedding night.
                fertilityService.cycleToDate(family.getWife(), date.minusDays(1), false);
                family.getWife().getMaternity().setHavingRelations(true);
                personService.save(family.getWife());

                Household household = person.getHousehold(date);
                if (household != null) {
                    DwellingPlace householdLocation = household.getDwellingPlace(date);
                    if (householdLocation != null) {
                        Occupation occupation = occupationService.findAvailableOccupationForPerson(person,
                                householdLocation, date);
                        if (occupation != null) {
                            person.addOccupation(occupation, date);
                            personService.save(person);
                            dayResults.add(new EmploymentEvent(date, person, occupation));

                            if (occupation.isFarmOwner()) {
                                DwellingPlace dwellingPlace = person.getResidence(date);
                                if (dwellingPlace != null && dwellingPlace.isHouse()
                                        && !dwellingPlace.getParent().isFarm()
                                        /* disallow putting a farm above a manor house (farm houses are also
                                         * attached to their parents, but they are excluded in the previous line */
                                        && !dwellingPlace.isAttachedToParent()
                                        && !dwellingPlace.isEntailed()) {
                                    Farm farm = householdDwellingPlaceService.convertRuralHouseToFarm(
                                            (Dwelling) dwellingPlace, date, farmNames);
                                    if (farm != null) {
                                        dayResults.add(new NewFarmEvent(date, farm));
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
        if (!dayResults.isEmpty()) {
            results.put(date, dayResults);
        }

        return results;
    }

    private Map<LocalDate, List<CalendarDayEvent>> advanceMaternitiesToDay(@NonNull LocalDate date) {
        List<Person> women = personService.findWomenWithPendingMaternities(date);
        log.info(women.size() + " women need to be checked");
        Map<LocalDate, List<CalendarDayEvent>> results = new TreeMap<>();
        for (Person woman : women) {
            // FIXME HACK we have inheritance problems with a woman dying before her expected death date. So don't
            // allow her to die in (at least) these conditions.
            boolean allowMaternalDeath = woman.getTitles().isEmpty() && woman.getOwnedDwellingPlaces().isEmpty();
            List<CalendarDayEvent> daysResults = fertilityService.cycleToDate(woman, date, allowMaternalDeath);
            for (CalendarDayEvent result : daysResults) {
                if (!results.containsKey(result.getDate())) {
                    results.put(result.getDate(), new ArrayList<>());
                }
                results.get(result.getDate()).add(result);
                if (result.getType() == CalendarEventType.BIRTH) {
                    BirthEvent event = (BirthEvent) result;
                    // If the child died before the given date, process the death, since we will have already passed
                    // it by in the main loop due to the batching of maternity checks.
                    if (event.getChild().getDeathDate().isBefore(date)) {
                        Map<LocalDate, List<CalendarDayEvent>> childDeathResults = processSingleDeath(event.getChild(),
                                event.getChild().getDeathDate());
                        results = mergeMaps(results, childDeathResults);
                    }
                }
                if (result.getType() == CalendarEventType.DEATH && !result.getDate().equals(date)) {
                    // A mother died in childbirth but the date is in the past according to our batching logic.
                    // Go back and process her death.
                    Map<LocalDate, List<CalendarDayEvent>> deathResults = processSingleDeath(woman, result.getDate());
                    results = mergeMaps(results, deathResults);
                }
            }
        }
        return results;
    }

    private Map<LocalDate, List<CalendarDayEvent>> processDeathsOnDay(@NonNull LocalDate date) {
        List<Person> peopleDyingToday = personService.findByDeathDate(date);
        Map<LocalDate, List<CalendarDayEvent>> results = new TreeMap<>();
        for (Person person : peopleDyingToday) {
            Map<LocalDate, List<CalendarDayEvent>> singleResult = processSingleDeath(person, date);
            results = mergeMaps(results, singleResult);
        }

        return results;
    }

    public Map<LocalDate, List<CalendarDayEvent>> processSingleDeath(@NonNull Person person, @NonNull LocalDate date) {
        Map<LocalDate, List<CalendarDayEvent>> results = new TreeMap<>();
        List<CalendarDayEvent> daysResults = new ArrayList<>();
        log.info(String.format("%d %s died on %s, aged %d", person.getId(), person.getName(), date,
                person.getAgeInYears(date)));
        List<CalendarDayEvent> events = personService.processDeath(person);
        daysResults.addAll(titleService.processDeadPersonsTitles(person));
        daysResults.addAll(inheritanceService.processDeath(person));
        daysResults.add(new DeathEvent(date, person));
        daysResults.addAll(events);
        if (!daysResults.isEmpty()) {
            results.put(date, daysResults);
        }
        return results;
    }

    /**
     * Based on the percent chance of immigrant arrival, checks each parish for a new immigrant household on this date.
     * If one is indicated, generates the household, family, residence, and possibly occupation.
     *
     * @param date the date to check for an arrival (for each parish)
     * @param nextDatePost the request parameters, used to determine the percent chance as well as to configure the
     *                     family generation using the RandomFamilyParameters.
     * @return a map of events generated (possibly empty)
     */
    private Map<LocalDate, List<CalendarDayEvent>> processImmigrants(@NonNull LocalDate date,
                                                                     @NonNull AdvanceToDatePost nextDatePost) {
        Map<LocalDate, List<CalendarDayEvent>> results = new HashMap<>();
        List<CalendarDayEvent> dayResults = new ArrayList<>();
        if (nextDatePost.getChanceNewFamilyPerYear() == null) {
            return results;
        }

        for (DwellingPlace parish :  dwellingPlaceService.loadByType(DwellingPlaceType.PARISH)) {
            double chance = nextDatePost.getChanceNewFamilyPerYear() / 365.0;
            if (new PercentDie().roll() < chance) {
                // Someone might want to immigrate. But if the population density is such that it is exerting outward
                // pressure, they will not come after all. So only come if the density is low enough or the roll is
                // high enough.
                double chanceOfEmigrating = ((Parish) parish).getChanceOfEmigrating(date);
                if (new PercentDie().roll() > chanceOfEmigrating) {
                    dayResults.addAll(immigrationService.processImmigrantArrival((Parish) parish,
                            nextDatePost.getFamilyParameters(), date));
                }
            }
        }

        if (!dayResults.isEmpty()) {
            results.put(date, dayResults);
        }

        return results;
    }


    private Map<LocalDate, List<CalendarDayEvent>> processTitlesInAbeyance(@NonNull LocalDate onDate) {
        List<Title> titles = titleService.findTitlesForAbeyanceCheck(onDate);
        Map<LocalDate, List<CalendarDayEvent>> results = new HashMap<>();
        List<CalendarDayEvent> dayResults = new ArrayList<>();

        for (Title title : titles) {
            List<CalendarDayEvent> result = titleService.checkForSingleTitleHeir(title, onDate, null);
            dayResults.addAll(result);
        }
        if (!dayResults.isEmpty()) {
            results.put(onDate, dayResults);
        }
        return results;
    }

    private Map<LocalDate, List<CalendarDayEvent>> mergeMaps(Map<LocalDate, List<CalendarDayEvent>> map1,
                                                             Map<LocalDate, List<CalendarDayEvent>> map2) {
        Map<LocalDate, List<CalendarDayEvent>> map3 = new TreeMap<>(map1);
        map2.forEach((k, v) -> map3.merge(k, v, (v1, v2) -> {
            List<CalendarDayEvent> list = new ArrayList<>(v1);
            list.addAll(v2);
            return list.stream()
                .sorted(Comparator.comparing(CalendarDayEvent::getType))
                .collect(Collectors.toList());
        }));
        return map3;
    }

    private void logMarriage(@NonNull Family family, @NonNull LocalDate date) {
        Person husband = family.getHusband();
        Occupation husbandOccupation = husband.getOccupation(date);
        String husbandOccupationName = husbandOccupation == null ? "unemployed" : husbandOccupation.getName();

        Person wife = family.getWife();
        Occupation wifeOccupation = wife.getOccupation(date);
        String wifeOccupationName = wifeOccupation == null ? "homemaker" : wifeOccupation.getName();

        DwellingPlace husbandResidence = husband.getResidence(date);
        String husbandLocation = husbandResidence == null ? "elsewhere" : husbandResidence.getLocationString();

        DwellingPlace wifeResidence = wife.getResidence(date);
        String wifeLocation = wifeResidence == null ? "elsewhere" : wifeResidence.getLocationString();

        log.info(String.format("%d %s, age %d, %s of %s, married %d %s, age %d, %s of %s, on %s",
                husband.getId(), husband.getName(), husband.getAgeInYears(date), husbandOccupationName, husbandLocation,
                wife.getId(), wife.getName(), wife.getAgeInYears(date), wifeOccupationName, wifeLocation, date));
    }

    private void filterOutEventTypes(Map<LocalDate, List<CalendarDayEvent>> map1,
                                     @NonNull AdvanceToDatePost post) {
        for (Map.Entry<LocalDate, List<CalendarDayEvent>> entry : map1.entrySet()) {
            entry.getValue().removeIf(post::isSuppressedEventType);
            entry.getValue().removeIf(e -> e.getType() == CalendarEventType.PROPERTY_TRANSFER &&
                    ((PropertyTransferEvent) e).getDwellingPlace().getType().equals("DWELLING"));
        }

        map1.values().removeIf(List::isEmpty);
    }

    public void checkForErrors(@NonNull LocalDate onDate) {
        dwellingPlaceService.getUnownedHousesEstatesAndFarms(onDate).forEach(p ->
            log.warn(String.format("%d %s has no owner on %s", p.getId(), p.getFriendlyName(), onDate)));
        householdService.loadHouseholdsWithoutHouses(onDate).forEach(h ->
            log.warn(String.format("%d %s is a homeless household on %s", h.getId(), h.getFriendlyName(onDate), onDate)));
        dwellingPlaceService.getPlacesSeparatedFromParents(onDate).forEach(p ->
            log.warn(String.format("%d %s is owned by %s but its parent %d %s is owned by %s",
                    p.getId(), p.getFriendlyName(), p.getOwners(onDate).stream()
                            .map(o -> o.getId() + " " + o.getName()).collect(Collectors.joining(", ")),
                    p.getParent().getId(), p.getParent().getFriendlyName(), p.getParent().getOwners(onDate).stream()
                            .map(o -> o.getId() + " " + o.getName()).collect(Collectors.joining(", ")))));
    }

    private void distributeCapital(@NonNull LocalDate date) {
        double goodYearFactor = (new BetweenDie()).roll(-20, 20) * 0.01;
        log.info(String.format("%d was a %s year with a factor of %s", date.getYear() - 1,
                goodYearFactor > 0 ? "good" : "bad", goodYearFactor));
        wealthService.distributeCapital(date, goodYearFactor);
    }
}
