package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;
import com.meryt.demographics.domain.title.Peerage;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.repository.TitleRepository;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.request.RandomTitleParameters;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.response.calendar.PropertyTransferEvent;
import com.meryt.demographics.response.calendar.TitleAbeyanceEvent;
import com.meryt.demographics.response.calendar.TitleExtinctionEvent;
import com.meryt.demographics.response.calendar.TitleInheritanceEvent;
import com.meryt.demographics.time.LocalDateComparator;

@Slf4j
@Service
public class TitleService {

    private final TitleRepository titleRepository;
    private final PersonService personService;
    private final HeirService heirService;
    private final DwellingPlaceService dwellingPlaceService;
    private final AncestryService ancestryService;
    private final FertilityService fertilityService;
    private final FamilyGenerator familyGenerator;
    private final FamilyService familyService;

    public TitleService(@Autowired @NonNull TitleRepository titleRepository,
                        @Autowired @NonNull PersonService personService,
                        @Autowired @NonNull HeirService heirService,
                        @Autowired @NonNull DwellingPlaceService dwellingPlaceService,
                        @Autowired @NonNull AncestryService ancestryService,
                        @Autowired @NonNull FertilityService fertilityService,
                        @Autowired @NonNull FamilyGenerator familyGenerator,
                        @Autowired @NonNull FamilyService familyService) {
        this.titleRepository = titleRepository;
        this.personService = personService;
        this.heirService = heirService;
        this.dwellingPlaceService = dwellingPlaceService;
        this.ancestryService = ancestryService;
        this.fertilityService = fertilityService;
        this.familyGenerator = familyGenerator;
        this.familyService = familyService;
    }

    @Nullable
    public Title load(long titleId) {
        return titleRepository.findById(titleId).orElse(null);
    }

    @NonNull
    public Title save(@NonNull Title title) {
        return titleRepository.save(title);
    }

    @NonNull
    Iterable<Title> findAll() {
        return titleRepository.findAll();
    }

    @NonNull
    public List<Title> findAllOrderByName() {
        return titleRepository.findAllByOrderByNameAsc();
    }

    @Nullable
    public Person getLatestHolder(@NonNull Title title) {
        Optional<PersonTitlePeriod> latestHolder = title.getTitleHolders().stream()
                .max(Comparator.comparing(PersonTitlePeriod::getFromDate));
        return latestHolder.map(PersonTitlePeriod::getPerson).orElse(null);
    }

    /**
     * Looks for the heir or potential heirs for a title given the death date of the latest title holder. If a single
     * heir is found, the person is returned along with the date at which he accedes to the title. If multiple
     * potential heirs are found, the date shows where the calculation had to stop due to incomplete data. If the
     * result is empty, it means the title has become extinct.
     *
     * @param title the title whose current latest heir should be the start of the search
     * @return a person or persons who should or may inherit, or an empty list if there are no possible heirs, or null
     * if there has never been a holder of this title
     */
    @Nullable
    public Pair<LocalDate, List<Person>> getTitleHeirs(@NonNull Title title) {

        List<PersonTitlePeriod> holdersMostRecentToOldest = title.getTitleHolders().stream()
                .sorted(Comparator.comparing(PersonTitlePeriod::getFromDate).reversed())
                .collect(Collectors.toList());

        if (holdersMostRecentToOldest.isEmpty()) {
            return null;
        }

        LocalDate inheritanceDate = null;
        for (PersonTitlePeriod period : holdersMostRecentToOldest) {
            Person currentHolder = period.getPerson();
            if (inheritanceDate == null) {
                if (currentHolder.getDeathDate() == null) {
                    // Should never happen
                    return null;
                }
                // On the first round, set the inheritance date to the death date of the last holder, plus one day.
                // This is because the person himself passes the "is-living" test on his own death date, so the logic
                // for finding the heir of a grandparent would find the person again.
                inheritanceDate = currentHolder.getDeathDate().plusDays(1);
            }
            log.info(String.format("Looking for heir to %s, %s, died %s", currentHolder.getName(), title.getName(),
                    currentHolder.getDeathDate()));
            List<Person> nextHolders = heirService.findPotentialHeirsForPerson(currentHolder,
                    inheritanceDate, title.getInheritance(), true, title.singleFemaleMayInherit());
            log.info(nextHolders.size() + " possible heir(s) found as of " + inheritanceDate);
            if (nextHolders.size() == 1) {
                // Stop once we have gone far back enough to find a single heir
                return Pair.of(inheritanceDate.minusDays(1), nextHolders);
            } else if (!nextHolders.isEmpty()) {
                // There are multiple potential heirs, meaning title is in abeyance. Go forward until we must stop due
                // to hitting a person who has not finished generation, or until a single heir is found.
                return findFutureHeirForTitleInAbeyance(title, currentHolder, inheritanceDate, nextHolders);
            } else if (!currentHolder.isFinishedGeneration()) {
                log.info(String.format("No heirs found for %s but %s has not yet finished generation",
                        currentHolder.getName(), (currentHolder.isMale() ? "he" : "she")));
                break;
            }
        }

        return Pair.of(inheritanceDate.minusDays(1), new ArrayList<>());
    }

    @Nullable
    private Pair<LocalDate, List<Person>> findFutureHeirForTitleInAbeyance(@NonNull Title title,
                                                                           @NonNull Person forPerson,
                                                                           @NonNull LocalDate startDate,
                                                                           @NonNull List<Person> remainingHeirs) {
        LocalDate currentDate = startDate;

        do {
            if (remainingHeirs.size() == 1) {
                log.info(String.format("Found single heir %s as of %s", remainingHeirs.get(0).getName(), currentDate));
                return Pair.of(currentDate.minusDays(1), remainingHeirs);
            }
            if (!remainingHeirs.get(0).isFinishedGeneration()) {
                log.info(String.format("Stopped looking for heir; %s has not finished generation as of %s",
                        remainingHeirs.get(0).getName(), currentDate));
                return Pair.of(currentDate.minusDays(1), remainingHeirs);
            }

            Person nextPotentialHeir = remainingHeirs.remove(0);
            currentDate = nextPotentialHeir.getDeathDate().plusDays(1);
            remainingHeirs = heirService.findPotentialHeirsForPerson(forPerson, currentDate,
                    title.getInheritance(), true, title.singleFemaleMayInherit());
        } while (!remainingHeirs.isEmpty());

        // If we got here we ran out of heirs
        log.info(String.format("Title %s has gone extinct as of %s", title.getName(), currentDate));
        return null;
    }

    /**
     * Finds the next heir to a title, if possible, and sets him as title holder if so. The person is returned if a
     * new heir is found, otherwise null is returned.
     */
    @Nullable
    public Person updateTitleHeirs(@NonNull Title title, @Nullable LocalDate untilDate) {
        Person currentHolder = getLatestHolder(title);
        if (currentHolder == null) {
            return null;
        }
        if (untilDate != null &&
                (currentHolder.isLiving(untilDate) || currentHolder.getBirthDate().isAfter(untilDate))) {
            return null;
        }
        Pair<LocalDate, List<Person>> nextHolders = getTitleHeirs(title);
        if (nextHolders == null) {
            return null;
        }
        if (nextHolders.getSecond().size() == 1) {
            Person nextHolder = nextHolders.getSecond().get(0);
            if (!currentHolder.isFinishedGeneration() &&
                    (nextHolder.isFemale() ||
                            (!title.getInheritance().isMalesOnly() && !nextHolder.getFather().equals(currentHolder)))) {
                // If the current holder is not finished, we can still proceed, but only if the heir is a
                // son of the current holder, or it's a male-only inheritance style and the heir is a son
                // (since grandchildren by an elder son come before younger sons).
                // Otherwise we have to wait to see if he has a son before we decide that a daughter's son
                // may inherit.
                // So skip this person if the heir is a female or not his son (but instead his grandson or
                // brother or whatever).
                log.info(String.format("%s is not an elder son of %s, skipping for now",
                        nextHolder.getName(), currentHolder.getName()));
                return null;
            }

            Pair<LocalDate, List<CalendarDayEvent>> dateAndEvents = addTitleToPerson(title, nextHolder,
                    nextHolders.getFirst());
            return dateAndEvents == null ? null : nextHolder;
        } else if (allHoldersHaveFinishedGeneration(title) && nextHolders.getSecond().size() > 1) {
            setAbeyanceCheckDate(title, nextHolders.getSecond(), null);
        } else if (allHoldersHaveFinishedGeneration(title) && nextHolders.getSecond().isEmpty()) {
            log.info(String.format("The title of %s has gone extinct.", title.getName()));
            title.setExtinct(true);
            save(title);
        }
        return null;
    }

    private Pair<LocalDate, List<CalendarDayEvent>> addTitleToPerson(@NonNull Title title,
                                                                     @NonNull Person person,
                                                                     @NonNull LocalDate dateObtained) {
        List<CalendarDayEvent> results = new ArrayList<>();
        // The person may not have been born when the current title-holder died (e.g. he inherited via
        // his mother), so in that case he inherited at birth.
        dateObtained = LocalDateComparator.max(dateObtained, person.getBirthDate());
        if (!person.isLiving(dateObtained)) {
            log.info(String.format("Unable to add title for %s; %s was not living on %s", person.getName(),
                    (person.isMale() ? "he" : "she"), dateObtained));
            return null;
        }
        log.info(String.format("Adding title for %s starting %s", person, dateObtained));
        title.setExtinct(false);
        title.setNextAbeyanceCheckDate(null);
        save(title);

        person.addOrUpdateTitle(title, dateObtained, null);
        if (Strings.isNullOrEmpty(person.getLastName())) {
            String lastNameFromTitle = title.getName().replaceAll("^[^ ]+ ", "");
            personService.updatePersonLastName(person, lastNameFromTitle, true, true);
        }
        personService.save(person);

        results.add(new TitleInheritanceEvent(dateObtained, title, person));

        for (DwellingPlace place : title.getEntailedProperties()) {
            PropertyTransferEvent event = dwellingPlaceService.transferDwellingPlaceToPerson(place, person, dateObtained, false,
                    DwellingPlaceOwnerPeriod.Reason.inheritedAsTitleHolderMessage(title));
            if (event != null) {
                results.add(event);
            }
        }

        if (person.isMarried(dateObtained)) {
            Person wife = person.isFemale() ? person : person.getSpouse(dateObtained);
            Person husband = person.isMale() ? person : person.getSpouse(dateObtained);
            Maternity mat = wife == null ? null : wife.getMaternity();
            // If they were a lower class couple before the new title was obtained, maybe need to reactivate the
            // maternity record.
            if (mat != null && (mat.getLastCheckDate() == null ||
                    mat.getLastCheckDate().isBefore(dateObtained) ||
                    mat.getLastCheckDate().equals(dateObtained))) {
                if (mat.getFatherId() == null || !mat.isHavingRelations()) {
                    mat.setFather(husband);
                    mat.setHavingRelations(true);
                    personService.save(wife);
                }
            }
        }

        return Pair.of(dateObtained, results);
    }

    /**
     * Find all titles such that the abeyance check date is non null and is before or equal to the given date
     *
     * @param date a date on which to check
     * @return a list of titles, possibly empty
     */
    @NonNull
    List<Title> findTitlesForAbeyanceCheck(@NonNull LocalDate date) {
        return titleRepository.findAllByNextAbeyanceCheckDateIsLessThanEqualAndExtinctIsFalse(date);
    }

    @NonNull
    List<CalendarDayEvent> processDeadPersonsTitles(@NonNull Person person) {
        LocalDate date = person.getDeathDate();
        if (date == null) {
            throw new IllegalArgumentException(String.format("%d %s has no death date", person.getId(), person.getName()));
        }
        List<CalendarDayEvent> results = new ArrayList<>();
        List<Person> singleTitleHeirs = new ArrayList<>();
        for (PersonTitlePeriod period : person.getTitles(date.minusDays(1))) {

            if (!person.getDeathDate().equals(period.getToDate())) {
                period.setToDate(person.getDeathDate());
                personService.save(person);
            }

            Title title = period.getTitle();
            if (title.getTitleHolders().stream().anyMatch(p -> p.getFromDate().isAfter(date) || p.getFromDate().equals(date))) {
                // The subsequent title holders have already been decided, so skip the title.
                continue;
            }
            LocalDate heirMayBeBorn = null;
            if (person.isMale()) {
                Person wife = person.getSpouse(date);
                if (wife != null) {
                    boolean allowMaternalDeath = wife.getTitles().isEmpty() && wife.getOwnedDwellingPlaces().isEmpty();
                    fertilityService.cycleToDate(wife, date, allowMaternalDeath);

                    if ((wife.isPregnant(date) || wife.mayGiveBirthBy(date)) &&
                        wife.getMaternity().getMiscarriageDate() == null) {
                        heirMayBeBorn = wife.getMaternity().getDueDate();
                    }
                }
            }
            List<CalendarDayEvent> result = checkForSingleTitleHeir(title, date, heirMayBeBorn);
            singleTitleHeirs.addAll(result.stream()
                    .filter(TitleInheritanceEvent.class::isInstance)
                    .map(TitleInheritanceEvent.class::cast)
                    .map(TitleInheritanceEvent::getPersonRecord)
                    .collect(Collectors.toList()));
            results.addAll(result);
        }

        if (!singleTitleHeirs.isEmpty()) {
            Double capital = person.getCapital(date);
            if (capital != null) {
                double portionForChildren = capital / 4;
                double remainingCapital = capital - portionForChildren;
                int numChildren = person.getLivingChildren(date).size();
                if (numChildren == 0) {
                    remainingCapital = capital;
                    portionForChildren = 0;
                }

                double portionForHeir = remainingCapital / singleTitleHeirs.size();
                for (Person heir : singleTitleHeirs) {
                    heir.addCapital(portionForHeir, date,
                            ancestryService.getCapitalReasonMessageForHeirWithRelationship(heir, person));
                    log.info(String.format("%.2f is inherited by %s on %s",
                            portionForHeir, ancestryService.getLogMessageForHeirWithRelationship(heir, person), date));
                    personService.save(heir);
                }
                if (portionForChildren > 0) {
                    double portionForChild = portionForChildren / numChildren;
                    for (Person child : person.getLivingChildren(date)) {
                        child.addCapital(portionForChild, date,
                                ancestryService.getCapitalReasonMessageForHeirWithRelationship(child, person));
                        log.info(String.format("%.2f is inherited by %s on %s", portionForHeir,
                                ancestryService.getLogMessageForHeirWithRelationship(child, person), date));
                        personService.save(child);
                    }
                }

                PersonCapitalPeriod period = person.getCapitalPeriod(date);
                if (period != null) {
                    period.setToDate(date);
                    personService.save(person);
                }
            }
        }

        return results;
    }

    /**
     * Attempt to find a single heir for a title on the given date. If no heir exists, the title is set to extinct.
     * If multiple potential heirs exist, the next abeyance check date is set. If a single heir exists and there is no
     * chance for a more direct heir to be born, the single heir is given the title.
     *
     * @param title the title for whom the latest holder has just died
     * @param date the death date of the latest holder
     * @param heirMayBeBornOn the date a heir may be born, if the title-holder's wife is pregnant at the time of his
     *                        death
     * @return a list of events describing any changes made
     */
    public List<CalendarDayEvent> checkForSingleTitleHeir(@NonNull Title title,
                                                          @NonNull LocalDate date,
                                                          @Nullable LocalDate heirMayBeBornOn) {
        Pair<LocalDate, List<Person>> heirs = getTitleHeirs(title);
        Person latestHolder = getLatestHolder(title);
        List<CalendarDayEvent> results = new ArrayList<>();
        if (heirs == null || heirs.getSecond().isEmpty()) {
            if (heirMayBeBornOn != null) {
                title.setNextAbeyanceCheckDate(heirMayBeBornOn);
                save(title);
            } else if (latestHolder != null && !latestHolder.isFinishedGeneration()) {
                title.setNextAbeyanceCheckDate(latestHolder.getDeathDate());
                save(title);
            } else {
                title.setExtinct(true);
                title.setNextAbeyanceCheckDate(null);
                save(title);
                results.add(new TitleExtinctionEvent(date, title));
            }
        } else if (heirs.getSecond().size() > 1) {
            results.add(new TitleAbeyanceEvent(date, title, heirs.getSecond()));
            setAbeyanceCheckDate(title, heirs.getSecond(), heirMayBeBornOn);
            reenableMaternitiesForPotentialHeirs(heirs.getSecond(), date);
        } else {
            Person heir = heirs.getSecond().get(0);
            if (heirMayBeBornOn != null && (!heir.isMale() || (latestHolder != null && !latestHolder.getChildren().contains(heir)))) {
                // If an heir may yet be born, and this heir is not a son of the dead person, we should wait till
                // the potential heir is born.
                title.setNextAbeyanceCheckDate(heirMayBeBornOn);
                save(title);
            } else {
                LocalDate dateOfTitle = heirs.getFirst();
                Pair<LocalDate, List<CalendarDayEvent>> dateObtainedAndEvents = addTitleToPerson(title, heir, dateOfTitle);
                if (dateObtainedAndEvents != null) {
                    results.addAll(dateObtainedAndEvents.getSecond());
                }
            }
        }
        return results;
    }

    private void setAbeyanceCheckDate(@NonNull Title title,
                                      @NonNull List<Person> possibleHeirs,
                                      @Nullable LocalDate heirMayBeBornOn) {
        Person nextToLastHeir = possibleHeirs.get(possibleHeirs.size() - 2);
        LocalDate nextCheck = heirMayBeBornOn == null
                ? nextToLastHeir.getDeathDate()
                : LocalDateComparator.min(heirMayBeBornOn, nextToLastHeir.getDeathDate());
        title.setNextAbeyanceCheckDate(nextCheck);
        save(title);
    }

    /**
     * When a title goes extinct, and there were properties entailed to the title, we must find new owners for them.
     *
     * Maybe a grandson will be allowed to inherit. Or maybe a random nobleman will be chosen.
     */
    @Nullable
    Person findNewOwnerForEntailedProperties(@NonNull Title extinctTitle,
                                             @NonNull Person latestHolder,
                                             @NonNull LocalDate date) {
        Person heir = null;
        if (PercentDie.roll() < 0.5) {
            // By definition the last holder has no male heirs. But perhaps a grandson can inherit the property.
            List<Person> grandsons = latestHolder.getChildren().stream()
                    .flatMap(p -> p.getChildren().stream())
                    .filter(p -> p.isLiving(date) && p.isMale())
                    .sorted(Comparator.comparing(Person::getBirthDate))
                    .collect(Collectors.toList());
            if (!grandsons.isEmpty()) {
                heir = grandsons.get(0);
                log.info(String.format("Grandson %d %s will inherit entailed property belonging to %d %s",
                        heir.getId(), heir.getName(), latestHolder.getId(), latestHolder.getName()));
            }
        }
        if (heir == null) {
            // Find a person with a title of the same or a greater social class.
            List<Person> personsWithTitles = findAllOrderByName().stream()
                    .filter(t -> !t.isExtinct() && t.getNextAbeyanceCheckDate() == null
                            && t.getSocialClass().getRank() >= extinctTitle.getSocialClass().getRank())
                    .map(t -> t.getHolder(date))
                    .filter(Objects::nonNull)
                    .filter(p -> p.getResidence(date) == null)
                    .collect(Collectors.toList());
            if (!personsWithTitles.isEmpty()) {
                Collections.shuffle(personsWithTitles);
                heir = personsWithTitles.get(0);
                log.info(String.format("%d %s, %s, will inherit entailed property belonging to %d %s",
                        heir.getId(), heir.getName(),
                        heir.getTitles(date).stream()
                            .map(PersonTitlePeriod::getTitle)
                            .map(Title::getName)
                            .collect(Collectors.joining(", ")),
                        latestHolder.getId(), latestHolder.getName()));
            }
        }

        return heir;
    }

    Title checkNewTitleCreation(@NonNull RandomTitleParameters titleParameters,
                                @NonNull LocalDate onDate) {
        if (titleParameters.shouldCreateNewTitleOnDay()) {
            return createRandomTitle(titleParameters, onDate);
        } else {
            return null;
        }
    }

    public Title createRandomTitle(@NonNull RandomTitleParameters titleParameters,
                                   @NonNull LocalDate onDate) {

        // A new title was created today. First generate the title.
        Title title = randomTitle(titleParameters);
        final Peerage peerage = title.getPeerage();
        final SocialClass socialClass = title.getSocialClass();

        List<String> namesList = title.getPeerage() == Peerage.SCOTLAND
                ? titleParameters.getScottishNames()
                : titleParameters.getNames();
        String randomName = randomTitleName(namesList, title.getSocialClass());

        // We might have created a new title with a rank higher than an existing one with the same name. If so, the
        // current holder will get the new title instead of the random person above.
        Title existing = findByNameEndsWith(randomName);
        Person person = null;
        if (existing != null) {
            Person newPerson = title.getHolder(onDate);
            if (newPerson != null) {
                person = newPerson;
            }
        }

        if (person == null) {
            // Find a person for the title
            List<SocialClass> validClasses = SocialClass.listFromClassToClass(SocialClass.GENTLEMAN, SocialClass.DUKE);
            List<Person> possiblePeople = personService.findBySocialClassAndGenderAndIsLiving(validClasses, Gender.MALE,
                    onDate).stream()
                    // Don't give titles to kids
                    .filter(p -> p.getAgeInYears(onDate) >= 20)
                    // Don't give someone a title when his father is still alive
                    .filter(p -> p.getFather() == null || !p.getFather().isLiving(onDate))
                    // Make sure he either lives nowhere or lives in the appropriate kingdom
                    .filter(p -> p.getResidence(onDate) == null
                            || (p.livesInScotland(onDate) && peerage == Peerage.SCOTLAND)
                            || (!p.livesInScotland(onDate) && peerage == Peerage.ENGLAND))
                    // Remove anyone who already has a title higher than the given title... no lesser titles are randomly
                    // given to those already with a title.
                    .filter(p -> p.getTitles(onDate).stream()
                            .noneMatch(t -> t.getTitle().getSocialClass().getRank() > socialClass.getRank()))
                    .collect(Collectors.toList());
            if (possiblePeople.isEmpty()) {
                // Generate a person
                RandomFamilyParameters randomFamilyParameters = titleParameters.getFamilyParametersOrDefault();
                randomFamilyParameters.setReferenceDate(onDate);
                randomFamilyParameters.setMinSocialClass(title.getSocialClass());
                randomFamilyParameters.setMaxSocialClass(title.getSocialClass());
                Family family = familyGenerator.generate(randomFamilyParameters);
                if (family == null) {
                    return null;
                }
                familyService.save(family);
                person = family.getHusband();
                person.setFounder(true);
            } else {
                person = possiblePeople.get(BetweenDie.roll(0, possiblePeople.size() - 1));
            }
        }

        switch (title.getSocialClass()) {
            case BARONET:
                if (onDate.getYear() < 1660) {
                    if (title.getPeerage() == Peerage.SCOTLAND) {
                        title.setName("Baron " + randomName);
                    } else if (title.getPeerage() == Peerage.ENGLAND) {
                        title.setName("Lord " + randomName);
                    }
                } else {
                    title.setName("Baronet " + randomName);
                }
                break;
            case BARON:
                if (title.getPeerage() == Peerage.SCOTLAND) {
                    title.setName("Lord of " + randomName);
                } else if (title.getPeerage() == Peerage.ENGLAND) {
                    title.setName("Baron " + randomName);
                }
                break;
            case VISCOUNT:
                if (title.getPeerage() == Peerage.SCOTLAND) {
                    if (onDate.getYear() < 1622) {
                        title.setName("Lord of " + randomName);
                    } else {
                        title.setName("Viscount of " + randomName);
                    }
                } else if (title.getPeerage() == Peerage.ENGLAND) {
                    if (onDate.getYear() < 1440) {
                        title.setName("Baron " + randomName);
                    } else {
                        title.setName("Viscount " + randomName);
                    }
                }
                break;
            case EARL:
                title.setName("Earl of " + randomName);
                break;
            case MARQUESS:
                if (onDate.getYear() < 1450) {
                    title.setName("Earl of " + randomName);
                } else {
                    title.setName("Marquess of " + randomName);
                }
                break;
            case DUKE:
                if (onDate.getYear() < 1350) {
                    title.setName("Earl of " + randomName);
                } else {
                    title.setName("Duke of " + randomName);
                }
                break;
            default:
                return null;
        }
        title = save(title);
        person.addOrUpdateTitle(title, onDate, null);
        if (person.getLastName() == null) {
            if (title.getPeerage() == Peerage.ENGLAND) {
                person.setLastName(randomName);
            } else {
                person.setLastName("of " + randomName);
            }
        }
        person = personService.save(person);

        title.setInheritanceRoot(person);
        save(title);

        if (!titleParameters.isSkipCapitalGeneration()) {
            double currentCapital = person.getCapitalNullSafe(onDate);
            double minCapital = WealthGenerator.getRandomStartingCapital(person.getSocialClass(),
                    person.getOccupation(onDate) != null);
            if (currentCapital < minCapital) {
                person.addCapital(minCapital - currentCapital, onDate, PersonCapitalPeriod.Reason.grantedWithTitle(title));
                personService.save(person);
            }
        }
        return title;
    }

    /**
     * Gets the highest ranking non-extinct title such that its name ends with the given string
     * @param name the string used to match the ending
     * @return the highest matching title, or null if there were none
     */
    private Title findByNameEndsWith(@NonNull String name) {
        return Lists.newArrayList(findAll()).stream()
                .filter(t -> t.getName().endsWith(name) && !t.isExtinct())
                .max(Comparator.comparing(Title::getSocialClass).reversed())
                .orElse(null);
    }

    @Nullable
    private String randomTitleName(@Nullable List<String> names, @NonNull SocialClass forClass) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        List<String> newList = new ArrayList<>(names);
        Collections.shuffle(newList);
        while (!newList.isEmpty()) {
            String name = newList.remove(0);
            Title existingTitle = findByNameEndsWith(name);
            if (existingTitle == null) {
                return name;
            }
            if (existingTitle.getSocialClass().getRank() < forClass.getRank()) {
                return name;
            }
        }
        return null;
    }

    private Title randomTitle(@NonNull RandomTitleParameters titleParameters) {
        Peerage peerage = titleParameters.getRandomPeerage();
        TitleInheritanceStyle inheritanceStyle = TitleInheritanceStyle.randomFavoringMaleOnly();

        Title title = new Title();
        title.setPeerage(peerage);
        title.setInheritance(inheritanceStyle);
        title.setSocialClass(titleParameters.getRandomSocialClass());
        return title;
    }

    /**
     * Check to see whether any holder has not finished generation. This is needed to handle the case where the
     * son and heir died without issue, but the previous holder (father) had not finished generation as of the check.
     *
     * @param title the title to check
     * @return true if all holders in the past are definitely not going to have any children that might affect the
     * order of inheritance
     */
    private boolean allHoldersHaveFinishedGeneration(@NonNull Title title) {
        return title.getTitleHolders().stream()
                .allMatch(th -> th.getPerson().isFinishedGeneration());
    }

    /**
     * A lower class person not living in the parishes may have had their maternity checking disabled by setting the
     * father to null. If the person or the person's spouse may be an heir to a title, this check should be reenabled
     *
     * @param heirs list of people who may be heirs
     * @param onDate the date on which to do the check
     */
    private void reenableMaternitiesForPotentialHeirs(@NonNull List<Person> heirs, @NonNull LocalDate onDate) {
        for (Person heir : heirs) {
            Person wife = heir.isFemale() ? heir : heir.getSpouse(onDate);
            if (wife != null && wife.isLiving(onDate) && wife.isMarried(onDate)
                    && wife.getMaternity().getFather() == null) {
                log.info(String.format("Reenabling maternity checks for %d %s because %d %s is a potential heir",
                        wife.getId(), wife.getName(), heir.getId(), heir.getName()));
                wife.getMaternity().setFather(wife.getSpouse(onDate));
                personService.save(wife);
            }
        }
    }
}
