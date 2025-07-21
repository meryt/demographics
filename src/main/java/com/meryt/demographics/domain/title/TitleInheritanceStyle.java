package com.meryt.demographics.domain.title;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import com.meryt.demographics.generator.random.Die;

@ToString
public enum TitleInheritanceStyle {
    HEIRS_MALE_OF_THE_BODY(true),
    HEIRS_OF_THE_BODY(false),
    HEIRS_MALE_GENERAL(true),
    HEIRS_GENERAL(false),
    IRISH_KIN_GROUP(true);

    @Getter
    private final boolean malesOnly;

    TitleInheritanceStyle(boolean malesOnly) {
        this.malesOnly = malesOnly;
    }

    public static TitleInheritanceStyle random() {
        return random(Peerage.ENGLAND);
    }

    public static TitleInheritanceStyle random(@NonNull Peerage peerage) {
        if (peerage == Peerage.IRELAND) {
            return TitleInheritanceStyle.IRISH_KIN_GROUP;
        }
        TitleInheritanceStyle inheritanceStyle;
        int roll = new Die(4).roll();
        if (roll == 1) {
            inheritanceStyle = TitleInheritanceStyle.HEIRS_GENERAL;
        } else if (roll == 2) {
            inheritanceStyle = TitleInheritanceStyle.HEIRS_MALE_GENERAL;
        } else if (roll == 3) {
            inheritanceStyle = TitleInheritanceStyle.HEIRS_OF_THE_BODY;
        } else {
            inheritanceStyle = TitleInheritanceStyle.HEIRS_MALE_OF_THE_BODY;
        }
        return inheritanceStyle;
    }

    public static TitleInheritanceStyle randomFavoringMaleOnly() {
        TitleInheritanceStyle inheritanceStyle;
        int roll = new Die(100).roll();
        if (roll <= 5) {
            inheritanceStyle = TitleInheritanceStyle.HEIRS_GENERAL;
        } else if (roll <= 10) {
            inheritanceStyle = TitleInheritanceStyle.HEIRS_OF_THE_BODY;
        } else if (roll <= 55) {
            inheritanceStyle = TitleInheritanceStyle.HEIRS_MALE_GENERAL;
        } else {
            inheritanceStyle = TitleInheritanceStyle.HEIRS_MALE_OF_THE_BODY;
        }
        return inheritanceStyle;
    }
}
