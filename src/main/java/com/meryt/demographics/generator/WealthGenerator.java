package com.meryt.demographics.generator;

import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.data.util.Pair;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.Die;

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

    public static Pair<Integer, Integer> getHouseValueRange(@NonNull SocialClass socialClass) {
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
                return Pair.of(2000, 20_000);
            case BARON:
                return Pair.of(4000, 40_000);
            case VISCOUNT:
                return Pair.of(10_000, 80_000);
            case EARL:
                return Pair.of(10_000, 100_000);
            case MARQUESS:
                return Pair.of(20_00, 200_000);
            case DUKE:
                return Pair.of(30_000, 300_000);
            case PRINCE:
                return Pair.of(30_000, 400_000);
            case MONARCH:
                return Pair.of(500_000, 1_500_000);
            default:
                throw new IllegalArgumentException("No value range defined for social class " + socialClass.name());
        }
    }

    private static Pair<Integer, Integer> getEstateValueRange(@NonNull SocialClass socialClass) {
        // An estate should cost about 30 times the required income for the social class, up to the amount
        // required for the next highest social class.
        return Pair.of(getYearlyCostOfLivingPerHousehold(socialClass) * 30,
                getYearlyCostOfLivingPerHousehold(socialClass.plusOne()) * 30);
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

    /**
     * Use this method to determine whether a person can rise in a social class when inheriting a large amount of
     * capital. A person can rise at most once per inheritance.
     *
     * @param inheritanceCapital the amount they are inheriting
     * @return the new social class they might be a part of
     */
    public static SocialClass getSocialClassForInheritance(@NonNull SocialClass currentSocialClass,
                                                           double inheritanceCapital) {
        if (currentSocialClass == SocialClass.MONARCH) {
            return currentSocialClass;
        }
        for (int i = SocialClass.MONARCH.getRank(); i > 0; i--) {
            SocialClass socialClass = SocialClass.fromRank(i);
            // You have to have four times the highest value of the social class's upper bound. E.g. if a viscount can
            // start with up to 200,000 then you need at least 800,000 to rise closer to the level of a viscount.
            if (inheritanceCapital >= (getStartingCapitalValueRange(socialClass).getSecond() * 4)) {
                if (socialClass.getRank() <= currentSocialClass.getRank() + 1) {
                    return currentSocialClass;
                } else {
                    return SocialClass.fromRank(currentSocialClass.getRank() + 1);
                }
            }
        }
        return currentSocialClass;
    }

    /**
     * Get the social class a person would have based on his wealth, not considering blood. Finds the highest social
     * class such that twice the upper bound of the social class's starting wealth is less than the given wealth.
     */
    public static SocialClass getSocialClassForWealth(double wealth) {
        for (int i = SocialClass.MONARCH.getRank(); i > 0; i--) {
            SocialClass socialClass = SocialClass.fromRank(i);
            // You have to have two times the highest value of the social class's upper bound. E.g. if a viscount can
            // start with up to 200,000 then you need at least 800,000 to rise closer to the level of a viscount.
            if (wealth >= (getStartingCapitalValueRange(socialClass).getSecond() * 2)) {
                return socialClass;
            }
        }
        return SocialClass.PAUPER;
    }

    /**
     * Gets the yearly income value range for a person with the given occupation. Since the income depends on social
     * class, both the person's social class and the occupation's max social class are compared, and the highest
     * one is taken as the social class for the calculation.
     * @param person
     * @param occupation
     * @return
     */
    public static Pair<Integer, Integer> getYearlyIncomeValueRangeForPersonWithOccupation(@NonNull Person person,
                                                                                          @Nullable Occupation occupation) {
        SocialClass socialClass;
        if (occupation == null) {
            socialClass = person.getSocialClass();
        } else {
            socialClass = person.getSocialClass().getRank() > occupation.getMaxClass().getRank()
                    ? occupation.getMaxClass()
                    : person.getSocialClass();
        }
        return getYearlyIncomeValueRange(socialClass);
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
                return Pair.of(100, 300);
            case BARONET:
                return Pair.of(400, 1_000);
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

    /**
     * At higher levels, the cost of living remains unchanged as it is assumed some expenses and some incomes are
     * from elsewhere. This is the cost of living in one house/estate in one parish.
     */
    public static Integer getYearlyCostOfLivingPerHousehold(@NonNull SocialClass socialClass) {
        switch (socialClass) {
            case PAUPER:
                return 2;
            case LABORER:
                return 9;
            case LANDOWNER_OR_CRAFTSMAN:
                return 25;
            case YEOMAN_OR_MERCHANT:
                return 100;
            case GENTLEMAN:
                return 700;
            case BARONET:
                return 1600;
            case BARON:
                return 4000;
            case VISCOUNT:
                return 4000;
            case EARL:
                return 4000;
            case MARQUESS:
                return 4000;
            case DUKE:
                return 4000;
            case PRINCE:
                return 4000;
            case MONARCH:
                return 4000;
            default:
                throw new IllegalArgumentException("No cost of living defined for social class " + socialClass.name());
        }
    }
}
