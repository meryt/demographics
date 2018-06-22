package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.repository.TitleRepository;
import com.meryt.demographics.time.LocalDateComparator;

@Slf4j
@Service
public class TitleService {

    private final TitleRepository titleRepository;
    private final PersonService personService;
    private final InheritanceService inheritanceService;

    public TitleService(@Autowired @NonNull TitleRepository titleRepository,
                        @Autowired @NonNull PersonService personService,
                        @Autowired @NonNull InheritanceService inheritanceService) {
        this.titleRepository = titleRepository;
        this.personService = personService;
        this.inheritanceService = inheritanceService;
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
                // On the first round, set the inheritance date to the death date of the last holder, plus one day.
                // This is because the person himself passes the "is-living" test on his own death date, so the logic
                // for finding the heir of a grandparent would find the person again.
                inheritanceDate = currentHolder.getDeathDate().plusDays(1);
            }
            log.info(String.format("Looking for heir to %s, %s, died %s", currentHolder.getName(), title.getName(),
                    currentHolder.getDeathDate()));
            List<Person> nextHolders = inheritanceService.findPotentialHeirsForPerson(currentHolder,
                    inheritanceDate, title.getInheritance(), true);
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

        return (inheritanceDate == null) ? null : Pair.of(inheritanceDate.minusDays(1), new ArrayList<>());
    }

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
            remainingHeirs = inheritanceService.findPotentialHeirsForPerson(forPerson, currentDate,
                    title.getInheritance(), true);
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
    public Person updateTitleHeirs(@NonNull Title title) {
        Person currentHolder = getLatestHolder(title);
        if (currentHolder == null) {
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

            // The person may not have been born when the current title-holder died (e.g. he inherited via
            // his mother), so in that case he inherited at birth.
            LocalDate dateObtained = LocalDateComparator.max(nextHolders.getFirst(), nextHolder.getBirthDate());
            if (!nextHolder.isLiving(dateObtained)) {
                log.info(String.format("Unable to add title for %s; %s was not living on %s", nextHolder.getName(),
                        (nextHolder.isMale() ? "he" : "she"), dateObtained));
                return null;
            }
            log.info(String.format("Adding title for %s starting %s", nextHolder, dateObtained));
            nextHolder.addOrUpdateTitle(title, dateObtained, null);
            if (Strings.isNullOrEmpty(nextHolder.getLastName())) {
                String lastNameFromTitle = title.getName().replaceAll("^[^ ]+ ", "");
                nextHolder.setLastName(lastNameFromTitle);
            }
            personService.save(nextHolder);
            return nextHolder;
        } else if (allHoldersHaveFinishedGeneration(title) && nextHolders.getSecond().isEmpty()) {
            log.info(String.format("The title of %s has gone extinct.", title.getName()));
            title.setExtinct(true);
            save(title);
        }
        return null;
    }

    /**
     * Check to see whether any holder has not finished generation. This is needed to handle the case where the
     * son and heir died without issue, but the previous holder (father) had not finished generation as of the check.
     *
     * @param title
     * @return
     */
    private boolean allHoldersHaveFinishedGeneration(@NonNull Title title) {
        return title.getTitleHolders().stream()
                .allMatch(th -> th.getPerson().isFinishedGeneration());
    }
}
