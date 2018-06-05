package com.meryt.demographics.domain.place;

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
public class DwellingPlaceOwnerPK implements Serializable {
    @Column(name = "dwelling_place_id")
    private long dwellingPlaceId;

    @Column(name = "from_date")
    private LocalDate fromDate;
}
