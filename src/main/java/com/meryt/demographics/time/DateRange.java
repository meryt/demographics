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

}
