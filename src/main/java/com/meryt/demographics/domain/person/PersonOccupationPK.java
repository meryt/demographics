package com.meryt.demographics.domain.person;

import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Embeddable;
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
}
