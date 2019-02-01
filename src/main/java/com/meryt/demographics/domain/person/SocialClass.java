package com.meryt.demographics.domain.person;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.meryt.demographics.generator.random.Die;
import lombok.Getter;
import lombok.NonNull;

public enum SocialClass {
    PAUPER(1, "Itinerant laborer, beggar, prisoner"),
    LABORER(2, "Laborer, servant, tenant farmer, sailor, soldier"),
    LANDOWNER_OR_CRAFTSMAN(3, "Freehold farmer, craftsman, small merchant, footman, lady's maid, lieutenant"),
    YEOMAN_OR_MERCHANT(4, "Yeoman farmer, merchant, clerk, steward, valet, butler, captain"),
    GENTLEMAN(5, "Gentleman farmer, clergyman, doctor, lawyer, agent, military officer, major"),
    BARONET(6, "Baronet, estate owner, knighted or high-ranking officer"),
    BARON(7, "Baron, abbot, bishop, colonel, Knight of the Garter"),
    VISCOUNT(8, "Viscount"),
    EARL(9, "Earl, archbishop"),
    MARQUESS(10, "Marquess"),
    DUKE(11, "Duke, Cardinal"),
    PRINCE(12, "Prince"),
    MONARCH(13, "King, emperor, pope");

    @Getter
    private final int rank;

    @Getter
    @JsonValue
    private final String description;

    SocialClass(int rank, String description) {
        this.rank = rank;
        this.description = description;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @JsonCreator
    public static SocialClass fromEnumName(String name) {
        return SocialClass.valueOf(name);
    }

    /**
     * Gets a class from the 1-based index.
     * @param rank an index between 1 and 13 inclusive
     * @throws IllegalArgumentException if the rank is not in range
     * @throws IllegalStateException if the enum value and rank don't match
     */
    @NonNull
    public static SocialClass fromRank(int rank) {
        if (rank < PAUPER.getRank() || rank > MONARCH.getRank()) {
            throw new IllegalArgumentException("No SocialClass exists for rank " + rank);
        }
        List<SocialClass> classes = Arrays.asList(PAUPER, LABORER, LANDOWNER_OR_CRAFTSMAN, YEOMAN_OR_MERCHANT,
                GENTLEMAN, BARONET, BARON, VISCOUNT, EARL, MARQUESS, DUKE, PRINCE, MONARCH);
        SocialClass socialClass = classes.get(rank -1);
        if (socialClass.getRank() != rank) {
            throw new IllegalStateException("Class " + socialClass.name() + " does not have expected rank of " + rank);
        }
        return socialClass;
    }

    /**
     * Returns the next lower class, or the lowest if it's already the lowest
     */
    @NonNull
    public SocialClass minusOne() {
        if (this == PAUPER) {
            return PAUPER;
        } else {
            return fromRank(this.getRank() - 1);
        }
    }

    /**
     * Returns the next higher class, or the highest if it's already the highest
     */
    @NonNull
    public SocialClass plusOne() {
        if (this == MONARCH) {
            return MONARCH;
        } else {
            return fromRank(this.getRank() + 1);
        }
    }

    @NonNull
    public String getFriendlyName() {
        return name().toLowerCase().replace("_", " ");
    }

    public boolean isAtLeast(SocialClass other) {
        return getRank() >= other.getRank();
    }

    /**
     * Gets a random social class between an optional min and optional max. This is not evenly distributed; that is,
     * it is highly more likely to generate a lower class person than higher class.
     *
     * @param minSocialClass minimum or null for no minimum
     * @param maxSocialClass maximum or null for no maximum
     * @return a social class equal to or greater/lesser than the min/max
     */
    @NonNull
    public static SocialClass randomBetween(@Nullable SocialClass minSocialClass, @Nullable SocialClass maxSocialClass) {
        if (minSocialClass != null && maxSocialClass != null && minSocialClass.getRank() > maxSocialClass.getRank()) {
            throw new IllegalArgumentException(String.format("Min SocialClass %s is greater than Max SocialClass %s",
                    minSocialClass.name(), maxSocialClass.name()));
        }

        SocialClass random;
        do {
            random = random();
        } while ((minSocialClass != null && minSocialClass.getRank() > random.getRank()) ||
                (maxSocialClass != null && maxSocialClass.getRank() < random.getRank()));

        return random;
    }

    @NonNull
    public static List<SocialClass> listFromClassToClass(@NonNull SocialClass minSocialClass,
                                                   @NonNull SocialClass maxSocialClass) {
        List<SocialClass> results = new ArrayList<>();
        for (int i = minSocialClass.getRank(); i <= maxSocialClass.getRank(); i++) {
            results.add(SocialClass.fromRank(i));
        }
        return results;
    }

    /**
     * Generate a random social class weighted towards the lower end.
     */
    @NonNull
    public static SocialClass random() {
        int total = 27_000_000;
        Die dMillions = new Die(total);
        int val = dMillions.roll();
        total -= 2;
        if (val > total) {
            return SocialClass.MONARCH;
        }
        total -= 20;
        if (val > total) {
            return SocialClass.PRINCE;
        }
        total -= 80;
        if (val > total) {
            return SocialClass.DUKE;
        }
        total -= 80;
        if (val > total) {
            return SocialClass.MARQUESS;
        }
        total -= 350;
        if (val > total) {
            return SocialClass.EARL;
        }
        total -= 550;
        if (val > total) {
            return SocialClass.VISCOUNT;
        }
        total -= 5000;
        if (val > total) {
            return SocialClass.BARON;
        }
        total -= 50_000;
        if (val > total) {
            return SocialClass.BARONET;
        }
        total -= 500_000;
        if (val > total) {
            return SocialClass.GENTLEMAN;
        }
        total -= 3_000_000;
        if (val > total) {
            return SocialClass.YEOMAN_OR_MERCHANT;
        }
        total -= 8_000_000;
        if (val > total) {
            return SocialClass.LANDOWNER_OR_CRAFTSMAN;
        }
        total -= 9_000_000;
        if (val > total) {
            return SocialClass.LABORER;
        }
        return SocialClass.PAUPER;
    }
}
