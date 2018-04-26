package com.meryt.demographics.domain.title;

import lombok.Getter;

public enum TitleInheritanceStyle {
    HEIRS_MALE_OF_THE_BODY(true),
    HEIRS_OF_THE_BODY(false),
    HEIRS_MALE_GENERAL(true),
    HEIRS_GENERAL(false);

    @Getter
    private final boolean malesOnly;

    TitleInheritanceStyle(boolean malesOnly) {
        this.malesOnly = malesOnly;
    }
}
