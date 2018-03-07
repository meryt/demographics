package com.meryt.demographics.domain.person;

import com.fasterxml.jackson.annotation.JsonValue;
import com.meryt.demographics.generator.random.Die;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

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
    MONARCH(13, "King, emperor, pope")
    ;

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

    /**
     * Gets a class from the 1-based index.
     * @param rank an index between 1 and 13 inclusive
     * @throws IllegalArgumentException if the rank is not in range
     * @throws IllegalStateException if the enum value and rank don't match
     */
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
     * Generate a random social class weighted towards the lower end.
     */
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
        total -= 1000;
        if (val > total) {
            return SocialClass.BARON;
        }
        total -= 5000;
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
