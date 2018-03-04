package com.meryt.demographics.request;

import com.meryt.demographics.domain.person.Gender;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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

}
