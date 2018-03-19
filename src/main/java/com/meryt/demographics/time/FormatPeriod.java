package com.meryt.demographics.time;

import lombok.NonNull;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;

public class FormatPeriod {

    private FormatPeriod() {
        // static class has private constructor
    }

    @NonNull
    public static String asYearsMonthsDays(@NonNull Period diff) {
        ArrayList<String> s = new ArrayList<>();
        if (diff.getYears() != 0) {
            s.add(diff.getYears() + " years");
        }
        if (diff.getMonths() != 0) {
            s.add(diff.getMonths() + " months");
        }
        if (diff.getDays() != 0) {
            s.add(diff.getDays() + " days");
        }
        if (s.isEmpty()) {
            return "0 days";
        }
        return String.join(", ", s);
    }

    @NonNull
    public static String diffAsYearsMonthsDays(@NonNull LocalDate fromDate, @NonNull LocalDate toDate) {
        return asYearsMonthsDays(Period.between(fromDate, toDate));
    }
}
