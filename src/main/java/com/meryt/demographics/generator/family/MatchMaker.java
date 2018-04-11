package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import java.time.Period;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.generator.random.PercentDie;
import lombok.NonNull;
import org.apache.commons.math3.distribution.BetaDistribution;

class MatchMaker {

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
     * Determine how eager a person of this age will be to marry
     */
    static double getDesireToMarryProbability(@NonNull Person person, @NonNull LocalDate onDate) {
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
        return ageAdjustedDesirePercent * person.getDomesticity() * BASE_PER_DAY_MARRY_DESIRE_PERCENT;
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
        if (!person.isLiving(onDate) || !potentialSpouse.isLiving(onDate)) {
            return false;
        }
        if (person.getGender() == potentialSpouse.getGender()) {
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
        return (die.roll() < ((personDom + spouseDom + femaleAgeModifier) - comelinessDiff - charismaDiff));
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

        int diff = Math.abs(manClass.getRank() - womanClass.getRank());
        // A man can marry at his rank or 1 lower without problem
        if (diff == 0 || diff == 1) {
            return true;
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
        double desirability = (lesser.getComeliness() + lesser.getCharisma()) / 2.0;

        // An aging woman of a greater rank will be less discriminating
        desirability += femaleAgeDesireToMarryModifier(greater, onDate);

        return ((new PercentDie().roll() * diff) < (desirability - 0.7));
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
