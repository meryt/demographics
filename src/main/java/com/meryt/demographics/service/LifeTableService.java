package com.meryt.demographics.service;

import java.time.LocalDate;
import javax.annotation.Nullable;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.repository.LifeTableRepository;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LifeTableService {

    private enum LifeTablePeriod {
        VICTORIAN,
        MEDIEVAL
    }

    private final LifeTableRepository lifeTableRepository;

    public LifeTableService(@Autowired LifeTableRepository lifeTableRepository) {
        this.lifeTableRepository = lifeTableRepository;
    }

    /**
     * Get a random life expectancy such that the age in years is greater than or equal to the min and less than or
     * equal to the max.
     * @return life expectancy in days
     */
    public long randomLifeExpectancy(@NonNull LocalDate birthDate,
                                     @Nullable Integer minAgeYears,
                                     @Nullable Integer maxAgeYears,
                                     @Nullable Gender gender) {
        LifeTablePeriod period;
        if (birthDate.isAfter(LocalDate.of(1800,1,1))) {
            period = LifeTablePeriod.VICTORIAN;
        } else {
            period = LifeTablePeriod.MEDIEVAL;
        }

        return randomLifeExpectancy(period, minAgeYears, maxAgeYears, gender);
    }

    /**
     * Get a random life expectancy such that the age in years is greater than or equal to the min and less than or
     * equal to the max.
     * @return life expectancy in days
     */
    private long randomLifeExpectancy(@NonNull LifeTablePeriod period,
                                     @Nullable Integer minAgeYears,
                                     @Nullable Integer maxAgeYears,
                                     @Nullable Gender gender) {

        if (minAgeYears == null) {
            minAgeYears = 0;
        }

        if (maxAgeYears != null && minAgeYears > maxAgeYears) {
            throw new IllegalArgumentException(String.format("minAgeYears (%d) cannot be less than maxAgeYears (%d)",
                    minAgeYears, maxAgeYears));
        }

        // If min and max are identical, return an age somewhere between the specified birthday and the next
        if (minAgeYears.equals(maxAgeYears)) {
            return (minAgeYears * 365) + new Die(365).roll() - 1;
        }

        double[] lx = lifeTableRepository.getLxValues(period.name().toLowerCase(), gender);

        double maxLx = 0.0;
        if (maxAgeYears != null && maxAgeYears < lx.length) {
            maxLx = lx[maxAgeYears];
        }

        if (minAgeYears >= lx.length) {
            throw new IllegalArgumentException(String.format(
                    "Min Age %d is greater than the max age (%d) in the requested life table", minAgeYears, lx.length));
        }

        double minLx = lx[minAgeYears];

        // Get a random value between the min lx and max lx
        double num;
        do {
            num = Math.random();
        } while (minLx < num || maxLx > num);

        int age = 0;
        for (; age < lx.length; age++) {
            if (lx[age] < num) {
                break;
            }
        }

        if (0 == age) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "Unable to determine a random age for minimum age of %d years", minAgeYears));
        }

        // Get the age granted by number of whole years lived, plus a random number of days after, before the next
        // birthday.
        return (age * (365 - 1)) + (new Die(365).roll() - 1);
    }

}
