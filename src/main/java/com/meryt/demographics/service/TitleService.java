package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Iterable<Title> findAll() {
        return titleRepository.findAll();
    }

    @NonNull
    public List<Title> findAllOrderByName() {
        return titleRepository.findAllByOrderByNameAsc();
    }

    /**
     * Finds the next heir to a title, if possible, and sets him as title holder if so. The person is returned if a
     * new heir is found, otherwise null is returned.
     */
    @Nullable
    public Person updateTitleHeirs(@NonNull Title title) {
        Optional<PersonTitlePeriod> latestHolder = title.getTitleHolders().stream()
                .max(Comparator.comparing(PersonTitlePeriod::getFromDate));
        if (latestHolder.isPresent()) {
            Person currentHolder = latestHolder.get().getPerson();
            log.info("Looking for heir to " + title.getName());
            List<Person> nextHolders = inheritanceService.findHeirForPerson(currentHolder,
                    currentHolder.getDeathDate(), title.getInheritance(), title.getInheritanceRoot());
            log.info(nextHolders.size() + " possible heir(s) found");
            if (nextHolders.size() == 1) {
                Person nextHolder = nextHolders.get(0);
                if (!currentHolder.isFinishedGeneration() &&
                        (nextHolder.isFemale() || !nextHolder.getFather().equals(currentHolder))) {
                    // If the current holder is not finished, we can still proceed, but only if the heir is a
                    // son of the current holder. Otherwise we have to wait to see if he has a son.
                    // So skip this person if the heir is a female or not his son (but instead his grandson or
                    // brother or whatever).
                    log.info(String.format("%s is not an elder son of %s, skipping for now",
                            nextHolder.getName(), currentHolder.getName()));
                    return null;
                }
                // The person may not have been born when the current title-holder died (e.g. he inherited via
                // his mother), so in that case he inherited at birth.
                LocalDate dateObtained = LocalDateComparator.max(currentHolder.getDeathDate(),
                        nextHolder.getBirthDate());
                log.info(String.format("Adding title for %s starting %s", nextHolder, dateObtained));
                nextHolder.addOrUpdateTitle(title, dateObtained, null);
                if (Strings.isNullOrEmpty(nextHolder.getLastName())) {
                    String lastNameFromTitle = title.getName().replaceAll("^[^ ]+ ", "");
                    nextHolder.setLastName(lastNameFromTitle);
                }
                personService.save(nextHolder);
                return nextHolder;
            }
        }
        return null;
    }
}
