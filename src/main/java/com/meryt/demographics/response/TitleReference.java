package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.title.Peerage;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;

@Getter
public class TitleReference {

    private final long id;
    private final String name;
    private final Peerage peerage;
    private final TitleInheritanceStyle inheritanceStyle;

    public TitleReference(@NonNull Title title) {
        this.id = title.getId();
        this.name = title.getName();
        this.peerage = title.getPeerage();
        this.inheritanceStyle = title.getInheritance();
    }
}
