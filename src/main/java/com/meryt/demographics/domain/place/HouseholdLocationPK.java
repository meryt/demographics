package com.meryt.demographics.domain.place;

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
public class HouseholdLocationPK implements Serializable {

    @Column(name = "household_id")
    private long householdId;

    @Column(name = "from_date")
    private LocalDate fromDate;

}
