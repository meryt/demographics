package com.meryt.demographics.domain.person;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@Setter
@Embeddable
public class PersonTitlePK implements Serializable {
    @Column(name = "title_id")
    private long titleId;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Override
    public String toString() {
        return String.format("titleId=%d,fromDate=%s", titleId, fromDate == null ? "null" : fromDate);
    }

}

