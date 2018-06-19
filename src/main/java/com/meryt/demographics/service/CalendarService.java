package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.repository.CheckDateRepository;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.response.calendar.MarriageEvent;

@Slf4j
@Service
public class CalendarService {

    private final CheckDateRepository checkDateRepository;
    private final PersonService personService;
    private final FamilyGenerator familyGenerator;
    private final HouseholdService householdService;
    private final FamilyService familyService;

    public CalendarService(@Autowired @NonNull CheckDateRepository checkDateRepository,
                           @Autowired @NonNull PersonService personService,
                           @Autowired @NonNull FamilyGenerator familyGenerator,
                           @Autowired @NonNull HouseholdService householdService,
                           @Autowired @NonNull FamilyService familyService) {
        this.checkDateRepository = checkDateRepository;
        this.personService = personService;
        this.familyGenerator = familyGenerator;
        this.householdService = householdService;
        this.familyService = familyService;
    }

    @Nullable
    public LocalDate getCurrentDate() {
        return checkDateRepository.getCurrentDate();
    }

    public void setCurrentDate(@NonNull LocalDate date) {
        checkDateRepository.setCurrentDate(date);
    }

    /**
     * Perform checks on current date up to given date.
     *
     * @param toDate the end date (inclusive)
     * @return a list of things that happened on this day
     */
    public Map<LocalDate, List<CalendarDayEvent>> advanceToDay(@NonNull LocalDate toDate,
                                                               @NonNull RandomFamilyParameters familyParameters) {
        LocalDate currentDate = getCurrentDate();
        if (currentDate == null) {
            throw new IllegalStateException("Current date is null");
        }

        Map<LocalDate, List<CalendarDayEvent>> results = new TreeMap<>();
        for (LocalDate date = currentDate.plusDays(1); !date.isAfter(toDate); date = date.plusDays(1)) {
            Map<LocalDate, List<CalendarDayEvent>> marriageEvents = generateMarriagesToDate(date, familyParameters);
            results = mergeMaps(results, marriageEvents);
            /*
            Map<LocalDate, List<CalendarDayEvent>> maternityEvents = advanceMaternitiesToDay(date, familyParameters);
            results = mergeMaps(results, maternityEvents);
            */
        }

        return results;
    }

    private Map<LocalDate, List<CalendarDayEvent>> generateMarriagesToDate(@NonNull LocalDate date,
                                                                           @NonNull RandomFamilyParameters familyParameters) {
        List<Person> unmarriedPeople = personService.findUnmarriedMen(date, familyParameters.getMinHusbandAgeOrDefault(),
                familyParameters.getMaxHusbandAgeOrDefault(), true);
        Map<LocalDate, List<CalendarDayEvent>> results = new TreeMap<>();
        List<CalendarDayEvent> todaysResults = new ArrayList<>();

        for (Person man : unmarriedPeople) {
            if (!man.getFamilies().isEmpty()) {
                log.info(String.format("%d %s is %s and his last wife died on %s", man.getId(), man.getName(),
                        man.getAge(date), man.getFamilies().get(man.getFamilies().size() - 1).getWife().getDeathDate()));
            } else {
                log.info(String.format("%d %s is %s and has never been married", man.getId(), man.getName(),
                        man.getAge(date)));
            }

            // TODO we should consider how likely we want it to be for someone to "import" a bride from outside of the DB
            Family family = familyGenerator.attemptToFindSpouse(date, date, man, familyParameters);
            if (family != null) {
                familyService.save(family);
                todaysResults.add(new MarriageEvent(date, family));
                logMarriage(family, date);
                family = familyService.createAndSaveMarriage(family.getHusband(), family.getWife(), family.getWeddingDate());
            }
        }
        if (!todaysResults.isEmpty()) {
            results.put(date, todaysResults);
        }

        return results;
    }

    private Map<LocalDate, List<CalendarDayEvent>> advanceMaternitiesToDay(@NonNull LocalDate date,
                                                                           @NonNull RandomFamilyParameters familyParameters) {
        List<Person> women = personService.findWomenWithPendingMaternities(date);
        log.info(women.size() + " women need to be checked");
        return new TreeMap<>();
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

        log.info(String.format("%d %s, age %d, %s of %s married %d %s, age %d, %s of %s",
                husband.getId(), husband.getName(), husband.getAgeInYears(date), husbandOccupationName, husbandLocation,
                wife.getId(), wife.getName(), wife.getAgeInYears(date), wifeOccupationName, wifeLocation));
    }
}
