package com.meryt.demographics.domain.person;

import java.io.Serializable;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@Setter
@Embeddable
class PersonOccupationPK implements Serializable {
    @Column(name = "person_id")
    private long personId;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Override
    public String toString() {
        return String.format("personId=%d,fromDate=%s", personId, fromDate == null ? "null" : fromDate);
    }
}
