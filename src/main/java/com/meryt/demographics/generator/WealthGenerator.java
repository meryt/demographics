package com.meryt.demographics.generator;

import lombok.NonNull;
import org.springframework.data.util.Pair;

import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.generator.random.BetweenDie;

public class WealthGenerator {

    private static final BetweenDie BETWEEN_DIE = new BetweenDie();

    public static double getRandomStartingCapital(@NonNull SocialClass socialClass, boolean isEmployed) {
        Pair<Integer, Integer> range = getStartingCapitalValueRange(socialClass);
        if (socialClass.getRank() <= SocialClass.YEOMAN_OR_MERCHANT.getRank() || isEmployed) {
            // Assume income is from labor. Take 1-5 years of income as total savings.
            return BETWEEN_DIE.roll(1, 5) * (BETWEEN_DIE.roll(range.getFirst(), range.getSecond()));
        } else {
            // Assume income is from rents. Take 1 year of income and consider it to be 4% of the total capital
            double oneYearInterest = (BETWEEN_DIE.roll(range.getFirst(), range.getSecond()));
            return oneYearInterest / 0.04;
        }
    }

    public static double getRandomHouseValue(@NonNull SocialClass socialClass) {
        Pair<Integer, Integer> range = getHouseValueRange(socialClass);
        return (double) (BETWEEN_DIE.roll(range.getFirst(), range.getSecond()));
    }

    public static double getRandomLandValue(@NonNull SocialClass socialClass) {
        Pair<Integer, Integer> range = getEstateValueRange(socialClass);
        return (double) (BETWEEN_DIE.roll(range.getFirst(), range.getSecond()));
    }

    private static Pair<Integer, Integer> getHouseValueRange(@NonNull SocialClass socialClass) {
        switch (socialClass) {
            case PAUPER:
                return Pair.of(20, 80);
            case LABORER:
                return Pair.of(100, 400);
            case LANDOWNER_OR_CRAFTSMAN:
                return Pair.of(200, 500);
            case YEOMAN_OR_MERCHANT:
                return Pair.of(500, 800);
            case GENTLEMAN:
                return Pair.of(800, 4000);
            case BARONET:
                return Pair.of(800, 4000);
            case BARON:
                return Pair.of(2000, 10_000);
            case VISCOUNT:
                return Pair.of(2000, 10_000);
            case EARL:
                return Pair.of(4000, 20_000);
            case MARQUESS:
                return Pair.of(5000, 20_000);
            case DUKE:
                return Pair.of(10_000, 30_000);
            case PRINCE:
                return Pair.of(10_000, 50_000);
            case MONARCH:
                return Pair.of(50_000, 500_000);
            default:
                throw new IllegalArgumentException("No value range defined for social class " + socialClass.name());
        }
    }

    private static Pair<Integer, Integer> getEstateValueRange(@NonNull SocialClass socialClass) {
        switch (socialClass) {
            case PAUPER:
                return Pair.of(20, 80);
            case LABORER:
                return Pair.of(100, 400);
            case LANDOWNER_OR_CRAFTSMAN:
                return Pair.of(200, 500);
            case YEOMAN_OR_MERCHANT:
                return Pair.of(500, 800);
            case GENTLEMAN:
                return Pair.of(800, 10_000);
            case BARONET:
                return Pair.of(1000, 20_000);
            case BARON:
                return Pair.of(2000, 20_000);
            case VISCOUNT:
                return Pair.of(2000, 20_000);
            case EARL:
                return Pair.of(4000, 40_000);
            case MARQUESS:
                return Pair.of(5000, 50_000);
            case DUKE:
                return Pair.of(10_000, 50_000);
            case PRINCE:
                return Pair.of(20_000, 50_000);
            case MONARCH:
                return Pair.of(30_000, 50_000);
            default:
                throw new IllegalArgumentException("No value range defined for social class " + socialClass.name());
        }
    }

    private static Pair<Integer, Integer> getStartingCapitalValueRange(@NonNull SocialClass socialClass) {
        switch (socialClass) {
            case PAUPER:
                return Pair.of(1, 5);
            case LABORER:
                return Pair.of(5, 15);
            case LANDOWNER_OR_CRAFTSMAN:
                return Pair.of(10, 50);
            case YEOMAN_OR_MERCHANT:
                return Pair.of(50, 200);
            case GENTLEMAN:
                return Pair.of(200, 2000);
            case BARONET:
                return Pair.of(2000, 10_000);
            case BARON:
                return Pair.of(10_000, 50_000);
            case VISCOUNT:
                return Pair.of(20_000, 100_000);
            case EARL:
                return Pair.of(50_000, 150_000);
            case MARQUESS:
                return Pair.of(50_00, 150_000);
            case DUKE:
                return Pair.of(80_000, 250_000);
            case PRINCE:
                return Pair.of(100_000, 500_000);
            case MONARCH:
                return Pair.of(200_000, 1_000_000);
            default:
                throw new IllegalArgumentException("No value range defined for social class " + socialClass.name());
        }
    }

    public static Pair<Integer, Integer> getYearlyIncomeValueRange(@NonNull SocialClass socialClass) {
        switch (socialClass) {
            case PAUPER:
                return Pair.of(1, 5);
            case LABORER:
                return Pair.of(5, 15);
            case LANDOWNER_OR_CRAFTSMAN:
                return Pair.of(10, 50);
            case YEOMAN_OR_MERCHANT:
                return Pair.of(50, 200);
            case GENTLEMAN:
                return Pair.of(200, 500);
            case BARONET:
                return Pair.of(500, 2_000);
            case BARON:
            case VISCOUNT:
            case EARL:
            case MARQUESS:
            case DUKE:
            case PRINCE:
            case MONARCH:
                return Pair.of(2_000, 5_000);
            default:
                throw new IllegalArgumentException("No value range defined for social class " + socialClass.name());
        }
    }
}
