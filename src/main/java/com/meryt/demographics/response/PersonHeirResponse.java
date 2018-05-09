package com.meryt.demographics.response;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;

@Getter
public class PersonHeirResponse extends PersonReference {

    private final LocalDate dateOfInheritance;
    private final Relationship relationship;

    public PersonHeirResponse(@NonNull Person heir,
                              @NonNull LocalDate dateOfInheritance,
                              @Nullable Relationship relationship) {
        super(heir);
        this.dateOfInheritance = dateOfInheritance;
        this.relationship = relationship;
    }
}
