package com.meryt.demographics.generator.person;

import java.time.LocalDate;
import lombok.NonNull;
import org.apache.commons.math3.distribution.BetaDistribution;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.domain.person.fertility.Paternity;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.math.FunkyBetaDistribution;

public class FertilityGenerator {
    private static final double RAND_FERTILITY_ALPHA          = 2.5;
    private static final double RAND_FERTILITY_BETA           = 5;
    private static final double RAND_FIRST_PERIOD_ALPHA		  = 1.1;
    private static final double RAND_FIRST_PERIOD_BETA		  = 5;
    private static final double RAND_FREQUENCY_ALPHA          = 4; // 4:5 makes a sort of bell curve
    private static final double RAND_FREQUENCY_BETA           = 5;
    private static final double RAND_WITHDRAWAL_ALPHA         = 1.1;
    private static final double RAND_WITHDRAWAL_BETA          = 9;


    private static final int  FIRST_PERIOD_BASE_MIN_AGE_YEARS = 11;
    private static final int  FIRST_PERIOD_BASE_MAX_AGE_YEARS = 16;



    Maternity randomMaternity(@NonNull Person woman) {
        Maternity maternity = new Maternity();
        maternity.setFertilityFactor(randFertilityFactor());
        maternity.setLastCycleDate(randFirstCycleDate(woman.getBirthDate()));
        maternity.setLastCheckDate(maternity.getLastCycleDate());
        maternity.setFrequencyFactor(randFrequencyFactor());
        maternity.setWithdrawalFactor(randWithdrawalFactor());
        maternity.setCycleLength(randCycleLength());
        maternity.setHavingRelations(true);
        return maternity;
    }

    Paternity randomPaternity() {
        Paternity paternity = new Paternity();
        paternity.setFertilityFactor(randFertilityFactor());
        return paternity;
    }

    private double randFertilityFactor() {
        return new FunkyBetaDistribution(RAND_FERTILITY_ALPHA, RAND_FERTILITY_BETA).sample();
    }

    private double randFrequencyFactor() {
        return new FunkyBetaDistribution(RAND_FREQUENCY_ALPHA, RAND_FREQUENCY_BETA).sample();
    }

    private double randWithdrawalFactor() {
        return new FunkyBetaDistribution(RAND_WITHDRAWAL_ALPHA, RAND_WITHDRAWAL_BETA).sample();
    }

    private int randCycleLength() {
        return new BetweenDie().roll(26,32);
    }

    private LocalDate randFirstCycleDate(@NonNull LocalDate birthDate) {
        double betaVal = new BetaDistribution(RAND_FIRST_PERIOD_ALPHA, RAND_FIRST_PERIOD_BETA).sample();
        int minAge = FIRST_PERIOD_BASE_MIN_AGE_YEARS * 365;
        int maxAge = FIRST_PERIOD_BASE_MAX_AGE_YEARS * 365;
        return birthDate.plusDays((long) Math.floor((betaVal * (maxAge - minAge)) + minAge));
    }
}
