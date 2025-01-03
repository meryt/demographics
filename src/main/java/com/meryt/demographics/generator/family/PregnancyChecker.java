package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.domain.person.fertility.Paternity;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.response.calendar.BirthEvent;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.response.calendar.ConceptionEvent;
import com.meryt.demographics.response.calendar.DeathEvent;
import com.meryt.demographics.response.calendar.MiscarriageEvent;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;

@Slf4j
public class PregnancyChecker {

    private static final double CHILDBIRTH_MATERNAL_DEATH_PROBABILITY = 0.02; // Sierra Leone rate
    private static final double MISCARRIAGE_DEATH_PROBABILITY = 0.002;
    private static final double GESTATION_STD_DEV = 2.41;
    private static final int HUMAN_GESTATION_DAYS = 266;

    private final PersonGenerator personGenerator;
    private final Person father;
    private final Person mother;
    private final Family family;
    private final boolean allowMaternalDeath;
    private final Maternity maternity;


    public PregnancyChecker(@NonNull PersonGenerator personGenerator,
                            @NonNull Family family,
                            boolean allowMaternalDeath) {
        this.family = family;
        this.father = family.getHusband();
        this.mother = family.getWife();
        if (this.mother == null) {
            throw new NullPointerException("The wife in the family record cannot be null");
        }
        this.allowMaternalDeath = allowMaternalDeath;
        this.maternity = (Maternity) mother.getFertility();
        if (this.maternity == null) {
            throw new IllegalArgumentException("The mother has no maternity record");
        }
        this.personGenerator = personGenerator;
    }

    /**
     * Loop over dates so long as the mother is living and check for births etc.
     */
    public List<CalendarDayEvent> checkDateRange(@NonNull LocalDate startDate,
                                                 @NonNull LocalDate endDate) {

        LocalDate currentDay = startDate;
        LocalDate endBeforeDate = endDate.plusDays(1);

        List<CalendarDayEvent> results = new ArrayList<>();
        while (currentDay.isBefore(endBeforeDate) && mother.isLiving(currentDay)) {
            results.addAll(checkDay(currentDay));
            currentDay = currentDay.plusDays(1);
        }
        return results;
    }

    private List<CalendarDayEvent> checkDay(@NonNull LocalDate day) {
        List<CalendarDayEvent> results = new ArrayList<>();
        if (maternity.isPregnant(day)) {
            if (day.isEqual(maternity.getDueDate())) {
                results.addAll(giveBirth(day));
            } else if (maternity.getMiscarriageDate() != null && day.isEqual(maternity.getMiscarriageDate())) {
                results.add(miscarry(day));
            }
        } else {
            CalendarDayEvent result = attemptConception(day);
            if (result != null) {
                results.add(result);
            }
        }

        // Advances the last-check-date etc.
        maternity.checkDay(day);
        return results;
    }

    private List<CalendarDayEvent> giveBirth(@NonNull LocalDate day) {
        List<CalendarDayEvent> results = new ArrayList<>(createChildren(day, maternity.isCarryingIdenticalTwins(),
                maternity.isCarryingFraternalTwins()));

        int numChildren = 1;
        if (maternity.isCarryingIdenticalTwins()) {
            numChildren++;
        }
        if (maternity.isCarryingFraternalTwins()) {
            numChildren++;
            maternity.setHadTwins(true);
        }
        maternity.setNumBirths(maternity.getNumBirths() + numChildren);

        if (maternity.getLastBirthDate() == null || day.isAfter(maternity.getLastBirthDate())) {
            maternity.setLastBirthDate(day);
        }

        if (allowMaternalDeath && PercentDie.roll() < CHILDBIRTH_MATERNAL_DEATH_PROBABILITY) {
            family.getWife().setDeathDate(day);
            family.getWife().setCauseOfDeath("childbirth");
            results.add(new DeathEvent(day, family.getWife()));
            log.info(String.format("%s died in childbirth on %s", family.getWife().getName(), day));
        }

        maternity.clearPregnancyFields();
        return results;
    }

    private CalendarDayEvent miscarry(@NonNull LocalDate day) {
        log.info(String.format("%s miscarried on %s", family.getWife().getName(), day));

        maternity.setNumMiscarriages(maternity.getNumMiscarriages() + 1);
        if (maternity.getLastBirthDate() == null || day.isAfter(maternity.getLastBirthDate())) {
            maternity.setLastBirthDate(day);
        }

        if (allowMaternalDeath && PercentDie.roll() < MISCARRIAGE_DEATH_PROBABILITY) {
            log.info(String.format("%s died due to a miscarriage on %s", family.getWife().getName(), day));
            family.getWife().setCauseOfDeath("miscarriage");
            family.getWife().setDeathDate(day);
        }

        maternity.clearPregnancyFields();
        return new MiscarriageEvent(day, family.getWife());
    }

    private List<CalendarDayEvent> createChildren(@NonNull LocalDate birthDate,
                                boolean includeIdenticalTwin,
                                boolean includeFraternalTwin) {
        List<CalendarDayEvent> results = new ArrayList<>();
        List<Person> children = personGenerator.generateChildrenForParents(family, birthDate,
                includeIdenticalTwin, includeFraternalTwin);
        maternity.setRandomBreastfeedingTillFromChildren(children);

        for (Person child : children) {
            log.info(String.format("%d %s gave birth on %s to %smale child named %s, fathered by %d %s",
                    family.getWife().getId(),
                    family.getWife().getName(),
                    birthDate,
                    child.isMale() ? "" : "fe",
                    child.getName(),
                    family.getHusband().getId(),
                    family.getHusband().getName()));
            results.add(new BirthEvent(birthDate, child, family.getHusband(), family.getWife()));
        }
        return results;
    }

    @Nullable
    private CalendarDayEvent attemptConception(@NonNull LocalDate day) {
        if (maternity.getFather() == null) {
            maternity.setFather(father);
        }
        if (maternity.getFrequencyFactor() <= 0 || !maternity.isHavingRelations() || father == null ||
            father.getFertility() == null || !father.isLiving(day.minusDays(3))) {
            return null;
        }

        double percentChance = maternity.getConceptionProbability(mother.getBirthDate(), day);
        percentChance *= ((Paternity) father.getFertility()).getAdjustedFertilityFactor(father.getAgeInDays(day));

        if (PercentDie.roll() < percentChance) {
            return conceive(father, day);
        }
        return null;
    }

    private CalendarDayEvent conceive(@NonNull Person father, @NonNull LocalDate day) {
        maternity.setConceptionDate(day);
        maternity.setFather(father);
        maternity.setDueDate(maternity.getConceptionDate().plusDays(getRandomGestation()));
        attemptMiscarriage(day);

        if (PercentDie.roll() < maternity.getFraternalTwinProbability(day, mother.getAgeInYears(day))) {
            maternity.setCarryingFraternalTwins(true);
            maternity.setDueDate(maternity.getDueDate().minusDays(15));
        }

        if (PercentDie.roll() < Maternity.IDENTICAL_TWIN_PROBABILITY) {
            maternity.setCarryingIdenticalTwins(true);
            maternity.setDueDate(maternity.getDueDate().minusDays(15));
        }

        log.debug(String.format("%s conceived on %s, due %s, to father %s",
                mother.getName(),
                day,
                maternity.getDueDate(),
                father.getName()));
        return new ConceptionEvent(day, mother, father, maternity.getDueDate());
    }

    private int getRandomGestation() {
        return (int) Math.round(new NormalDistribution(HUMAN_GESTATION_DAYS, GESTATION_STD_DEV).sample());
    }

    private void attemptMiscarriage(@NonNull LocalDate day) {
        double chanceMiscarriage = getMiscarriageProbabilityFirstEightWeeks(day);

        if (PercentDie.roll() < chanceMiscarriage) {
            int daysOut = new Die(7 * 8).roll() - 1;
            maternity.setMiscarriageDate(day.plusDays(daysOut));
            return;
        }

        chanceMiscarriage = getMiscarriageProbabilityEightToTwentyWeeks(day);

        if (PercentDie.roll() < chanceMiscarriage) {
            int daysOut = (new Die(7 * 12).roll() - 1) + (7 * 8);
            maternity.setMiscarriageDate(day.plusDays(daysOut));
        }

        // TODO possibly add chance of stillbirth beyond 20 weeks
    }

    private double getMiscarriageProbabilityFirstEightWeeks(@NonNull LocalDate day) {
        int age = mother.getAgeInYears(day);
        double ageMiscarriageFactor = age < 30 ? 0 : ((age - 30) / 20.0);
        double chanceMiscarriage = (ageMiscarriageFactor * 5 + 10) / 100.0;

        // Modify risk based on # of previous miscarriages; +5% per
        double riskFactor = 1 + (0.05 * maternity.getNumMiscarriages());
        chanceMiscarriage *= riskFactor;
        return chanceMiscarriage;
    }

    private double getMiscarriageProbabilityEightToTwentyWeeks(@NonNull LocalDate day) {
        int age = mother.getAgeInYears(day);
        double ageMiscarriageFactor = age < 30 ? 0 : ((age - 30) / 20.0);
        double chanceMiscarriage = (ageMiscarriageFactor * 8 + 2) / 100.0;

        // Modify risk based on # of previous miscarriages; +5% per
        double riskFactor = 1 + (0.05 * maternity.getNumMiscarriages());
        chanceMiscarriage *= riskFactor;
        return chanceMiscarriage;
    }
}
