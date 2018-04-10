package com.meryt.demographics.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.title.Title;

@Getter
public class TitleResponse extends TitleReference {

    private final PersonReference inheritanceRoot;

    private final List<TitleHolderResponse> titleHolders = new ArrayList<>();

    public TitleResponse(@NonNull Title title) {
        super(title);
        this.inheritanceRoot = new PersonReference(title.getInheritanceRoot());
        for (PersonTitlePeriod titleHolderPeriod : title.getTitleHolders()) {
            titleHolders.add(new TitleHolderResponse(titleHolderPeriod));
        }
    }
}

