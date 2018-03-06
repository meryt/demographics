package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.time.Period;
import com.meryt.demographics.domain.person.Person;
import lombok.NonNull;
import org.apache.commons.math3.distribution.BetaDistribution;

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
     * Determine whether two such people will desire to be married
     */
    public static boolean checkCompatibility(@NonNull Person person, @NonNull Person potentialSpouse) {
        PercentDie die = new PercentDie();
        double comelinessDiff = Math.abs(person.getComeliness() - potentialSpouse.getComeliness());
        double charismaDiff = Math.abs(person.getCharisma() - potentialSpouse.getCharisma());
        double personDom = person.getDomesticity();
        double spouseDom = potentialSpouse.getDomesticity();
        return (die.roll() < (personDom + spouseDom - comelinessDiff - charismaDiff));
    }

}
