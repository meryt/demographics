package com.meryt.demographics.domain.person.fertility;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.time.LocalDateComparator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class Maternity extends Fertility {

    // Chance of identical twins is same for all women
	public static final double IDENTICAL_TWIN_PROBABILITY = 0.004;
    private static final double FRATERNAL_TWIN_BASE_PROBABILITY = 0.01;

    private Person father;
    private LocalDate conceptionDate;
    private LocalDate miscarriageDate;
    private LocalDate dueDate;
    private LocalDate lastCycleDate;
    private LocalDate lastCheckDate;
    private boolean carryingIdenticalTwins;
    private boolean carryingFraternalTwins;
    private LocalDate breastfeedingTill;
    private boolean hadTwins;
    private int numBirths;
    private int numMiscarriages;
    private LocalDate lastBirthDate;
    private double frequencyFactor;
    private boolean havingRelations;

    public boolean isPregnant(@NonNull LocalDate onDate) {
        return null != conceptionDate && conceptionDate.isBefore(onDate) && null != dueDate
                && (dueDate.isAfter(onDate) || dueDate.isEqual(onDate));
    }

    /**
     * Sets a random breastfeeding-till date from an array of 1 or more children.
     *
     * It will take the deathdate of the longest-lived child, or a random date
     * between 1 or 2 years, whichever comes first.
     */
    public void setRandomBreastfeedingTillFromChildren(@NonNull List<Person> children) {
        LocalDate highestDeathDate = LocalDateComparator.max(children.stream()
                .map(Person::getDeathDate)
                .collect(Collectors.toList()));
        LocalDate highestBirthDate = LocalDateComparator.max(children.stream()
                .map(Person::getBirthDate)
                .collect(Collectors.toList()));

        if (highestBirthDate == null || highestDeathDate == null) {
            return;
        }

        breastfeedingTill = LocalDateComparator.min(Arrays.asList(highestDeathDate,
                highestBirthDate.plusDays(365 + new Die(365).roll())));
    }

    public void clearPregnancyFields() {
        setDueDate(null);
        setMiscarriageDate(null);
        setCarryingFraternalTwins(false);
        setCarryingIdenticalTwins(false);
        setConceptionDate(null);
    }

    @JsonIgnore
    public double getConceptionProbability(@NonNull LocalDate day) {
        if (isPregnant(day) || isLastCycleDateNullOrInFuture(day)) {
            return 0;
        }

        if (lastBirthDate != null && day.isAfter(lastBirthDate)
                && LocalDateComparator.daysBetween(lastBirthDate, day) < 30) {
            return 0.00045;
        } else {
            return getDailyConceptionProbability(day);
        }
    }

    private double getDailyConceptionProbability(@NonNull LocalDate day) {
        if (getLastCycleDate() == null) {
            return 0;
        }
        LocalDate nextCycleDate = getLastCycleDate().plusDays(28);
        int daysBeforeCycle = (int) LocalDateComparator.daysBetween(day, nextCycleDate);
        switch (daysBeforeCycle) {
            case 10: return 0.02;
            case 11: return 0.05;
            case 12: return 0.09;
            case 13: return 0.15;
            case 14: return 0.26;
            case 15: return 0.20;
            case 16: return 0.14;
            case 17: return 0.11;
            case 18: return 0.04;
            case 19: return 0.01;
            default: return 0.007;
        }
    }

    private boolean isLastCycleDateNullOrInFuture(@NonNull LocalDate day) {
        return getLastCycleDate() == null || getLastCycleDate().isAfter(day);
    }

    public double getFraternalTwinProbability(@NonNull LocalDate day, int motherAgeInYears) {
        double chance = FRATERNAL_TWIN_BASE_PROBABILITY;

        // Breast-feeding at conception increases chance
        if (getBreastfeedingTill() != null && day.isBefore(getBreastfeedingTill())) {
            chance *= 2;
        }

        // TODO Maternal family history of twins increases chance

        // Having had twins herself increases chance
        if (hadTwins) {
            chance *= 2;
        }

        // Number of births increases chance
        chance *= 1 + (getNumBirths() * 0.25);

        // Age above 25 increases chance
        chance *= (motherAgeInYears >= 25 ? ((motherAgeInYears - 15) * 0.1) : 1);

        return chance;
    }

    /**
     * Called by checker to Update any conditions that have expired
     */
    public void checkDay(@NonNull LocalDate day) {
        if (getBreastfeedingTill() != null && day.isAfter(getBreastfeedingTill())) {
            setBreastfeedingTill(null);
        }

        LocalDate nextCycleDate = getNextCycleDate();
        if (nextCycleDate != null && nextCycleDate.isEqual(day)) {
            setLastCycleDate(day);
        }

        setLastCheckDate(day);
    }

    private LocalDate getNextCycleDate() {
        if (getLastCycleDate() == null) {
            return null;
        }

        return lastCycleDate.plusDays(28);
    }

}
