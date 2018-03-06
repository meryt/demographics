package com.meryt.demographics.generator;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.request.PersonParameters;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
public class FamilyGenerator {

    private final PersonGenerator personGenerator;

    public FamilyGenerator(@Autowired PersonGenerator personGenerator) {
        this.personGenerator = personGenerator;
    }

    public Family generate(@NonNull FamilyParameters familyParameters) {
        Person person = generateFounder(familyParameters);

        Family family = generateSpouse(person, familyParameters.getReferenceDate());

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

    private Family generateSpouse(@NonNull Person person, LocalDate untilDate) {

        Family family = new Family();

        int minMarriageAge;
        Gender spouseGender;
        if (person.isMale()) {
            minMarriageAge = FamilyParameters.DEFAULT_MIN_HUSBAND_AGE;
            spouseGender = Gender.FEMALE;
            family.setHusband(person);
        } else {
            minMarriageAge = FamilyParameters.DEFAULT_MIN_WIFE_AGE;
            spouseGender = Gender.MALE;
            family.setWife(person);
        }

        LocalDate endDate;
        if (untilDate == null || person.getDeathDate().isBefore(untilDate)) {
            endDate = person.getDeathDate();
        } else {
            endDate = untilDate;
        }

        LocalDate startDate = person.getBirthDate().plusYears(minMarriageAge);

        PercentDie die = new PercentDie();
        for (LocalDate currentDate = startDate; currentDate.isBefore(endDate) ; currentDate = currentDate.plusDays(1)) {
            double percentPerDay = MatchMaker.getDesireToMarryProbability(person, currentDate);
            if (die.roll() <= percentPerDay) {
                // He wants to get married. Can he find a spouse? Generate a random person of the appropriate gender
                // and age, and do a random check against the domesticity. If success, do a marriage. Otherwise
                // discard the random person and continue searching.
                LocalDate birthDate = getRandomSpouseBirthDate(person, currentDate);
                PersonParameters spouseParameters = new PersonParameters();
                spouseParameters.setGender(spouseGender);
                spouseParameters.setBirthDate(birthDate);
                spouseParameters.setAliveOnDate(currentDate);
                Person potentialSpouse = personGenerator.generate(spouseParameters);
                if (die.roll() < potentialSpouse.getDomesticity()) {
                    family.setWeddingDate(currentDate);
                    if (potentialSpouse.isFemale()) {
                        family.setWife(potentialSpouse);
                    } else {
                        family.setHusband(potentialSpouse);
                    }
                    break;
                }
            }
        }

        return family;
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
     */
    private LocalDate getRandomSpouseBirthDate(@NonNull Person person, @NonNull LocalDate weddingDate) {
        int personAge = person.getAgeInYears(weddingDate);
        BetweenDie die = new BetweenDie();
        int spouseAge;
        if (person.isMale()) {
            spouseAge = die.roll(FamilyParameters.DEFAULT_MIN_WIFE_AGE, personAge + 2);
        } else {
            int minAge = Math.max(personAge - 1, FamilyParameters.DEFAULT_MIN_HUSBAND_AGE);
            int maxAge = Math.max(FamilyParameters.DEFAULT_MAX_WIFE_AGE, minAge);
            spouseAge = die.roll(minAge, maxAge);
        }

        // The birth date is this many years (minus a random number of days) ago.
        return weddingDate.minusDays((365 * spouseAge) - new Die(364).roll());
    }

}
