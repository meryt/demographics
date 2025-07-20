package com.meryt.demographics.domain.place;

import java.io.Serializable;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode
@Getter
@Setter
@ToString
@Embeddable
public class DwellingPlaceOwnerPK implements Serializable {
    @Column(name = "dwelling_place_id")
    private long dwellingPlaceId;

    @Column(name = "person_id")
    private long personId;

    @Column(name = "from_date")
    private LocalDate fromDate;
}
