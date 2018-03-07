package com.meryt.demographics.generator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.service.FamilyService;
import com.meryt.demographics.service.LifeTableService;
import com.meryt.demographics.service.NameService;
import lombok.NonNull;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PersonGenerator {

    private static final double CHILDBIRTH_DEATH_PROBABILITY  = 0.02;
    private static final double RAND_DOMESTICITY_ALPHA        = 1.5;
	private static final double RAND_DOMESTICITY_BETA         = 2.5;
	private static final BetaDistribution DOMESTICITY_BETA = new BetaDistribution(RAND_DOMESTICITY_ALPHA,
            RAND_DOMESTICITY_BETA);
	private static final BetaDistribution TRAIT_BETA = new BetaDistribution(2, 1.8);

    private final NameService nameService;
    private final LifeTableService lifeTableService;
    private final FamilyService familyService;

    public PersonGenerator(@Autowired NameService nameService,
                           @Autowired LifeTableService lifeTableService,
                           @Autowired FamilyService familyService) {
        this.nameService = nameService;
        this.lifeTableService = lifeTableService;
        this.familyService = familyService;
    }

    public Person generate(PersonParameters personParameters) {
        Person person = new Person();
        person.setGender(personParameters.getGender() == null ? Gender.random() : personParameters.getGender());
        person.setFirstName(nameService.randomFirstName(person.getGender()));
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

        person.setDomesticity(randomDomesticity());
        person.setCharisma(randomTrait());
        person.setComeliness(randomTrait());

        return person;
    }

    /**
     * Generates children for this family, given a birthdate. The new children will be added to the existing list
     * of children in the family.
     *
     * @param family
     * @param birthDate
     * @param includeIdenticalTwin
     * @param includeFraternalTwin
     * @return
     */
    public List<Person> generateChildrenForParents(@NonNull Family family,
                                                   @NonNull LocalDate birthDate,
                                                   boolean includeIdenticalTwin,
                                                   boolean includeFraternalTwin) {
        PersonParameters personParameters = new PersonParameters();
        personParameters.setBirthDate(birthDate);
        if (family.isMarriage()) {
            personParameters.setLastName(family.getHusband().getLastName());
        } else {
            personParameters.setLastName(family.getWife().getLastName(birthDate));
        }

        List<Person> children = new ArrayList<>();
        children.add(generate(personParameters));

        if (includeIdenticalTwin) {
            personParameters.setGender(children.get(0).getGender());
            children.add(generate(personParameters));
            matchIdenticalTwinParameters(children.get(0), children.get(1));
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
}
