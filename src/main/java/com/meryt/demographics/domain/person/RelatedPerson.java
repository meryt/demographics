package com.meryt.demographics.domain.person;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Relationship;

@Getter
@AllArgsConstructor
public class RelatedPerson {
    @NonNull
    private final Person person;
    @Nullable
    private final Relationship relationship;
}
