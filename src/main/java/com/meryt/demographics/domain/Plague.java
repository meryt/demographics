package com.meryt.demographics.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.time.DateRange;

import static java.time.temporal.ChronoUnit.DAYS;

public class Plague implements DateRange {

    private static List<Plague> PLAGUES = new ArrayList<>();
    static {
        Plague testPlague = new Plague("famine", LocalDate.of(1291, 3, 1), LocalDate.of(1291, 9, 30), 0.02);
        PLAGUES.add(testPlague);

        Plague greatFamine = new Plague("Great Famine", LocalDate.of(1315, 9, 1), LocalDate.of(1317, 12, 31), 0.2);
        PLAGUES.add(greatFamine);

        Plague blackDeath = new Plague("Black Death", LocalDate.of(1350, 3, 1), LocalDate.of(1350, 9, 30), 0.3);
        PLAGUES.add(blackDeath);

        Plague plagueOfMen = new Plague("plague", LocalDate.of(1361, 9, 1), LocalDate.of(1362, 3, 31), 0.1);
        plagueOfMen.setPercentDeathChildFemale(0.06);
        plagueOfMen.setPercentDeathAdultFemale(0.03);
        PLAGUES.add(plagueOfMen);

        Plague plagueOfChildren = new Plague("plague", LocalDate.of(1369, 3, 1), LocalDate.of(1369, 12, 25), 0.01);
        plagueOfChildren.setPercentDeathChildMale(0.08);
        plagueOfChildren.setPercentDeathChildFemale(0.06);
        PLAGUES.add(plagueOfChildren);

        Plague fourthPestilence = new Plague("plague", LocalDate.of(1378, 3, 1), LocalDate.of(1378, 9, 30), 0.01);
        fourthPestilence.setPercentDeathChildMale(0.08);
        fourthPestilence.setPercentDeathChildFemale(0.08);
        PLAGUES.add(fourthPestilence);

        Plague plagueOfBoys = new Plague("plague", LocalDate.of(1378, 3, 1), LocalDate.of(1378, 12, 25), 0.01);
        plagueOfBoys.setPercentDeathChildMale(0.1);
        PLAGUES.add(plagueOfBoys);

        Plague plagueOfBoys2 = new Plague("plague", LocalDate.of(1390, 3, 1), LocalDate.of(1390, 12, 25), 0.01);
        plagueOfBoys2.setPercentDeathChildMale(0.6);
        PLAGUES.add(plagueOfBoys2);

        PLAGUES.add(new Plague("plague", LocalDate.of(1402, 3, 1), LocalDate.of(1403, 12, 31), 0.08));

        PLAGUES.add(new Plague("plague", LocalDate.of(1430, 3, 1), LocalDate.of(1430, 12, 31), 0.03));

        PLAGUES.add(new Plague("plague", LocalDate.of(1432, 3, 1), LocalDate.of(1432, 12, 31), 0.03));

        PLAGUES.add(new Plague("dysentery", LocalDate.of(1439, 1, 1), LocalDate.of(1439, 12, 31), 0.09));

        PLAGUES.add(new Plague("plague", LocalDate.of(1475, 1, 1), LocalDate.of(1475, 12, 31), 0.12));

        PLAGUES.add(new Plague("plague", LocalDate.of(1499, 1, 1), LocalDate.of(1500, 12, 31), 0.08));

        PLAGUES.add(new Plague("plague", LocalDate.of(1514, 1, 1), LocalDate.of(1515, 12, 31), 0.06));

        PLAGUES.add(new Plague("plague", LocalDate.of(1530, 1, 1), LocalDate.of(1530, 12, 31), 0.03));

        PLAGUES.add(new Plague("plague", LocalDate.of(1539, 1, 1), LocalDate.of(1539, 12, 31), 0.03));

        PLAGUES.add(new Plague("plague", LocalDate.of(1545, 1, 1), LocalDate.of(1546, 12, 31), 0.1));

        PLAGUES.add(new Plague("plague", LocalDate.of(1568, 3, 1), LocalDate.of(1568, 12, 31), 0.05));

        PLAGUES.add(new Plague("plague", LocalDate.of(1576, 9, 1), LocalDate.of(1577, 6, 30), 0.1));

        PLAGUES.add(new Plague("plague", LocalDate.of(1584, 1, 1), LocalDate.of(1588, 6, 30), 0.03));

        PLAGUES.add(new Plague("plague", LocalDate.of(1598, 9, 1), LocalDate.of(1600, 3, 31), 0.12));

        PLAGUES.add(new Plague("plague", LocalDate.of(1645, 1, 1), LocalDate.of(1646, 1, 31), 0.08));
    }

    /**
     * Gets a plague that was happening on the given date, if any. Assumes that no plagues overlap; otherwise only
     * one will be returned.
     *
     * @return a plague that was underway on the given date, or else null
     */
    @Nullable
    public static Plague getPlagueForDate(@NonNull LocalDate date) {
        return PLAGUES.stream()
                .filter(p -> p.contains(date))
                .findAny()
                .orElse(null);
    }

    @Getter
    private final String name;
    @Getter
    private final LocalDate fromDate;
    @Getter
    private final LocalDate toDate;
    private double percentDeathAdultMale;
    @Setter(AccessLevel.PRIVATE)
    private double percentDeathAdultFemale;
    @Setter(AccessLevel.PRIVATE)
    private double percentDeathChildMale;
    @Setter(AccessLevel.PRIVATE)
    private double percentDeathChildFemale;
    @Getter(AccessLevel.PACKAGE)
    private final long daysLength;

    private Plague(@NonNull String name, @NonNull LocalDate fromDate, @NonNull LocalDate toDate, double percentDeath) {
        this.name = name;
        this.fromDate = fromDate;
        this.toDate = toDate;
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be strictly before toDate");
        }
        setPercentDeathForAll(percentDeath);
        // Add a day because we check both start date and end date (i.e. end date is inclusive)
        daysLength = DAYS.between(fromDate, toDate) + 1;
    }

    public boolean didPersonDieOnDate(@NonNull Person person, @NonNull LocalDate onDate) {
        if (daysLength == 0 || !person.isLiving(onDate)) {
            return false;
        }

        double dailyChanceOfDying = 1 - Math.pow((1 - getOverallChanceOfDying(person, onDate)), 1.0 / (double) daysLength);
        return PercentDie.roll() <= dailyChanceOfDying;
    }

    /**
     * Get the chance of dying within the period of the plague. I.e. if 20% of the population died during the plague,
     * the return value should be 0.2.
     *
     * @param person the person to test (the age and gender may be taken into account)
     * @param onDate the date on which to check (determines the age)
     * @return the chance of dying within the entire period
     */
    double getOverallChanceOfDying(@NonNull Person person, @NonNull LocalDate onDate) {
        double overallChanceOfDying;
        if (person.getAgeInYears(onDate) >= 18) {
            overallChanceOfDying = person.isMale() ? percentDeathAdultMale : percentDeathAdultFemale;
        } else {
            overallChanceOfDying = person.isMale() ? percentDeathChildMale : percentDeathChildFemale;
        }
        return overallChanceOfDying;
    }

    private void setPercentDeathForAll(double percentDeath) {
        percentDeathAdultMale = percentDeath;
        percentDeathAdultFemale = percentDeath;
        percentDeathChildMale = percentDeath;
        percentDeathChildFemale = percentDeath;
    }

}
