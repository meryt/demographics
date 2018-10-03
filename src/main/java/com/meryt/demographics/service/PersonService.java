package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.AncestryRecord;
import com.meryt.demographics.domain.family.LeastCommonAncestorRelationship;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.person.RelatedPerson;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.Estate;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.generator.family.MatchMaker;
import com.meryt.demographics.repository.PersonRepository;
import com.meryt.demographics.request.RandomFamilyParameters;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.response.calendar.EmploymentEvent;
import com.meryt.demographics.time.LocalDateComparator;

@Slf4j
@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final AncestryService ancestryService;
    private final HouseholdService householdService;

    public PersonService(@Autowired @NonNull PersonRepository personRepository,
                         @Autowired @NonNull AncestryService ancestryService,
                         @Autowired @NonNull HouseholdService householdService) {
        this.personRepository = personRepository;
        this.ancestryService = ancestryService;
        this.householdService = householdService;
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
    Iterable<Person> loadAll(List<Long> ids) {
        return personRepository.findAllById(ids);
    }

    @NonNull
    List<Person> loadFounders() {
        return personRepository.findByFounderTrueOrderByBirthDate();
    }

    public void delete(@NonNull Person person) {
        personRepository.delete(person);
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
    List<Person> findBySocialClassAndGenderAndIsLiving(@NonNull SocialClass socialClass,
                                                       @NonNull Gender gender,
                                                       @NonNull LocalDate onDate) {
        return personRepository.findBySocialClassAndGenderAndIsLiving(socialClass, gender, onDate);
    }

    @NonNull
    List<Person> findByDeathDate(@NonNull LocalDate deathDate) {
        return personRepository.findByDeathDate(deathDate);
    }

    @NonNull
    List<Person> findUnmarriedPeople(@NonNull LocalDate checkDate,
                                     int minHusbandAge,
                                     int maxHusbandAge,
                                     int minWifeAge,
                                     int maxWifeAge,
                                     boolean residentsOnly,
                                     @Nullable Gender gender) {
        int minAge = Math.min(minHusbandAge, minWifeAge);
        int maxAge = Math.max(maxHusbandAge, maxWifeAge);
        List<Person> results = personRepository.findUnmarriedPeople(checkDate, checkDate.minusYears(maxAge),
                checkDate.minusYears(minAge), gender);
        return results.stream()
                .filter(p -> (p.isFemale()
                                && p.getAgeInYears(checkDate) >= minWifeAge && p.getAgeInYears(checkDate) <= maxWifeAge)
                          || (p.isMale()
                                && p.getAgeInYears(checkDate) >= minHusbandAge && p.getAgeInYears(checkDate) <= maxHusbandAge))
                .filter(p -> !residentsOnly || p.getResidence(checkDate) != null)
                .collect(Collectors.toList());
    }

    @NonNull
    List<Person> findUnmarriedUnemployedPeopleBySocialClassAndGenderAndAge(@NonNull List<SocialClass> socialClasses,
                                                                           @Nullable Gender gender,
                                                                           int minAgeInYears,
                                                                           int maxAgeInYears,
                                                                           @NonNull LocalDate onDate) {
        LocalDate minBirthDate = onDate.minusYears(maxAgeInYears);
        LocalDate maxBirthDate = onDate.minusYears(minAgeInYears);
        return personRepository.findUnmarriedUnemployedPeopleBySocialClassAndGenderAndAge(socialClasses,
                gender, minBirthDate, maxBirthDate);
    }

    /**
     * Find all descendants of a person.
     *
     * @param person the person whose descendants we will find
     * @param aliveOnDate if non-null, only descendants alive on this date will be returned
     * @return a list of Persons, possibly empty
     */
    @NonNull
    public List<Person> findDescendants(@NonNull Person person, @Nullable LocalDate aliveOnDate) {
        List<AncestryRecord> desc = ancestryService.getDescendants(person.getId());
        List<Long> descIds = desc.stream()
                .map(AncestryRecord::getDescendantId)
                .collect(Collectors.toList());
        List<Person> descendants = new ArrayList<>();
        loadAll(descIds).forEach(descendants::add);

        if (aliveOnDate != null) {
            return descendants.stream()
                    .filter(p -> p.isLiving(aliveOnDate))
                    .collect(Collectors.toList());
        } else {
            return descendants;
        }
    }

    @NonNull
    public List<Person> findLivingRelatives(@NonNull Person person,
                                            @NonNull LocalDate aliveOnDate,
                                            @Nullable Long maxDistance) {
        List<LeastCommonAncestorRelationship> relatives = ancestryService.getLivingRelatives(person.getId(),
                aliveOnDate, maxDistance);
        List<Long> relativeIds = relatives.stream()
                .map(LeastCommonAncestorRelationship::getSubject2)
                .collect(Collectors.toList());
        List<Person> relativePersons = new ArrayList<>();
        loadAll(relativeIds).forEach(relativePersons::add);
        return relativePersons;
    }

    @NonNull
    public List<Person> findRelatives(@NonNull Person person, @Nullable Long maxDistance) {
        List<LeastCommonAncestorRelationship> relatives = ancestryService.getRelatives(person.getId(), maxDistance);
        List<Long> relativeIds = relatives.stream()
                .map(LeastCommonAncestorRelationship::getSubject2)
                .collect(Collectors.toList());
        List<Person> relativePersons = new ArrayList<>();
        loadAll(relativeIds).forEach(relativePersons::add);
        return relativePersons;
    }

    /**
     * Finds all living persons related to the given person such that they share the lowest relationship distance
     * from the person for any living person.  (E.g. if the closest relationship among any living relative is at
     * distance 2, it will return all living persons at that distance: all living brothers & sisters,
     * living grandparents and grandchildren, etc.)
     *
     * @param person the person whose relatives we want to find
     * @param aliveOnDate the date to check for aliveness
     * @param maxDistance the deepest we are willing to go to search for ancestors
     * @return a list of persons, possibly empty, all of whom are at the same distance from the target
     */
    @NonNull
    List<Person> findClosestLivingRelatives(@NonNull Person person,
                                            @NonNull LocalDate aliveOnDate,
                                            @Nullable Long maxDistance) {
        List<LeastCommonAncestorRelationship> relatives = ancestryService.getLivingRelatives(person.getId(),
                aliveOnDate, maxDistance);
        Integer minDistance = relatives.stream()
                .map(LeastCommonAncestorRelationship::getDistance)
                .min(Integer::compareTo).orElse(null);
        if (minDistance == null) {
            return Collections.emptyList();
        }

        List<Long> relativeIds = relatives.stream()
                .filter(lca -> lca.getDistance() == minDistance)
                .map(LeastCommonAncestorRelationship::getSubject2)
                .collect(Collectors.toList());
        List<Person> relativePersons = new ArrayList<>();
        loadAll(relativeIds).forEach(relativePersons::add);
        return relativePersons;
    }

    /**
     * Perform some clean-up work upon a person's death, such as removing them from a household and distributing their
     * property to their heirs.
     * @param person the person who died
     */
    List<CalendarDayEvent> processDeath(@NonNull Person person) {
        List<CalendarDayEvent> results = new ArrayList<>();
        person.setFinishedGeneration(true);
        person = save(person);
        processDeadPersonsHousehold(person);
        CalendarDayEvent occupationEvent = processDeadPersonsOccupation(person);
        if (occupationEvent != null) {
            results.add(occupationEvent);
        }
        return results;
    }

    private void processDeadPersonsHousehold(@NonNull Person person) {
        LocalDate date = person.getDeathDate();
        if (date == null) {
            throw new IllegalArgumentException(String.format("Unable to process death: %d %s has a null death date",
                    person.getId(), person.getName()));
        }
        Household currentHousehold = person.getHousehold(date);
        if (currentHousehold != null) {
            householdService.endPersonResidence(currentHousehold, person, date);
        } else {
            // If the household is null (as it should be if their residence there ended upon their death) we should
            // still get their household so we can see whether is empty now. So get the household from 1 day ago.
            currentHousehold = person.getHousehold(date.minusDays(1));
        }
        if (currentHousehold == null) {
            return;
        }

        if (currentHousehold.getInhabitants(date).isEmpty()) {
            log.info("The former household is now empty. Removing from its location.");
            HouseholdLocationPeriod period = currentHousehold.getHouseholdLocationPeriod(date);
            if (period != null) {
                period.setToDate(date);
                householdService.save(period);
                log.info("Removed household from " + period.getDwellingPlace().getLocationString());
                if (period.getDwellingPlace().getHouseholds(date).isEmpty()) {
                    log.info("This dwelling place is now empty.");
                }
            }
        } else if (currentHousehold.getHead(date) == null) {
            log.info("Resetting household head");
            householdService.resetHeadAsOf(currentHousehold, date);
        }
    }

    @Nullable
    private CalendarDayEvent processDeadPersonsOccupation(@NonNull Person person) {
        LocalDate date = person.getDeathDate();
        Occupation occupation = person.getOccupation(date.minusDays(1));
        if (occupation == null) {
            return null;
        }

        List<Person> possibleJobHeirs = person.getLivingChildren(date).stream()
                .filter(p -> p.getGender() == person.getGender()
                        && p.getAgeInYears(date) >= 16
                        && p.getOccupation(date) == null
                        && p.getHousehold(date) != null)
                .sorted(Comparator.comparing(Person::getBirthDate))
                .collect(Collectors.toList());
        if (!possibleJobHeirs.isEmpty()) {
            possibleJobHeirs.get(0).addOccupation(occupation, date);
            return new EmploymentEvent(date, possibleJobHeirs.get(0), occupation);
        }

        return null;
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
                                                    @NonNull RandomFamilyParameters familyParameters) {
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
                // Keep people in the same social bracket
                .filter(p -> (p.getSocialClass().getRank() >= person.getSocialClass().getRank() - 2) &&
                              p.getSocialClass().getRank() <= person.getSocialClass().getRank() + 2)
                // Filter out married people, women who were married more than once, and widows with children
                .filter(p -> !p.isMarriedNowOrAfter(filterSearchDate) && !isWidowWithChildren(p, filterSearchDate))
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

    /**
     * Loads only unfinished people who have never lived in a household
     */
    List<Person> loadUnfinishedNonResidents() {
        return personRepository.findUnfinishedNonResidents(null);
    }

    /**
     * Returns true if the person is a woman who was married more than once, or who has living children, or has a
     * husband still living. Otherwise false.
     * @param person the person to check
     * @param onDate the date on which to perform the check
     * @return true if this is a widow with children, or woman married more than once, else false
     */
    private static boolean isWidowWithChildren(@NonNull Person person, @NonNull LocalDate onDate) {
        return (person.isFemale() &&
                (person.getFamilies().size() >= 2
                || !person.getLivingChildren(onDate).isEmpty()
                || person.getSpouse(onDate) != null));
    }

    public void generateStartingCapitalForFounder(@NonNull Person founder, @NonNull LocalDate onDate) {
        double startingWealth = WealthGenerator.getRandomStartingCapital(founder.getSocialClass(),
                !founder.getOccupations().isEmpty());
        PersonCapitalPeriod period = new PersonCapitalPeriod();
        period.setPerson(founder);
        period.setPersonId(founder.getId());
        period.setFromDate(onDate);
        period.setToDate(founder.getDeathDate());
        period.setCapital(startingWealth);
        period.setReason(PersonCapitalPeriod.Reason.startingCapitalMessage());
        founder.getCapitalPeriods().add(period);
        save(founder);
    }

    /**
     * If a person becomes owner of an estate and he has no last name, he gets a new last name of "of [Estate Name]".
     * If he has living children without a last name, they also get the new name.
     *
     * @param person the new owner
     * @param estate the estate he bought or inherited
     * @param date the date on which the property transfer occurred, so that the name can be applied to his living
     *             children
     */
    void maybeUpdateLastNameForNewOwnerOfEstate(@NonNull Person person, @NonNull Estate estate, @NonNull LocalDate date) {
        if (person.getLastName() == null) {
            person.setLastName("of " + estate.getName());
            log.info(String.format("Resetting last name of buyer purchasing estate from %d %s to %d %s",
                    person.getId(), person.getFirstName(), person.getId(), person.getName()));
            save(person);
            for (Person child : person.getLivingChildren(date)) {
                if (child.getLastName() == null) {
                    log.info(String.format("Setting last name of child %d %s from %s to %s", child.getId(),
                            child.getName(), child.getLastName(), person.getLastName()));
                    child.setLastName(person.getLastName());
                    save(child);
                }
            }
        }
    }
}
