package com.meryt.demographics.request;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.rest.BadRequestException;

/**
 * A request that lets you either advance to a specific date or advance a specific number of days. Only one can be
 * selected.
 */
@Getter
@Setter
public class AdvanceToDatePost {
    private LocalDate date;
    private Integer advanceDays;
    private RandomFamilyParameters familyParameters;
    private Integer firstMonthOfYear;
    private Integer firstDayOfYear;
    /**
     * Number of days to check at once for maternities, since this is the most expensive part of the calculations.
     */
    private Integer maternityNumDays;
    /**
     * Number of days between ancestry rebuilds, if set. If null, rebuilds whenever children are born.
     */
    private Integer daysBetweenAncestryRebuild;
    /**
     * Percent chance, per year, that a new family will arrive in a parish. The check will be performed per day,
     * with this value divided by 365 to determine whether a new family moves into the parish.
     */
    private Double chanceNewFamilyPerYear;

    private List<String> suppressedEventTypes;
    private List<String> farmNames;
    private RandomTitleParameters titleParameters;

    private Boolean generateMarriages;
    private Boolean processImmigrants;
    private Boolean processQuarterDays;

    public void validate() {
        if (date == null && advanceDays == null) {
            throw new BadRequestException("One of date or advanceDays must be specified");
        }
        if (date != null && advanceDays != null) {
            throw new BadRequestException("Only one of date or advanceDays may be specified, not both");
        }
        if (advanceDays != null && advanceDays < 0) {
            throw new BadRequestException("advanceDays must be 0 or a positive integer");
        }
    }

    public int getFirstMonthOfYearOrDefault() {
        return firstMonthOfYear == null ? 1 : firstMonthOfYear;
    }

    public int getFirstDayOfYearOrDefault() {
        return firstDayOfYear == null ? 1 : firstDayOfYear;
    }

    public int getMaternityNumDaysOrDefault() {
        return maternityNumDays == null ? 1 : maternityNumDays;
    }

    public boolean isSuppressedEventType(@NonNull CalendarDayEvent event) {
        return suppressedEventTypes != null && !suppressedEventTypes.isEmpty()
                && suppressedEventTypes.contains(event.getType().name());
    }

    public List<String> getFarmNamesOrDefault() {
        return farmNames == null ? new ArrayList<>() : farmNames;
    }
}
