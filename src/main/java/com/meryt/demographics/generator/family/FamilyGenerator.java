package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.generator.PersonGenerator;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.request.PersonParameters;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FamilyGenerator {

    private static final int MAX_MONTHS_PREMARITAL = 6;

    private final PersonGenerator personGenerator;

    public FamilyGenerator(@Autowired PersonGenerator personGenerator) {
        this.personGenerator = personGenerator;
    }

    public Family generate(@NonNull FamilyParameters familyParameters) {
        Person person = generateFounder(familyParameters);

        Family family = searchForSpouse(person, familyParameters);
        generateChildren(family, familyParameters);
        return family;
    }

    private Person generateFounder(FamilyParameters familyParameters) {
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

        return personGenerator.generate(personParameters);
    }

    private Family searchForSpouse(@NonNull Person person, @NonNull FamilyParameters familyParameters) {

        Family family = new Family();

        LocalDate untilDate = familyParameters.getReferenceDate();

        int minMarriageAge = person.isMale()
                ? familyParameters.getMinHusbandAgeOrDefault()
                : familyParameters.getMinWifeAgeOrDefault();
        if (person.isMale()) {
            family.setHusband(person);
        } else {
            family.setWife(person);
        }

        LocalDate endDate;
        if (untilDate == null || person.getDeathDate().isBefore(untilDate)) {
            endDate = person.getDeathDate();
        } else {
            endDate = untilDate;
        }

        LocalDate startDate = person.getBirthDate().plusYears(minMarriageAge);
        attemptToFindSpouse(startDate, endDate, family, person, familyParameters);

        return family;
    }

    /**
     * Run a loop from start date to end date and attempt to find a spouse during that time.
     * @param startDate date to begin looping at (e.g. when person reaches marriageable age)
     * @param endDate date to stop looping
     * @param family the family on which to set the spouse and wedding date, if one is found
     * @param person the person seeking a spouse
     */
    private void attemptToFindSpouse(@NonNull LocalDate startDate, @NonNull LocalDate endDate, @NonNull Family family,
                                     @NonNull Person person, @NonNull FamilyParameters familyParameters) {
        Gender spouseGender = person.isFemale() ? Gender.MALE : Gender.FEMALE;
        PercentDie die = new PercentDie();
        for (LocalDate currentDate = startDate; currentDate.isBefore(endDate) ; currentDate = currentDate.plusDays(1)) {
            double percentPerDay = MatchMaker.getDesireToMarryProbability(person, currentDate);
            if (die.roll() <= percentPerDay) {
                // He wants to get married. Can he find a spouse? Generate a random person of the appropriate gender
                // and age, and do a random check against the domesticity. If success, do a marriage. Otherwise
                // discard the random person and continue searching.
                LocalDate birthDate = getRandomSpouseBirthDate(person, currentDate, familyParameters);
                PersonParameters spouseParameters = new PersonParameters();
                spouseParameters.setGender(spouseGender);
                spouseParameters.setBirthDate(birthDate);
                spouseParameters.setAliveOnDate(currentDate);
                Person potentialSpouse = personGenerator.generate(spouseParameters);
                if (MatchMaker.checkCompatibility(person, potentialSpouse)) {
                    family.setWeddingDate(currentDate);
                    family.addSpouse(potentialSpouse);
                    break;
                }
            }
        }
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

        boolean allowPremarital = (family.getWife().getSocialClass().ordinal()
                <= SocialClass.LANDOWNER_OR_CRAFTSMAN.ordinal());
        LocalDate fromDate = family.getWeddingDate();

        List<LocalDate> dates = Arrays.asList(familyParameters.getReferenceDate(), family.getHusband().getDeathDate(),
                family.getWife().getDeathDate());
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

    }

}
