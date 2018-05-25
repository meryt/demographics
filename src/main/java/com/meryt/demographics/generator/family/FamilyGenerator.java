package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.RelatedPerson;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.service.PersonService;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FamilyGenerator {

    private static final int DIED_IN_CHILDHOOD_AGE = 13;
    private static final int MAX_MONTHS_PREMARITAL = 6;

    private final PersonGenerator personGenerator;

    private final PersonService personService;

    public FamilyGenerator(@Autowired PersonGenerator personGenerator,
                           @Autowired PersonService personService) {
        this.personGenerator = personGenerator;
        this.personService = personService;
    }

    /**
     * This method will always generate a family (provided the family parameters are not impossible to satisfy).
     */
    @NonNull
    public Family generate(@NonNull FamilyParameters familyParameters) {
        Person person = generateFounder(familyParameters);
        Family family = null;
        while (family == null) {
            family = generate(person, familyParameters);
        }
        return family;
    }

    /**
     * This method will attempt to generate a family for the given founder. If the attempt fails (no spouse could be
     * found) returns null.
     *
     * @param founder the founder (may be male or female)
     * @param familyParameters additional parameters
     * @return a family with a husband and wife and possibly children, or null
     */
    @Nullable
    public Family generate(@NonNull Person founder, @NonNull FamilyParameters familyParameters) {

        Family family = searchForSpouse(founder, familyParameters);
        if (family != null && family.getHusband() != null && family.getWife() != null) {
            log.info(String.format("%s married %s on %s", family.getHusband().getName(), family.getWife().getName(),
                    family.getWeddingDate()));
        } else {
            log.info(String.format("%s could not find a %s.", founder.getName(),
                    founder.isMale() ? "wife" : "husband"));
            return null;
        }

        if (family.getWife().isSurvivedByASpouse()) {
            family.getWife().setFinishedGeneration(true);
        }
        if (family.getHusband().isSurvivedByASpouse()) {
            family.getHusband().setFinishedGeneration(true);
        }

        generateChildren(family, familyParameters);

        for (Person child : family.getChildren()) {
            if (child.getAgeInYears(child.getDeathDate()) <= DIED_IN_CHILDHOOD_AGE) {
                child.setFinishedGeneration(true);
            }
        }

        for (Person p : Arrays.asList(family.getWife(), family.getHusband())) {
            if (p != null && familyParameters.getReferenceDate() != null && !p.isLiving(familyParameters.getReferenceDate())) {
                log.info(String.format("%s died on %s", p.getName(), p.getDeathDate()));
            }
        }

        return family;
    }

    /**
     * Generates a valid founder given the family parameters
     *
     * @return a person suitable to found a family
     */
    @NonNull
    Person generateFounder(FamilyParameters familyParameters) {
        validate(familyParameters);

        PercentDie die = new PercentDie();
        Gender founderGender = (die.roll() < familyParameters.getPercentMaleFoundersOrDefault())
                ? Gender.MALE
                : Gender.FEMALE;

        LocalDate targetDate = familyParameters.getReferenceDate();

        int minAge;
        int maxAge;
        if (founderGender == Gender.MALE) {
            minAge = familyParameters.getMinHusbandAgeOrDefault();
            maxAge = familyParameters.getMaxHusbandAgeOrDefault();
        } else {
            minAge = familyParameters.getMinWifeAgeOrDefault();
            maxAge = familyParameters.getMaxWifeAgeOrDefault();
        }

        PersonParameters personParameters = new PersonParameters();
        personParameters.setGender(founderGender);
        personParameters.setAliveOnDate(targetDate);
        personParameters.setMinAge(minAge);
        personParameters.setMaxAge(maxAge);
        personParameters.setMinSocialClass(familyParameters.getMinSocialClass());
        personParameters.setMaxSocialClass(familyParameters.getMaxSocialClass());
        personParameters.setLastName(familyParameters.getFounderLastName());

        return personGenerator.generate(personParameters);
    }

    @Nullable
    private Family searchForSpouse(@NonNull Person person, @NonNull FamilyParameters familyParameters) {

        LocalDate untilDate = familyParameters.getReferenceDate();

        LocalDate endDate;
        if (untilDate == null || person.getDeathDate().isBefore(untilDate)) {
            endDate = person.getDeathDate();
        } else {
            endDate = untilDate;
        }

        LocalDate startDate = MatchMaker.getDateToStartMarriageSearch(person,
                familyParameters.getMinHusbandAgeOrDefault(),
                familyParameters.getMinWifeAgeOrDefault());

        return attemptToFindSpouse(startDate, endDate, person, familyParameters);
    }

    /**
     * Run a loop from start date to end date and attempt to find a spouse during that time.
     * @param startDate date to begin looping at (e.g. when person reaches marriageable age)
     * @param endDate date to stop looping
     * @param person the person seeking a spouse
     */
    @Nullable
    private Family attemptToFindSpouse(@NonNull LocalDate startDate,
                                       @NonNull LocalDate endDate,
                                       @NonNull Person person,
                                       @NonNull FamilyParameters familyParameters) {

        PercentDie die = new PercentDie();
        for (LocalDate currentDate = startDate; currentDate.isBefore(endDate) ; currentDate = currentDate.plusDays(1)) {
            double percentPerDay = MatchMaker.getDesireToMarryProbability(person, currentDate,
                    person.getSpouses().size(),
                    person.getLivingChildren(startDate).stream().anyMatch(Person::isMale));
            if (die.roll() <= percentPerDay) {
                // He wants to get married. Can he find a spouse?
                Person potentialSpouse;
                if (familyParameters.getSpouse() != null) {
                    potentialSpouse = familyParameters.getSpouse();
                    Integer minAge = potentialSpouse.isFemale()
                            ? familyParameters.getMinWifeAge()
                            : familyParameters.getMinHusbandAge();
                    if (minAge != null && potentialSpouse.getAgeInYears(currentDate) < minAge) {
                        return null;
                    }
                } else if (familyParameters.isAllowExistingSpouse()) {
                    // We're not given a spouse, but are allowed to chose an eligible one from the database
                    List<RelatedPerson> potentialSpouses = personService.findPotentialSpouses(person, currentDate,
                            false, familyParameters);
                    if (familyParameters.getMinSpouseSelection() != null &&
                            potentialSpouses.size() < familyParameters.getMinSpouseSelection()) {
                        // If there are fewer potential spouses than the min selection size, we may need to generate
                        // a spouse. Get a random value between 1 and the selection size, and if the value is above
                        // the size of the list, generate a random person. Otherwise use the die roll as the index
                        // into the list.
                        int roll = new Die(familyParameters.getMinSpouseSelection()).roll();
                        if (roll > potentialSpouses.size()) {
                            // generate a random spouse
                            potentialSpouse = generateRandomPotentialSpouse(person, currentDate, familyParameters);
                        } else {
                            potentialSpouse = potentialSpouses.get(roll - 1).getPerson();
                        }
                    } else if (potentialSpouses.isEmpty()) {
                        potentialSpouse = generateRandomPotentialSpouse(person, currentDate, familyParameters);
                    } else {
                        potentialSpouse = potentialSpouses.get(new Die(potentialSpouses.size()).roll() - 1).getPerson();
                    }
                } else {
                    // generate a random spouse
                    potentialSpouse = generateRandomPotentialSpouse(person, currentDate, familyParameters);
                }
                if (MatchMaker.checkCompatibility(person, potentialSpouse, currentDate)) {
                    Family family = new Family();
                    if (person.isMale()) {
                        family.setHusband(person);
                    } else {
                        family.setWife(person);
                    }

                    family.setWeddingDate(currentDate);
                    family.addSpouse(potentialSpouse);
                    return family;
                }
            }
        }
        return null;
    }

    private Person generateRandomPotentialSpouse(@NonNull Person person,
                                                 @NonNull LocalDate onDate,
                                                 @NonNull FamilyParameters familyParameters) {
        // Generate a random person of the appropriate gender
        // and age, and do a random check against the domesticity and other factors. If success, do a marriage.
        // Otherwise discard the random person and continue searching.
        LocalDate birthDate = getRandomSpouseBirthDate(person, onDate, familyParameters);
        SocialClass socialClass = getRandomSpouseSocialClass(person);
        PersonParameters spouseParameters = new PersonParameters();
        spouseParameters.setGender(person.isMale() ? Gender.FEMALE : Gender.MALE);
        spouseParameters.setBirthDate(birthDate);
        spouseParameters.setAliveOnDate(onDate);
        spouseParameters.setMinSocialClass(socialClass);
        spouseParameters.setMaxSocialClass(socialClass);
        spouseParameters.setLastName(familyParameters.getSpouseLastName());
        return personGenerator.generate(spouseParameters);

    }

    private void validate(FamilyParameters familyParameters) {
        if (familyParameters.getReferenceDate() == null) {
            throw new IllegalArgumentException("referenceDate is required for generating a family.");
        }
    }

    /**
     * Get a random date that can be used as the birth date of a spouse for the given person, if the wedding is on the
     * given date. If the given person is a man, the birth date for his wife will be below or barely above his own
     * birth date; if a woman, the husband's birth date will be barely below or above hers.
     *
     * @param person the person who is looking for a spouse
     * @param weddingDate the desired wedding date
     */
    private LocalDate getRandomSpouseBirthDate(@NonNull Person person, @NonNull LocalDate weddingDate,
                                               @NonNull FamilyParameters familyParameters) {
        int personAge = person.getAgeInYears(weddingDate);
        BetweenDie die = new BetweenDie();
        int spouseAge;
        if (person.isMale()) {
            int minAge = Math.min(familyParameters.getMinWifeAgeOrDefault(), personAge);
            int maxAge = Math.max(familyParameters.getMinWifeAgeOrDefault(), personAge + 2);
            spouseAge = die.roll(minAge, maxAge);
        } else {
            int minAge = familyParameters.getMinHusbandAgeOrDefault(personAge);
            int maxAge = Math.max(familyParameters.getMaxHusbandAgeOrDefault(), minAge);
            spouseAge = die.roll(minAge, maxAge);
        }

        // The birth date is this many years (minus a random number of days) ago.
        return weddingDate.minusDays((365 * spouseAge) + new Die(364).roll());
    }

    /**
     * Gets a random social class for a potential spouse of a person, given their social class. Uses a normal
     * distribution centered on the person's own class.
     *
     * @param person the person searching for a spouse
     * @return a random rank using a normal distribution
     */
    private SocialClass getRandomSpouseSocialClass(@NonNull Person person) {
        double randomRank = new NormalDistribution(person.getSocialClass().getRank(), 1).sample();
        long rank = Math.round(randomRank);
        if (rank < SocialClass.PAUPER.getRank()) {
            rank = SocialClass.PAUPER.getRank();
        } else if (rank > SocialClass.MONARCH.getRank()) {
            rank = SocialClass.MONARCH.getRank();
        }

        return SocialClass.fromRank((int) rank);
    }

    /**
     * Generates children for the family given the parameters, and adds them to the existing list of children.
     *
     * @param family a family that is expected to include a husband and wife and wedding date
     * @param familyParameters the parameters for random family generation
     */
    private void generateChildren(@NonNull Family family, @NonNull FamilyParameters familyParameters) {
        if (family.getHusband() == null || family.getWife() == null || family.getWeddingDate() == null
                || family.getHusband().getDeathDate() == null || family.getWife().getDeathDate() == null) {
            return;
        }

        ((Maternity) family.getWife().getFertility()).cycleToDate(family.getWeddingDate(), true);

        boolean allowPremarital = (family.getWife().getSocialClass().getRank()
                <= SocialClass.LANDOWNER_OR_CRAFTSMAN.getRank());
        LocalDate fromDate = family.getWeddingDate();

        List<LocalDate> dates = new ArrayList<>();
        dates.add(family.getHusband().getDeathDate().plusMonths(10));
        dates.add(family.getWife().getDeathDate());
        if (!familyParameters.isCycleToDeath()) {
            dates.add(familyParameters.getReferenceDate());
        }
        Optional<LocalDate> minDate = dates.stream().min(Comparator.comparing(LocalDate::toEpochDay));
        LocalDate toDate = familyParameters.getReferenceDate();
        if (minDate.isPresent()) {
            toDate = minDate.get();
        }

        if (allowPremarital) {
            fromDate = fromDate.minusDays(new Die(30 * MAX_MONTHS_PREMARITAL).roll());
        }

        if (fromDate.isAfter(toDate)) {
            return;
        }

        PregnancyChecker checker = new PregnancyChecker(personGenerator, family, false);
        checker.checkDateRange(fromDate, toDate);

        // If we're cycling until first death but woman is pregnant, continue cycling till she gives birth
        if (familyParameters.isCycleToDeath() && family.getWife().getMaternity().isPregnant(toDate)) {
            checker.checkDateRange(toDate.plusDays(1), family.getWife().getMaternity().getDueDate());
        }
    }

}
