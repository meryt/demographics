package com.meryt.demographics.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.meryt.demographics.domain.person.Gender;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PersonParameters {

    private static final LocalDate DEFAULT_ALIVE_ON = LocalDate.of(1700, 1, 1);

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

    @JsonIgnore
    @NonNull
    public LocalDate getAliveOnDateOrDefault() {
        return aliveOnDate == null ? DEFAULT_ALIVE_ON : aliveOnDate;
    }

}
