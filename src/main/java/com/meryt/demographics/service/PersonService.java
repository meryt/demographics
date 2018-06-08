package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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

    @NonNull
    List<Person> loadFounders() {
        return personRepository.findByFounderTrueOrderByBirthDate();
    }

    /**
     * Gets all women who are living on or before this date and whose last check day is on or before this date
     * @param checkDate the date the check should be done. Will find women whose last check day was well before this
     *                  date, not only 1 day behind
     * @return a list of women, possibly empty
     */
    @NonNull
    List<Person> findWomenWithPendingMaternities(@NonNull LocalDate checkDate) {
        return personRepository.findWomenWithPendingMaternities(checkDate);
    }

    @NonNull
    List<Person> findUnmarriedMen(@NonNull LocalDate checkDate, int minHusbandAge, int maxHusbandAge) {
        return personRepository.findUnmarriedMen(checkDate, checkDate.minusYears(maxHusbandAge),
                checkDate.minusYears(minHusbandAge));
    }

    /**
     * Find potential spouses for a person based on family parameters.
     *
     * @param person the person looking for a spouse
     * @param onDate the date he or she looks (or begins looking if includeFutureSpouses is true)
     * @param includeFutureSpouses if true, results may include spouses who are not yet eligible but will be in the
     *                             person's lifespan
     * @param familyParameters extra parameters
     * @return a list of potential spouses, possibly empty, with their relationship to the person
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
                .filter(p -> isWidowWithChildren(p, filterSearchDate))
                // Convert to a data structure that includes the relationship
                .map(spouse -> new RelatedPerson(spouse, ancestryService.calculateRelationship(spouse, person)))
                // Filter out anyone too closely related
                .filter(resp -> resp.getRelationship() == null ||
                        resp.getRelationship().getDegreeOfSeparation() > minDegreesSeparation)
                .collect(Collectors.toList());
    }

    List<Person> loadUnfinishedPersons() {
        return personRepository.findUnfinishedPersons(null);
    }

    private static boolean isWidowWithChildren(@NonNull Person person, @NonNull LocalDate onDate) {
        if (!person.isFemale() || person.getFamilies().size() != 1) {
            // We want to filter out both
            return false;
        }
        if (person.getLivingChildren(onDate).isEmpty()) {
            return false;
        }
        Person husband = person.getFamilies().get(0).getHusband();
        return husband == null || !husband.isLiving(onDate);
    }

}
