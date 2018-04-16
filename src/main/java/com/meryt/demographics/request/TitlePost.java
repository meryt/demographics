package com.meryt.demographics.request;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.title.Peerage;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;
import com.meryt.demographics.service.PersonService;

@Getter
@Setter
public class TitlePost {

    private String name;

    private SocialClass socialClass;

    private Peerage peerage;

    private TitleInheritanceStyle inheritance;

    private Long inheritanceRoot;

    public Title toTitle(@Nullable PersonService personService) {
        Title title = new Title();
        title.setName(name);
        title.setSocialClass(socialClass);
        title.setPeerage(peerage);
        title.setInheritance(inheritance);
        if (inheritanceRoot != null) {
            title.setInheritanceRoot(personService.load(inheritanceRoot));
        }
        return title;
    }
}