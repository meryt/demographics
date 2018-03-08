package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.domain.person.fertility.Paternity;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.math.FunkyBetaDistribution;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.LifeTableService;
import com.meryt.demographics.service.NameService;

@Service
public class PersonGenerator {

    private static final double CHILDBIRTH_DEATH_PROBABILITY  = 0.02;
    private static final double RAND_DOMESTICITY_ALPHA        = 4;
	private static final double RAND_DOMESTICITY_BETA         = 5;
    private static final double RAND_FERTILITY_ALPHA          = 2.5;
    private static final double RAND_FERTILITY_BETA           = 5;
    private static final double RAND_FIRST_PERIOD_ALPHA		  = 1.1;
    private static final double RAND_FIRST_PERIOD_BETA		  = 5;
    private static final double RAND_FREQUENCY_ALPHA          = 4; // 4:5 makes a sort of bell curve
    private static final double RAND_FREQUENCY_BETA           = 5;
    private static final double RAND_WITHDRAWAL_ALPHA         = 1.1;
    private static final double RAND_WITHDRAWAL_BETA          = 9;


    private static final int  FIRST_PERIOD_BASE_MIN_AGE_YEARS = 11;
    private static final int  FIRST_PERIOD_BASE_MAX_AGE_YEARS = 16;


    private static final BetaDistribution DOMESTICITY_BETA = new FunkyBetaDistribution(RAND_DOMESTICITY_ALPHA,
            RAND_DOMESTICITY_BETA);
	private static final BetaDistribution TRAIT_BETA = new BetaDistribution(2, 1.8);

    private final NameService nameService;
    private final LifeTableService lifeTableService;
    private final FamilyService familyService;

    PersonGenerator(@Autowired NameService nameService,
                           @Autowired LifeTableService lifeTableService,
                           @Autowired FamilyService familyService) {
        this.nameService = nameService;
        this.lifeTableService = lifeTableService;
        this.familyService = familyService;
    }

    public Person generate(PersonParameters personParameters) {
        Person person = new Person();
        person.setGender(personParameters.getGender() == null ? Gender.random() : personParameters.getGender());
        person.setFirstName(nameService.randomFirstName(person.getGender(), personParameters.getExcludeNames()));
        person.setLastName(personParameters.getLastName() != null
                ? personParameters.getLastName()
                : nameService.randomLastName());
        person.setSocialClass(SocialClass.random());

        LocalDate aliveOnDate = personParameters.getAliveOnDateOrDefault();
        Integer minAge = personParameters.getMinAge() == null ? 0 : personParameters.getMinAge();
        if (personParameters.getBirthDate() != null) {
            person.setBirthDate(personParameters.getBirthDate());
            // Get a random death date such that the person is alive on the reference date if born on this
            // date
            long lifespan;
            do {
                lifespan = lifeTableService.randomLifeExpectancy(LifeTableService.LifeTablePeriod.VICTORIAN,
                        person.getBirthDate().until(aliveOnDate).getYears(), null);
            } while (person.getBirthDate().plusDays(lifespan).isBefore(aliveOnDate));
            person.setDeathDate(person.getBirthDate().plusDays(lifespan));
        } else {
            // Get a random age such that the person is at least minAge / at most maxAge on this reference date.
            // From this we get a birth date (not the actual lifespan).
            long ageAtReference = lifeTableService.randomLifeExpectancy(LifeTableService.LifeTablePeriod.VICTORIAN,
                    minAge, personParameters.getMaxAge());
            person.setBirthDate(aliveOnDate.minusDays(ageAtReference));

            // Now get a lifespan at least as old as he was determined to be at the reference date
            long lifespan = lifeTableService.randomLifeExpectancy(LifeTableService.LifeTablePeriod.VICTORIAN,
                    (int) Math.ceil(ageAtReference / 365.0), null);
            LocalDate deathDate = person.getBirthDate().plusDays(lifespan);
            // From this we can set a death date and the actual lifespan.
            person.setDeathDate(deathDate);
        }

        generateAndSetTraits(personParameters, person);

        if (person.isMale()) {
            person.setFertility(randomPaternity());
        } else {
            person.setFertility(randomMaternity(person));
        }

        return person;
    }

    /**
     * Generates a child or children (if twins are requested) for this family, given a birthdate. The new children will
     * be added to the existing list of children in the family object, and will also be returned from this method.
     *
     * @param family a family that must include a husband and wife
     * @param birthDate the desired birth date
     * @param includeIdenticalTwin if true, an identical twin will be included for the first child generated
     * @param includeFraternalTwin if true, a second random child will be added
     * @return a list of the children created
     */
    public List<Person> generateChildrenForParents(@NonNull Family family,
                                                   @NonNull LocalDate birthDate,
                                                   boolean includeIdenticalTwin,
                                                   boolean includeFraternalTwin) {
        if (family.getHusband() == null || family.getWife() == null) {
            throw new IllegalStateException("Cannot generate children without both parents");
        }
        PersonParameters personParameters = new PersonParameters();
        personParameters.setBirthDate(birthDate);
        if (family.isMarriage()) {
            personParameters.setLastName(family.getHusband().getLastName());
        } else {
            personParameters.setLastName(family.getWife().getLastName(birthDate));
        }

        // Don't name kids after other kids already born and not yet dead
        Set<String> alreadyUsedNames = family.getChildren().stream()
                .filter(p -> p.isLiving(birthDate))
                .map(Person::getFirstName)
                .collect(Collectors.toSet());
        personParameters.getExcludeNames().addAll(alreadyUsedNames);

        personParameters.setFather(family.getHusband());
        personParameters.setMother(family.getWife());

        List<Person> children = new ArrayList<>();
        children.add(generate(personParameters));
        personParameters.getExcludeNames().add(children.get(0).getFirstName());

        if (includeIdenticalTwin) {
            personParameters.setGender(children.get(0).getGender());
            children.add(generate(personParameters));
            matchIdenticalTwinParameters(children.get(0), children.get(1));
            personParameters.getExcludeNames().add(children.get(1).getFirstName());
        }
        if (includeFraternalTwin) {
            children.add(generateChildrenForParents(family, birthDate, false, false).get(0));
        }

        family.getChildren().addAll(children);

        // Chance of child death increases with number of children
        PercentDie die = new PercentDie();
        double chanceDeath = children.size() * CHILDBIRTH_DEATH_PROBABILITY;
        for (Person child : children) {
            if (die.roll() <= chanceDeath) {
                child.setDeathDate(birthDate);
            }
            child.setSocialClass(familyService.getCalculatedChildSocialClass(family, child, false, birthDate));
        }

        return children;
    }

    private void generateAndSetTraits(@NonNull PersonParameters personParameters, @NonNull Person person) {
        Person favoredParent = null;
        Person otherParent = null;
        Person father = personParameters.getFather();
        Person mother = personParameters.getMother();
        if (father != null && mother != null) {
            if (new Die(2).roll() == 1) {
                favoredParent = father;
                otherParent = mother;
            } else {
                favoredParent = mother;
                otherParent = father;
            }
        } else if (father != null) {
            favoredParent = father;
        } else if (mother != null) {
            favoredParent = mother;
        }

        person.setDomesticity(randomDomesticity());
        person.setCharisma(randomTrait());
        if (favoredParent != null) {
            person.setComeliness(randomTrait(favoredParent.getComeliness(),
                    otherParent == null ? null : otherParent.getComeliness()));
            person.setStrength(randomTrait(favoredParent.getStrength(),
                    otherParent == null ? null : otherParent.getStrength()));
        } else {
            person.setComeliness(randomTrait());
            person.setStrength(randomTrait());
        }
        if (mother != null) {
            person.setIntelligence(randomTrait(mother.getIntelligence(), father == null ? null : father.getIntelligence()));
        } else {
            person.setIntelligence(randomTrait());
        }
        person.setMorality(randomTrait());
    }

    /**
     * Copies some properties from twin1 to twin2 when identical twins are created.
     * @param twin1 the twin to use as the template
     * @param twin2 the twin who gets the values applied
     */
    private void matchIdenticalTwinParameters(@NonNull Person twin1, @NonNull Person twin2) {
        twin2.setComeliness(twin1.getComeliness());
    }

    double randomDomesticity() {
        return DOMESTICITY_BETA.sample();
    }

    private double randomTrait() {
        return TRAIT_BETA.sample();
    }

    /**
     * Get a random trait based on parents' traits.
     *
     * @param parentTrait if non-null, use a normal distribution with this as the mean
     * @param otherParentTrait if also non-null, shift the mean a little towards this parent's value
     * @return a double value for the trait based on the parent, or a random value if both are null
     */
    private double randomTrait(Double parentTrait, Double otherParentTrait) {
        if (parentTrait == null) {
            return randomTrait();
        }
        Double mean = parentTrait;
        if (otherParentTrait != null) {
            double diff = ((parentTrait - otherParentTrait) / 3);
            mean -= diff;
        }
        return new NormalDistribution(mean, 0.1).sample();
    }

    private Maternity randomMaternity(@NonNull Person woman) {
        Maternity maternity = new Maternity();
        maternity.setFertilityFactor(randFertilityFactor());
        maternity.setLastCycleDate(randFirstCycleDate(woman.getBirthDate()));
        maternity.setLastCheckDate(maternity.getLastCycleDate());
        maternity.setFrequencyFactor(randFrequencyFactor());
        maternity.setWithdrawalFactor(randWithdrawalFactor());
        maternity.setCycleLength(randCycleLength());
        maternity.setHavingRelations(true);
        return maternity;
    }

    private Paternity randomPaternity() {
        Paternity paternity = new Paternity();
        paternity.setFertilityFactor(randFertilityFactor());
        return paternity;
    }

    private double randFertilityFactor() {
        return new FunkyBetaDistribution(RAND_FERTILITY_ALPHA, RAND_FERTILITY_BETA).sample();
    }

    private double randFrequencyFactor() {
        return new FunkyBetaDistribution(RAND_FREQUENCY_ALPHA, RAND_FREQUENCY_BETA).sample();
    }

    private double randWithdrawalFactor() {
        return new FunkyBetaDistribution(RAND_WITHDRAWAL_ALPHA, RAND_WITHDRAWAL_BETA).sample();
    }

    private int randCycleLength() {
        return new BetweenDie().roll(26,32);
    }

    private LocalDate randFirstCycleDate(@NonNull LocalDate birthDate) {
        double betaVal = new BetaDistribution(RAND_FIRST_PERIOD_ALPHA, RAND_FIRST_PERIOD_BETA).sample();
        int minAge = FIRST_PERIOD_BASE_MIN_AGE_YEARS * 365;
        int maxAge = FIRST_PERIOD_BASE_MAX_AGE_YEARS * 365;
        return birthDate.plusDays((long) Math.floor((betaVal * (maxAge - minAge)) + minAge));
    }
}
