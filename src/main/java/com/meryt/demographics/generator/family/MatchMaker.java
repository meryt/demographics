package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.apache.commons.math3.distribution.BetaDistribution;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.person.Trait;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.time.LocalDateComparator;

public class MatchMaker {

    /**
     * Beta with peak at 2.5 for x = 0.2
     */
    private static final BetaDistribution DESIRE_TO_MARRY_BETA = new BetaDistribution(2, 5);
    private static final double DESIRE_TO_MARRY_MAX = 2.5;
    private static final double BASE_PER_DAY_MARRY_DESIRE_PERCENT = 0.0019;

    private MatchMaker() {
        // hide constructor of class with static methods
    }

    /**
     * Given a person and the min age for men/women to marry, determine the date the person might begin looking to
     * marry. If the person has already been married, starts looking after the death of the last spouse.
     *
     * @param person a person to search for
     * @param minHusbandAge the age at which men begin to search
     * @param minWifeAge the age at which womenb egin to search
     * @return a date, possibly after the death date if the person died young or was previously married and was
     * outlived by their spouse
     */
    public static LocalDate getDateToStartMarriageSearch(@NonNull Person person,
                                                         int minHusbandAge,
                                                         int minWifeAge) {
        LocalDate startDate;
        if (person.isMale()) {
            startDate = person.getBirthDate().plusYears(minHusbandAge);
        } else {
            startDate = person.getBirthDate().plusYears(minWifeAge);
        }

        // Create a new family only after the death of the last existing spouse.
        List<Family> existingFamilies = person.getFamilies();
        for (Family existingFamily : existingFamilies) {
            Person spouse = person.isMale() ? existingFamily.getWife() : existingFamily.getHusband();
            if (existingFamily.isMarriage() &&
                    spouse != null
                    && spouse.getDeathDate() != null
                    && spouse.getDeathDate().isAfter(startDate)) {
                startDate = spouse.getDeathDate();
            }
        }

        return startDate;
    }

    /**
     * Determine how eager a person of this age will be to marry
     */
    static double getDesireToMarryProbability(@NonNull Person person,
                                              @NonNull LocalDate onDate,
                                              @Nullable Integer numPreviousSpouses,
                                              @Nullable LocalDate lastSpouseDeathDate,
                                              @Nullable LocalDate lastLivingSonDeathDate) {
        if (person.isFemale() && lastSpouseDeathDate != null && lastSpouseDeathDate.isBefore(onDate)
                && LocalDateComparator.daysBetween(lastSpouseDeathDate, onDate) < 9*30) {
            // A widow won't remarry till she is sure she is not pregnant
            return 0.0;
        }
        LocalDate birthDate = person.getBirthDate();
        if (birthDate == null) {
            throw new NullPointerException("Cannot calculate without person birth date");
        }
        int age = Period.between(birthDate, onDate).getYears();
        if (age > 100) {
            return 0.0;
        }
        double adjustedAge = age / 100.0;
        double ageAdjustedDesirePercent = (DESIRE_TO_MARRY_BETA.density(adjustedAge) / DESIRE_TO_MARRY_MAX);
        double dailyAgeAdjustedDesirePercent = ageAdjustedDesirePercent * person.getDomesticity() * BASE_PER_DAY_MARRY_DESIRE_PERCENT;
        // Previous marriages reduce desire to marry again for women, and likewise for men assuming they have at
        // least one living a son. A man without sons is not affected by previous marriages.
        if (numPreviousSpouses != null && numPreviousSpouses > 0 &&
                (person.isFemale() ||
                    (lastLivingSonDeathDate != null && onDate.isBefore(lastLivingSonDeathDate)))) {
            dailyAgeAdjustedDesirePercent /= numPreviousSpouses;
        }
        return dailyAgeAdjustedDesirePercent;
    }

    /**
     * Determine whether two such people will desire to be married. First checks that their social classes are
     * compatible or at least that the lesser partner is highly desirable; then check that their traits are compatible.
     *
     * @param person the person we're checking for
     * @param potentialSpouse the potential spouse
     * @param onDate the date (an older woman is less discriminating)
     */
    static boolean checkCompatibility(@NonNull Person person,
                                      @NonNull Person potentialSpouse,
                                      @NonNull LocalDate onDate) {
        if (person.getGender() == potentialSpouse.getGender()) {
            return false;
        }
        if (!person.isLiving(onDate) || !potentialSpouse.isLiving(onDate)) {
            return false;
        }
        if (potentialSpouse.isMarriedNowOrAfter(onDate)) {
            return false;
        }

        Person man = person.isMale() ? person : potentialSpouse;
        Person woman = person.isMale() ? potentialSpouse : person;
        if (!checkSocialClassCompatibility(man, woman, onDate)) {
            return false;
        }

        PercentDie die = new PercentDie();
        double comelinessDiff = Math.abs(person.getComeliness() - potentialSpouse.getComeliness());
        double charismaDiff = Math.abs(person.getCharisma() - potentialSpouse.getCharisma());
        double personDom = person.getDomesticity();
        double spouseDom = potentialSpouse.getDomesticity();
        double femaleAgeModifier = femaleAgeDesireToMarryModifier(woman, onDate);

        Set<Trait> sharedTraits = new HashSet<>(person.getTraits());
        sharedTraits.retainAll(potentialSpouse.getTraits());

        // People who share one or more traits will get on especially well.
        double sharedTraitModifier = 0.25 * sharedTraits.size();

        // A person with desirable traits will be a desirable partner. If both have bad traits, it will greatly
        // decrease compatibility. If both have good traits, it will increase. If they cancel each other out, it
        // means one spouse was desirable and the other not particularly.
        double traitModifier = getTraitModifier(person, 0.03) + getTraitModifier(potentialSpouse, 0.03);

        return (die.roll() < ((personDom + spouseDom + femaleAgeModifier + sharedTraitModifier + traitModifier)
                - comelinessDiff - charismaDiff));
    }

    /**
     * Determine whether the couple is compatible in terms of social class. A high class difference is a problem,
     * particularly if the female outranks the male. However, an extremely attractive lesser-born person may overcome
     * this. If there is no difference or the man is only one level higher, it always passes the check.
     *
     * @param man the male
     * @param woman the female
     * @param onDate the date to use to determine the woman's age factor
     * @return true if the random check for compatibility passes
     */
    private static boolean checkSocialClassCompatibility(@NonNull Person man,
                                                         @NonNull Person woman,
                                                         @NonNull LocalDate onDate) {
        SocialClass manClass = man.getSocialClass();
        SocialClass womanClass = woman.getSocialClass();

        int diff = manClass.getRank() - womanClass.getRank();
        // A man can marry at his rank or 1 lower without problem.
        if (diff == 0 || diff == 1) {
            return true;
        }
        // A woman cannot readily marry below her rank and must check attractiveness etc. as if the difference was 1
        // greater than it really is.
        if (diff < 0) {
            diff = Math.abs(diff) + 1;
        }

        // The likelihood depends on the desirability of the lower-ranking spouse.
        Person lesser;
        Person greater;
        if (manClass.getRank() < womanClass.getRank()) {
            lesser = man;
            greater = woman;
        } else {
            lesser = woman;
            greater = man;
        }

        // Get the average of comeliness and charisma
        double attractiveness = (lesser.getComeliness() + lesser.getCharisma()) / 2.0;

        // Get the ranking from the traits. Positive traits provide a bonus, negative traits a malus.
        attractiveness += getTraitModifier(lesser, 0.05);

        // An aging woman of a greater rank will be less discriminating
        attractiveness += femaleAgeDesireToMarryModifier(greater, onDate);

        // For a rank difference of 2, the person must have an attractiveness score of 0.7; for 3 of 0.75, for 4 of 0.8
        // rank diff = 2, min attractiveness = 0.7
        // rank diff = 3, min attractiveness = 0.8
        // rank diff = 4, min attractiveness = 0.9
        double minAttractiveness = 0.5 + (0.1 * diff);

        return (new PercentDie().roll() < (attractiveness - minAttractiveness));
    }

    /**
     * Gets a modifier based on a person's traits sum of + and - values.
     *
     * @param person the person whose traits to check
     * @param baseModifier the value by which to multiple the integer sum, e.g. 0.05
     * @return a double
     */
    private static double getTraitModifier(@NonNull Person person, double baseModifier) {
        int rating = person.getTraits().stream()
                .mapToInt(Trait::getRating)
                .sum();
        return (baseModifier * rating);
    }

    /**
     * Get a modifier to the desirability of a potential husband, given the age of the woman. If she is older, her
     * desire to marry increases. At age 20 or below the modifier is 0.0.
     *
     * @param woman the woman whose age we check
     * @param onDate the date we use to check age
     * @return a number between 0.0 and 0.2
     */
    private static double femaleAgeDesireToMarryModifier(@NonNull Person woman, @NonNull LocalDate onDate) {
        if (woman.isMale()) {
            return 0.0;
        }
        int age = woman.getAgeInYears(onDate);
        if (age > 30) {
            return 0.2;
        } else if (age > 25) {
            return 0.1;
        } else if (age > 20) {
            return 0.05;
        } else {
            return 0.0;
        }
    }

}
