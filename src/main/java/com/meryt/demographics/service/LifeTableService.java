package com.meryt.demographics.service;

import com.meryt.demographics.generator.Die;
import com.meryt.demographics.repository.LifeTableRepository;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LifeTableService {

    public enum LifeTablePeriod {
        VICTORIAN
    }

    private final LifeTableRepository lifeTableRepository;

    public LifeTableService(@Autowired LifeTableRepository lifeTableRepository) {
        this.lifeTableRepository = lifeTableRepository;
    }

    /**
     * Get a random life expectancy in days
     * @return life expectancy in days
     */
    public long randomLifeExpectancy(@NonNull LifeTablePeriod period) {
        return randomLifeExpectancy(period, 0, null);
    }

    /**
     * Get a random life expectancy in days such that the age in years is less than or equal to the specified age
     * @return life expectancy in days
     */
    public long randomLifeExpectancy(@NonNull LifeTablePeriod period, @NonNull Integer maxAgeYears) {
        return randomLifeExpectancy(period, 0, maxAgeYears);
    }

    /**
     * Get a random life expectancy such that the age in years is greater than or equal to the min and less than or
     * equal to the max.
     * @return life expectancy in days
     */
    public long randomLifeExpectancy(@NonNull LifeTablePeriod period, @NonNull Integer minAgeYears, Integer maxAgeYears) {

        if (maxAgeYears != null && minAgeYears > maxAgeYears) {
            throw new IllegalArgumentException(String.format("minAgeYears (%d) cannot be less than maxAgeYears (%d)",
                    minAgeYears, maxAgeYears));
        }

        // If min and max are identical, return an age somewhere between the specified birthday and the next
        if (maxAgeYears != null && minAgeYears.equals(maxAgeYears)) {
            return (minAgeYears * 365) + new Die(365).roll() - 1;
        }

        double[] lx = lifeTableRepository.getLxValues(period.name().toLowerCase());

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
