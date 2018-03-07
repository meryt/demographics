package com.meryt.demographics.time;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;
import lombok.NonNull;

public class LocalDateComparator {

    private LocalDateComparator() {
        // hide constructor of private class
    }

    public static LocalDate min(@NonNull Collection<LocalDate> dates) {
        if (dates.isEmpty()) {
            return null;
        }
        Optional<LocalDate> lowest = dates.stream()
                .min(LocalDate::compareTo);
        return lowest.orElse(null);
    }

    public static LocalDate max(@NonNull Collection<LocalDate> dates) {
        if (dates.isEmpty()) {
            return null;
        }
        Optional<LocalDate> highest = dates.stream()
                .max(LocalDate::compareTo);
        return highest.orElse(null);
    }

    /**
     * Gets the number of days between fromDate and toDate. If toDate is before fromDate, the number will be negative.
     */
    public static long daysBetween(@NonNull LocalDate fromDate, @NonNull LocalDate toDate) {
        return fromDate.until(toDate, ChronoUnit.DAYS);
    }
}
