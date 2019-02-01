package com.meryt.demographics.request;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.title.Peerage;
import com.meryt.demographics.generator.random.PercentDie;

@Getter
@Setter
public class RandomTitleParameters {
    private Double percentScottish;
    private SocialClass minSocialClass;
    private SocialClass maxSocialClass;
    private List<String> names;
    private List<String> scottishNames;

    private double getPercentScottishOrDefault() {
        return getPercentScottish() == null
                ? 0.3
                : getPercentScottish();
    }

    public Peerage getRandomPeerage() {
        return (new PercentDie().roll() <= getPercentScottishOrDefault())
                ? Peerage.SCOTLAND
                : Peerage.ENGLAND;
    }

    public SocialClass getRandomSocialClass() {
        SocialClass min = minSocialClass == null ? SocialClass.BARONET : minSocialClass;
        SocialClass max = maxSocialClass == null ? SocialClass.DUKE : maxSocialClass;
        return SocialClass.randomBetween(min, max);
    }
}
