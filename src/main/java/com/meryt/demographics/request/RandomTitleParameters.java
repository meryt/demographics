package com.meryt.demographics.request;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.title.Peerage;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.PercentDie;

@Getter
@Setter
public class RandomTitleParameters {
    private static final int DEFAULT_NUM_NEW_TITLE_PER_CENTURY = 5;

    private Integer numNewTitlesPerCentury;
    private Double percentScottish;
    private SocialClass minSocialClass;
    private SocialClass maxSocialClass;
    private boolean socialClassEquallyWeighted;
    private List<String> names;
    private List<String> scottishNames;
    private RandomFamilyParameters familyParameters;

    private double getPercentScottishOrDefault() {
        return getPercentScottish() == null
                ? 0.3
                : getPercentScottish();
    }

    public Peerage getRandomPeerage() {
        return (PercentDie.roll() <= getPercentScottishOrDefault())
                ? Peerage.SCOTLAND
                : Peerage.ENGLAND;
    }

    public SocialClass getRandomSocialClass() {
        SocialClass min = minSocialClass == null ? SocialClass.BARONET : minSocialClass;
        SocialClass max = maxSocialClass == null ? SocialClass.DUKE : maxSocialClass;
        if (isSocialClassEquallyWeighted()) {
            return SocialClass.fromRank(BetweenDie.roll(min.getRank(), max.getRank()));
        } else {
            return SocialClass.randomBetween(min, max);
        }
    }

    public RandomFamilyParameters getFamilyParametersOrDefault() {
        if (familyParameters != null) {
            return familyParameters;
        }
        RandomFamilyParameters params = new RandomFamilyParameters();
        params.setPercentMaleFounders(1.0);
        params.setPersist(true);
        params.setCycleToDeath(false);
        params.setMinSpouseSelection(3);
        params.setAllowExistingSpouse(true);
        params.setChanceGeneratedSpouse(0.8);
        return params;
    }

    public boolean shouldCreateNewTitleOnDay() {
        double percentChance = (getNumNewTitlesPerCenturyOrDefault() / 100.0) / 365.0;
        double roll = PercentDie.roll();
        return (roll <= percentChance);
    }

    private int getNumNewTitlesPerCenturyOrDefault() {
        return numNewTitlesPerCentury == null ? DEFAULT_NUM_NEW_TITLE_PER_CENTURY : numNewTitlesPerCentury;
    }
}
