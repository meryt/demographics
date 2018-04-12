package com.meryt.demographics.request;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonParameters {

    private Gender gender;
    /**
     * If set, ensures the person was alive on this date
     */
    private LocalDate aliveOnDate;
    /**
     * If aliveOnDate is set, the person will be at least this old on that date. Otherwise the person will live to
     * at least this age.
     */
    private Integer minAge;
    /**
     * If aliveOnDate is set, the person will be no more than this old on that date. Otherwise the person will live
     * no older than this age.
     */
    private Integer maxAge;

    /**
     * If set, this birth date will be used. If aliveOnDate is also used, the person will be alive on that date.
     */
    private LocalDate birthDate;

    /**
     * If set, this last name will be used.
     */
    private String lastName;

    /**
     * If non-empty, the following names will not be used
     */
    private Set<String> excludeNames = new HashSet<>();

    private Person father;

    private Person mother;

    private SocialClass minSocialClass;

    private SocialClass maxSocialClass;

    public void validate() {
        if (birthDate == null && aliveOnDate == null) {
            throw new IllegalArgumentException("Cannot generate a person without at least one of birthDate or aliveOnDate");
        }
    }

}
