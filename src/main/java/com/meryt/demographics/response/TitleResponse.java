package com.meryt.demographics.response;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.service.AncestryService;

@Getter
public class TitleResponse extends TitleReference {

    private final PersonReference inheritanceRoot;

    private final List<TitleHolderResponse> titleHolders = new ArrayList<>();

    private final List<RelatedPersonResponse> heirs;

    public TitleResponse(@NonNull Title title,
                         @Nullable AncestryService ancestryService,
                         @Nullable List<RelatedPersonResponse> heirs) {
        super(title);
        if (title.getInheritanceRoot() != null) {
            this.inheritanceRoot = new PersonReference(title.getInheritanceRoot());
        } else {
            this.inheritanceRoot = null;
        }
        Person previous = null;
        for (PersonTitlePeriod titleHolderPeriod : title.getTitleHolders()) {
            titleHolders.add(new TitleHolderResponse(titleHolderPeriod, previous, ancestryService));
            previous = titleHolderPeriod.getPerson();
        }
        this.heirs = heirs;
    }
}

