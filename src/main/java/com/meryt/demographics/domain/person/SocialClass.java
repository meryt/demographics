package com.meryt.demographics.domain.person;

import com.fasterxml.jackson.annotation.JsonValue;
import com.meryt.demographics.generator.Die;
import lombok.Getter;

public enum SocialClass {
    PAUPER("Itinerant laborer, beggar, prisoner"),
    LABORER("Laborer, servant, tenant farmer, sailor, soldier"),
    LANDOWNER_OR_CRAFTSMAN("Freehold farmer, craftsman, small merchant, footman, lady's maid, lieutenant"),
    YEOMAN_OR_MERCHANT("Yeoman farmer, merchant, clerk, steward, valet, butler, captain"),
    GENTLEMAN("Gentleman farmer, clergyman, doctor, lawyer, agent, military officer, major"),
    BARONET("Baronet, estate owner, knighted or high-ranking officer"),
    BARON("Baron, abbot, bishop, colonel, Knight of the Garter"),
    VISCOUNT("Viscount"),
    EARL("Earl, archbishop"),
    MARQUESS("Marquess"),
    DUKE("Duke, Cardinal"),
    PRINCE("Prince"),
    MONARCH("King, emperor, pope")
    ;

    @Getter
    @JsonValue
    private final String description;

    SocialClass(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return getDescription();
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
