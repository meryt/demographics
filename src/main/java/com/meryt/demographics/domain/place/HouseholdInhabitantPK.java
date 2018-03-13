package com.meryt.demographics.domain.place;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public class HouseholdInhabitantPK {

    @Column(name = "person_id")
    private long personId;

    @Column(name = "from_date")
    private LocalDate fromDate;

}
