package com.meryt.demographics.service;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.generator.family.MatchMaker;
import com.meryt.demographics.repository.PersonRepository;

@Service
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(@Autowired @NonNull PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public Person save(@NonNull Person person) {
        return personRepository.save(person);
    }

    /**
     * Finds a person by ID or returns null if none found
     */
    @Nullable
    public Person load(long personId) {
        return personRepository.findById(personId).orElse(null);
    }

    public List<Person> findPotentialSpouses(@NonNull Person person,
                                             @Nullable LocalDate onDate,
                                             int minHusbandAge,
                                             int minWifeAge,
                                             int maxWifeAge) {
        LocalDate searchDate = MatchMaker.getDateToStartMarriageSearch(person, minHusbandAge, minWifeAge);
        if (onDate != null && onDate.isAfter(searchDate)) {
            searchDate = onDate;
        }

        LocalDate minBirthDate = null;
        LocalDate maxBirthDate;
        Integer minAgeAtDeath;
        if (person.isMale()) {
            // A woman should be no more than three years older than man
            minBirthDate = person.getBirthDate().minusYears(3);
            // At death she should be at least minWifeAge, otherwise there is no use considering her as she died a child.
            maxBirthDate = person.getDeathDate().minusYears(minWifeAge);
            // A woman should be no more than maxWifeAge years old at the start of the search, so she should be born
            // no more than maxAge years ago (searchDate - maxAge)
            if (searchDate.minusYears(maxWifeAge).isAfter(minBirthDate)){
                minBirthDate = searchDate.minusYears(maxWifeAge);
            }

            minAgeAtDeath = minWifeAge;
        } else {
            maxBirthDate = person.getBirthDate().plusYears(3);
            minAgeAtDeath = minHusbandAge;
        }

        final LocalDate filterSearchDate = searchDate;

        Gender gender = person.isMale() ? Gender.FEMALE : Gender.MALE;
        return personRepository.findPotentialSpouses(gender, searchDate, minBirthDate, maxBirthDate,
                minAgeAtDeath).stream()
                // Filter out women who were married more than once, or widows with children
                .filter(p -> p.getFamilies().isEmpty()
                        || (p.isFemale()
                            && p.getFamilies().size() == 1
                            && p.getLivingChildren(filterSearchDate).isEmpty()
                            && p.getFamilies().iterator().next().getHusband().getDeathDate().isBefore(filterSearchDate)))
                .collect(Collectors.toList());
    }

}
