package com.meryt.demographics.service;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.RelatedPerson;
import com.meryt.demographics.generator.family.MatchMaker;
import com.meryt.demographics.repository.PersonRepository;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.time.LocalDateComparator;

@Service
public class PersonService {

    private final PersonRepository personRepository;

    private final AncestryService ancestryService;

    public PersonService(@Autowired @NonNull PersonRepository personRepository,
                         @Autowired @NonNull AncestryService ancestryService) {
        this.personRepository = personRepository;
        this.ancestryService = ancestryService;
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

    /**
     * Finds potential spouses for this person on this date
     *
     * @param person the person looking for a spouse
     * @param onDate the date he or she begins looking
     * @param minHusbandAge the minimum age for a man to get married
     * @param minWifeAge the minimum age for a woman to get married
     * @param maxWifeAge if the person looking for a spouse is a man, he will not accept a wife over this age
     * @return a list of persons, possibly empty
     */
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

    /**
     * Find potential spouses for a person based on family parameters.
     *
     * @param person the person looking for a spouse
     * @param onDate the date he or she looks (or begins looking if includeFutureSpouses is true)
     * @param includeFutureSpouses if true, results may include spouses who are not yet eligible but will be in the
     *                             person's lifespan
     * @param familyParameters extra parameters
     * @return a list of potential spouses, possibly empty
     */
    public List<RelatedPerson> findPotentialSpouses(@NonNull Person person,
                                             @Nullable LocalDate onDate,
                                             boolean includeFutureSpouses,
                                             @NonNull FamilyParameters familyParameters) {
        int minHusbandAge = familyParameters.getMinHusbandAgeOrDefault();
        int minWifeAge = familyParameters.getMinWifeAgeOrDefault();
        LocalDate searchDate = MatchMaker.getDateToStartMarriageSearch(person, minHusbandAge, minWifeAge);
        if (onDate != null && onDate.isAfter(searchDate)) {
            searchDate = onDate;
        } else if (onDate != null && onDate.isBefore(searchDate)) {
            // Can't start searching before the person is eligible.
            return Collections.emptyList();
        }

        Gender spouseGender;
        LocalDate minBirthDate = null;
        LocalDate maxBirthDate;
        if (person.isMale()) {
            spouseGender = Gender.FEMALE;
            // A woman should be no more than this many years older than the man
            minBirthDate = person.getBirthDate().minusYears(familyParameters.getMaxOlderWifeAgeDiffOrDefault());
            // She can't be born any later than, say, 35 years ago to be a suitable bride.
            LocalDate minBirthDateForAge = searchDate.minusYears(familyParameters.getMaxMarriageableWifeAgeOrDefault());
            minBirthDate = LocalDateComparator.max(Arrays.asList(minBirthDate, minBirthDateForAge));

            if (!includeFutureSpouses) {
                // She must be of a marriageable age today
                maxBirthDate = searchDate.minusYears(familyParameters.getMinWifeAgeOrDefault());
            } else {
                // She must live at least long enough to marry this man before he dies; there's no point in considering
                // her if she died a child
                maxBirthDate = person.getDeathDate().minusYears(familyParameters.getMinWifeAgeOrDefault());
            }
        } else {
            spouseGender = Gender.MALE;
            // A man should be no more than this many years younger than the woman
            maxBirthDate = person.getBirthDate().plusYears(familyParameters.getMaxOlderWifeAgeDiffOrDefault());
            if (!includeFutureSpouses) {
                LocalDate maxBirthDateForAge = searchDate.minusYears(familyParameters.getMinHusbandAgeOrDefault());
                maxBirthDate = LocalDateComparator.min(Arrays.asList(maxBirthDate, maxBirthDateForAge));
            } else {
                LocalDate maxBirthDateForAge = person.getDeathDate().minusYears(familyParameters.getMinHusbandAgeOrDefault());
                maxBirthDate = LocalDateComparator.min(Arrays.asList(maxBirthDate, maxBirthDateForAge));
            }
        }

        final LocalDate filterSearchDate = searchDate;
        int minDegreesSeparation = familyParameters.getMinDegreesSeparationOrDefault();

        return personRepository.findPotentialSpouses(spouseGender, searchDate, minBirthDate,
                maxBirthDate, null).stream()
                // Filter out women who were married more than once, or widows with children
                .filter(p -> p.getFamilies().isEmpty()
                        || (p.isFemale()
                        && p.getFamilies().size() == 1
                        && p.getLivingChildren(filterSearchDate).isEmpty()
                        && p.getFamilies().iterator().next().getHusband().getDeathDate().isBefore(filterSearchDate)))
                // Convert to a data structure that includes the relationship
                .map(spouse -> new RelatedPerson(spouse, ancestryService.calculateRelationship(spouse, person)))
                // Filter out anyone too closely related
                .filter(resp -> resp.getRelationship() == null ||
                        resp.getRelationship().getDegreeOfSeparation() > minDegreesSeparation)
                .collect(Collectors.toList());
    }

}
