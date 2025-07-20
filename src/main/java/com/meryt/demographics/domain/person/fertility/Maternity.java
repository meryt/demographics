package com.meryt.demographics.domain.person.fertility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.time.LocalDateComparator;

@Entity
@Table(name = "maternities")
@Getter
@Setter
public class Maternity extends Fertility {

    // Chance of identical twins is same for all women
	public static final double IDENTICAL_TWIN_PROBABILITY = 0.004;
    private static final double FRATERNAL_TWIN_BASE_PROBABILITY = 0.01;
    // Should equal MONTHLY_CONCEPTION_PROBABILITY_WITH_WITHDRAWAL
    // divided by MONTHLY_CONCEPTION_PROBABILITY.
    // This is the "multiplier" factor used against base fertility chance when
    // using perfect withdrawal
	private static final double WITHDRAWAL_FACTOR = 0.0863;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "father_id", referencedColumnName = "id")
    private Person father;

    private LocalDate conceptionDate;
    private LocalDate miscarriageDate;
    private LocalDate dueDate;
    private LocalDate lastCycleDate;
    private LocalDate lastCheckDate;
    @Column(name = "identical_twins")
    private boolean carryingIdenticalTwins;
    @Column(name = "fraternal_twins")
    private boolean carryingFraternalTwins;
    @Getter
    @Setter
    private LocalDate breastfeedingTill;
    private boolean hadTwins;
    private int numBirths;
    private int numMiscarriages;
    private LocalDate lastBirthDate;
    private double frequencyFactor;
    private double withdrawalFactor;
    private boolean havingRelations;
    private int cycleLength = 28;

    public boolean isPregnant(@NonNull LocalDate onDate) {
        return null != conceptionDate && conceptionDate.isBefore(onDate) && null != dueDate
                && (dueDate.isAfter(onDate) || dueDate.isEqual(onDate));
    }

    public boolean mayGiveBirthBy(@NonNull LocalDate onDate) {
        if (lastCheckDate == null || lastCheckDate.isAfter(onDate)) {
            return false;
        }
        return isPregnant(lastCheckDate) && miscarriageDate == null;
    }

    /**
     * Sets a random breastfeeding-till date from an array of 1 or more children.
     *
     * It will take the death date of the longest-lived child, or a random date
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

    /**
     * Get the father ID (for JSON)
     */
    public Long getFatherId() {
        return father == null ? null : father.getId();
    }

    public void clearPregnancyFields() {
        setDueDate(null);
        setMiscarriageDate(null);
        setCarryingFraternalTwins(false);
        setCarryingIdenticalTwins(false);
        setConceptionDate(null);
    }

    @JsonIgnore
    public double getConceptionProbability(@NonNull LocalDate mothersBirthDate, @NonNull LocalDate day) {
        if (isPregnant(day) || isLastCycleDateNullOrInFuture(day)) {
            return 0;
        }

        double percentChance;
        if (lastBirthDate != null && day.isAfter(lastBirthDate)
                && LocalDateComparator.daysBetween(lastBirthDate, day) < 30) {
            percentChance = 0.00045;
        } else {
            percentChance = getDailyConceptionProbability(day);
        }

        if (0 < getWithdrawalFactor()) {
            double wd = getWithdrawalFactor();
            int numChild = Math.min(getNumBirths(), 5);
            if (numChild < 5) {
                wd /= (5 - numChild);
            }
            double wdFactor = 1 - wd * (1 - WITHDRAWAL_FACTOR);
            percentChance *= wdFactor;
        }

        // If she is breastfeeding there may be a multiplying factor that will
        // reduce percent chance
        percentChance *= getBreastfeedingFertilityFactor(day);
        // Age may affect fertility
        percentChance *= getAgeFertilityFactor(mothersBirthDate, day);

        // Apply the woman's base fertility factor. This is 1 for a perfectly
        // fertile woman, else a multiplier less than 1 to reduce percent chance
        percentChance *= getFertilityFactor();

        // Apply frequency factor
        percentChance *= getFrequencyFactor();

        // This is her chance with a perfectly fertile man
        return percentChance;
    }

    private double getDailyConceptionProbability(@NonNull LocalDate day) {
        if (getLastCycleDate() == null) {
            return 0;
        }
        LocalDate nextCycleDate = getLastCycleDate().plusDays(getCycleLength());
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

    private LocalDate getPrevCycleDate() {
        if (lastCycleDate == null) {
            return null;
        }
        return lastCycleDate.minusDays(cycleLength);
    }

    /**
     * Advances/retreats the last cycle date to near a given date.  If the current last cycle date is null, sets it to
     * the given date.
     *
     * @param toDate the target date
     * @param forceLessThan - if true, the last cycle date will be less than the given date.  If false it will be
     *                      greater than.
     */
    public void cycleToDate(@NonNull LocalDate toDate, boolean forceLessThan) {
        if (lastCycleDate == null || lastCycleDate.equals(toDate)) {
            setLastCycleDate(toDate);
        }

        if (lastCycleDate.isBefore(toDate)) {
            cycleForwardsToDate(toDate, forceLessThan);
        } else {
            cycleBackwardsToDate(toDate);
        }
    }

    public void cycleForwardsToDate(@NonNull LocalDate toDate, boolean forceLessThan) {
        while (lastCycleDate.isBefore(toDate)) {
            LocalDate d = getNextCycleDate();
            if (d == null) {
                break;
            }
            lastCycleDate = d;
        }
        if (forceLessThan && lastCycleDate.isAfter(toDate)) {
            setLastCycleDate(getPrevCycleDate());
        }
    }

    private void cycleBackwardsToDate(@NonNull LocalDate toDate) {
        while (lastCycleDate.isAfter(toDate)) {
            setLastCycleDate(getPrevCycleDate());
        }
    }

    @SuppressWarnings("squid:S3776") // cognitive complexity
    private double getBreastfeedingFertilityFactor(@NonNull LocalDate day) {
        if (getLastBirthDate() == null || getBreastfeedingTill() == null
                || day.isBefore(getLastBirthDate())) {
            return 1;
        }

        // Get the number of days she has been breast-feeding
        int bfSince = (int) LocalDateComparator.daysBetween(getLastBirthDate(), day);
        if (bfSince < 6 * 30)       { return 0.0112; }
        else if (bfSince < 7 * 30)  { return 0.05; }
        else if (bfSince < 8 * 30)  { return 0.15; }
        else if (bfSince < 9 * 30)  { return 0.25; }
        else if (bfSince < 10 * 30) { return 0.30; }
        else if (bfSince < 11 * 30) { return 0.35; }
        else if (bfSince < 12 * 30) { return 0.40; }
        else if (bfSince < 13 * 30) { return 0.45; }
        else if (bfSince < 15 * 30) { return 0.55; }
        else if (bfSince < 17 * 30) { return 0.65; }
        else if (bfSince < 19 * 30) { return 0.75; }
        else if (bfSince < 21 * 30) { return 0.85; }
        else if (bfSince < 23 * 30) { return 0.95; }
		else {
            return 1;
        }
    }

    @SuppressWarnings("squid:S3776") // cognitive complexity
    private double getAgeFertilityFactor(@NonNull LocalDate mothersBirthDate, @NonNull LocalDate day) {
        int ageInYears = mothersBirthDate.until(day).getYears();

        if      (25 > ageInYears) { return 1; }
        else if (30 > ageInYears) { return 0.95; }
        else if (35 > ageInYears) { return 0.83; }
        else if (36 > ageInYears) { return 0.75; }
        else if (37 > ageInYears) { return 0.70; }
        else if (38 > ageInYears) { return 0.65; }
        else if (39 > ageInYears) { return 0.60; }
        else if (40 > ageInYears) { return 0.55; }
        else if (41 > ageInYears) { return 0.50; }
        else if (42 > ageInYears) { return 0.45; }
        else if (43 > ageInYears) { return 0.40; }
        else if (44 > ageInYears) { return 0.30; }
        else if (45 > ageInYears) { return 0.25; }
        else if (46 > ageInYears) { return 0.15; }
        else if (47 > ageInYears) { return 0.10; }
        else if (48 > ageInYears) { return 0.05; }
		else { return 0; }
    }

}
