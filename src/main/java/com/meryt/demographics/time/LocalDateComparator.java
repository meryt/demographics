package com.meryt.demographics.time;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;

import com.meryt.demographics.domain.place.HouseholdLocationPeriod;

public class LocalDateComparator {

    private LocalDateComparator() {
        // hide constructor of private class
    }

    public static LocalDate min(LocalDate... dates) {
        return min(Arrays.asList(dates));
    }

    public static LocalDate min(@NonNull Collection<LocalDate> dates) {
        if (dates.isEmpty()) {
            return null;
        }
        Optional<LocalDate> lowest = dates.stream()
                .min(LocalDate::compareTo);
        return lowest.orElse(null);
    }

    public static LocalDate max(LocalDate... dates) {
        return max(Arrays.asList(dates));
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

    public static boolean firstIsOnOrBeforeSecond(@NonNull LocalDate first, @NonNull LocalDate second) {
        return first.isEqual(second) || first.isBefore(second);
    }

    public static List<? extends DateRange> getRangesWithinRange(@NonNull List<? extends DateRange> ranges,
                                                                 @NonNull LocalDate fromDate,
                                                                 @Nullable LocalDate toDate) {
        return ranges.stream()
                .filter(period ->
                        // Simplest case: the fromDate is on the period, or the toDate is non-null and on the period
                        (period.contains(fromDate) || (toDate != null && period.contains(toDate))

                                // if the toDate is null (meaning return all results starting from fromDate) just check
                                // that the fromDate is before the period's fromDate
                                || (toDate == null && period.getFromDate().isAfter(fromDate))

                                // The to date is not null, and the period is after the from Date. So return the period
                                // if the period's toDate is null (meaning the period lasts forever) or its toDate is
                                // before the requested toDate
                                || (toDate != null
                                        && period.getFromDate().isAfter(fromDate)
                                        && period.getFromDate().isBefore(toDate)
                                        && (period.getToDate() == null
                                                || period.getToDate().isBefore(toDate)))))
                        .collect(Collectors.toList());
    }

}
