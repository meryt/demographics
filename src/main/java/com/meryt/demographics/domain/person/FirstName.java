package com.meryt.demographics.domain.person;

import java.time.LocalDate;
import lombok.Data;

@Data
public class FirstName {
    private String name;
    private Gender gender;
    private int rank;
    private double weight;
    private String culture;
    private LocalDate fromDate;
    private LocalDate toDate;
}
