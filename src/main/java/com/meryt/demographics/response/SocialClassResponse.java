package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.SocialClass;

@Getter
public class SocialClassResponse {

    private final String name;
    private final String friendlyName;
    private final int rank;
    private final String description;

    public SocialClassResponse(@NonNull SocialClass socialClass) {
        this.name = socialClass.name();
        this.friendlyName = socialClass.getFriendlyName();
        this.rank = socialClass.getRank();
        this.description = socialClass.getDescription();
    }
}
