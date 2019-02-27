package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.Comparator;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.title.Peerage;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;

@Getter
public class TitleReference {

    private final long id;
    private final String name;
    private final Peerage peerage;
    private final SocialClass socialClass;
    private final TitleInheritanceStyle inheritanceStyle;
    private final boolean extinct;
    private final LocalDate abeyanceCheckDate;
    private final LocalDate extinctionDate;
    private final PersonReference currentHolder;
    private final LocalDate creationDate;

    public TitleReference(@NonNull Title title) {
        this(title, null);
    }

    public TitleReference(@NonNull Title title, @Nullable LocalDate onDate) {
        this.id = title.getId();
        this.name = title.getName();
        this.peerage = title.getPeerage();
        this.socialClass = title.getSocialClass();
        this.inheritanceStyle = title.getInheritance();
        this.extinct = title.isExtinct();
        this.abeyanceCheckDate = title.getNextAbeyanceCheckDate();
        PersonTitlePeriod firstHolder = title.getTitleHolders().stream()
                .min(Comparator.comparing(PersonTitlePeriod::getFromDate))
                .orElse(null);
        this.creationDate = firstHolder == null ? null : firstHolder.getFromDate();

        PersonTitlePeriod lastHolder = title.getTitleHolders().stream()
                .max(Comparator.comparing(PersonTitlePeriod::getFromDate))
                .orElse(null);
        this.extinctionDate = (this.extinct && lastHolder != null) ? lastHolder.getToDate() : null;

        if (onDate == null) {
            currentHolder = null;
        } else {
            Person holder = title.getHolder(onDate);
            if (holder == null) {
                currentHolder = null;
            } else {
                currentHolder = new PersonReference(holder, onDate);
            }
        }
    }
}
