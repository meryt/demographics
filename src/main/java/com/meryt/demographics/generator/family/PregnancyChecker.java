package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import java.util.List;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.domain.person.fertility.Paternity;
import com.meryt.demographics.generator.person.PersonGenerator;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.generator.random.PercentDie;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;

@Slf4j
public class PregnancyChecker {

    private static final double CHILDBIRTH_MATERNAL_DEATH_PROBABILITY = 0.02; // Sierra Leone rate
    private static final double MISCARRIAGE_DEATH_PROBABILITY = 0.002;
    private static final double GESTATION_STD_DEV = 2.41;
    private static final int HUMAN_GESTATION_DAYS = 266;

    private final PercentDie die;
    private final PersonGenerator personGenerator;
    private final Person father;
    private final Person mother;
    private final Family family;
    private final boolean allowMaternalDeath;
    private final Maternity maternity;


    public PregnancyChecker(@NonNull PersonGenerator personGenerator,
                            @NonNull Family family,
                            boolean allowMaternalDeath) {
        this.die = new PercentDie();
        this.family = family;
        this.father = family.getHusband();
        this.mother = family.getWife();
        if (this.father == null) {
            throw new NullPointerException("The husband in the family record cannot be null");
        }
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
    public void checkDateRange(@NonNull LocalDate startDate,
                               @NonNull LocalDate endDate) {

        LocalDate currentDay = startDate;
        LocalDate endBeforeDate = endDate.plusDays(1);

        while (currentDay.isBefore(endBeforeDate) && mother.isLiving(currentDay)) {
            checkDay(currentDay);
            currentDay = currentDay.plusDays(1);
        }
    }

    private void checkDay(@NonNull LocalDate day) {
        if (maternity.isPregnant(day)) {
            if (day.isEqual(maternity.getDueDate())) {
                giveBirth(day);
            } else if (maternity.getMiscarriageDate() != null && day.isEqual(maternity.getMiscarriageDate())) {
                miscarry(day);
            }
        } else {
            attemptConception(day);
        }

        // Advances the last-check-date etc.
        maternity.checkDay(day);
    }

    private void giveBirth(@NonNull LocalDate day) {
        createChildren(day, maternity.isCarryingIdenticalTwins(), maternity.isCarryingFraternalTwins());

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

        if (allowMaternalDeath && new PercentDie().roll() < CHILDBIRTH_MATERNAL_DEATH_PROBABILITY) {
            family.getWife().setDeathDate(day);
            log.info(String.format("%s died in childbirth on %s", family.getWife().getName(), day));
        }

        maternity.clearPregnancyFields();
    }

    private void miscarry(@NonNull LocalDate day) {
        log.info(String.format("%s miscarried on %s", family.getWife().getName(), day));

        maternity.setNumMiscarriages(maternity.getNumMiscarriages() + 1);
        if (maternity.getLastBirthDate() == null || day.isAfter(maternity.getLastBirthDate())) {
            maternity.setLastBirthDate(day);
        }

        if (allowMaternalDeath && new PercentDie().roll() < MISCARRIAGE_DEATH_PROBABILITY) {
            log.info(String.format("%s died due to a miscarriage on %s", family.getWife().getName(), day));
            family.getWife().setDeathDate(day);
        }

        maternity.clearPregnancyFields();
    }

    private void createChildren(@NonNull LocalDate birthDate,
                                boolean includeIdenticalTwin,
                                boolean includeFraternalTwin) {
        List<Person> children = personGenerator.generateChildrenForParents(family, birthDate,
                includeIdenticalTwin, includeFraternalTwin);
        maternity.setRandomBreastfeedingTillFromChildren(children);

        for (Person child : children) {
            log.info(String.format("%s gave birth on %s to %smale child named %s, fathered by %s",
                    family.getWife().getName(),
                    birthDate,
                    child.isMale() ? "" : "fe",
                    child.getName(),
                    family.getHusband().getName()));
        }
    }

    private void attemptConception(@NonNull LocalDate day) {
        if (maternity.getFather() == null) {
            maternity.setFather(father);
        }
        if (maternity.getFrequencyFactor() <= 0 || !maternity.isHavingRelations() || father == null ||
            father.getFertility() == null || !father.isLiving(day.minusDays(3))) {
            return;
        }

        double percentChance = maternity.getConceptionProbability(mother.getBirthDate(), day);
        percentChance *= ((Paternity) father.getFertility()).getAdjustedFertilityFactor(father.getAgeInDays(day));

        if (new PercentDie().roll() < percentChance) {
            conceive(father, day);
        }
    }

    private void conceive(@NonNull Person father, @NonNull LocalDate day) {
        maternity.setConceptionDate(day);
        maternity.setFather(father);
        maternity.setDueDate(maternity.getConceptionDate().plusDays(getRandomGestation()));
        attemptMiscarriage(day);

        if (die.roll() < maternity.getFraternalTwinProbability(day, mother.getAgeInYears(day))) {
            maternity.setCarryingFraternalTwins(true);
            maternity.setDueDate(maternity.getDueDate().minusDays(15));
        }

        if (die.roll() < Maternity.IDENTICAL_TWIN_PROBABILITY) {
            maternity.setCarryingIdenticalTwins(true);
            maternity.setDueDate(maternity.getDueDate().minusDays(15));
        }

        log.debug(String.format("%s conceived on %s, due %s, to father %s",
                mother.getName(),
                day,
                maternity.getDueDate(),
                father.getName()));
    }

    private int getRandomGestation() {
        return (int) Math.round(new NormalDistribution(HUMAN_GESTATION_DAYS, GESTATION_STD_DEV).sample());
    }

    private void attemptMiscarriage(@NonNull LocalDate day) {
        double chanceMiscarriage = getMiscarriageProbabilityFirstEightWeeks(day);

        if (die.roll() < chanceMiscarriage) {
            int daysOut = new Die(7 * 8).roll() - 1;
            maternity.setMiscarriageDate(day.plusDays(daysOut));
            return;
        }

        chanceMiscarriage = getMiscarriageProbabilityEightToTwentyWeeks(day);

        if (die.roll() < chanceMiscarriage) {
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
