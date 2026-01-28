package com.meryt.demographics.domain.person;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FirstName {
    private String name;
    private Gender gender;
    private int rank;
    private double weight;
    private String culture;
    private LocalDate fromDate;
    private LocalDate toDate;

    public FirstName(String name, String culture) {
        this.name = name;
        this.culture = culture;
    }
}
