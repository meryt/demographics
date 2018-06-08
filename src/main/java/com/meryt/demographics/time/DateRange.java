package com.meryt.demographics.time;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.NonNull;

/**
 * A date range that is inclusive on its lower end (from) and exclusive on its upper. The upper part of the range may
 * be null.
 */
public interface DateRange {

    @NonNull
    LocalDate getFromDate();

    @Nullable
    LocalDate getToDate();

    default boolean contains(@NonNull LocalDate date) {
        return (getFromDate().isBefore(date) || getFromDate().equals(date))
                && (getToDate() == null || getToDate().isAfter(date));
    }

    /**
     * Determines whether a new date range starting at the given date and never ending would exactly equal this date
     * range.
     *
     * @param fromDate
     * @return true if the from dates match and the toDate of this date range is null
     */
    default boolean openEndedRangeEquals(@NonNull LocalDate fromDate) {
        return (getFromDate().equals(fromDate) && getToDate() == null);
    }

    /**
     * Returns true if the ranges exactly overlap. If either range is open-ended, they both must be.
     *
     * @param other the other date range
     * @return true iff the from dates and to dates are equal
     */
    default boolean rangeEquals(@NonNull DateRange other) {
        return (getFromDate().equals(other.getFromDate()) &&  (
                (getToDate() == null && other.getToDate() == null) ||
                        (getToDate() != null && getToDate().equals(other.getToDate()))));
    }

}
