package com.meryt.demographics.service;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;

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
                                             int minWifeAge) {
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
            maxBirthDate = person.getDeathDate().minusYears(minWifeAge);
            minAgeAtDeath = minWifeAge;
        } else {
            maxBirthDate = person.getBirthDate().plusYears(3);
            minAgeAtDeath = minHusbandAge;
        }

        Gender gender = person.isMale() ? Gender.FEMALE : Gender.MALE;
        return personRepository.findPotentialSpouses(gender, searchDate, minBirthDate, maxBirthDate,
                minAgeAtDeath);
    }

}
