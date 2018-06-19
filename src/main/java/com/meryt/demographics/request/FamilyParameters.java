package com.meryt.demographics.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FamilyParameters {

    private Long husbandId;
    private Long wifeId;
    private LocalDate weddingDate;

}
