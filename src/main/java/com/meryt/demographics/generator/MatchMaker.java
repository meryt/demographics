package com.meryt.demographics.generator;

import com.meryt.demographics.domain.person.Person;
import lombok.NonNull;
import org.apache.commons.math3.distribution.BetaDistribution;

import java.time.LocalDate;
import java.time.Period;

public class MatchMaker {

    /**
     * Beta with peak at 2.5 for x = 0.2
     */
    private final static BetaDistribution DESIRE_TO_MARRY_BETA = new BetaDistribution(2, 5);
    private final static double DESIRE_TO_MARRY_MAX = 2.5;
    private final static double BASE_PER_DAY_MARRY_DESIRE_PERCENT = 0.0019;

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
}
