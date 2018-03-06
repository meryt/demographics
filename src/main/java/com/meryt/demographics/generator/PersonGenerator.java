package com.meryt.demographics.generator;

import java.time.LocalDate;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.service.LifeTableService;
import com.meryt.demographics.service.NameService;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PersonGenerator {

    private static final double RAND_DOMESTICITY_ALPHA        = 1.5;
	private static final double RAND_DOMESTICITY_BETA         = 2.5;
	private static final BetaDistribution DOMESTICITY_BETA = new BetaDistribution(RAND_DOMESTICITY_ALPHA,
            RAND_DOMESTICITY_BETA);
	private static final BetaDistribution TRAIT_BETA = new BetaDistribution(2, 1.8);

    private final NameService nameService;
    private final LifeTableService lifeTableService;

    public PersonGenerator(@Autowired NameService nameService, @Autowired LifeTableService lifeTableService) {
        this.nameService = nameService;
        this.lifeTableService = lifeTableService;
    }

    public Person generate(PersonParameters personParameters) {
        Person person = new Person();
        person.setGender(personParameters.getGender() == null ? Gender.random() : personParameters.getGender());
        person.setFirstName(nameService.randomFirstName(person.getGender()));
        person.setLastName(nameService.randomLastName());
        person.setSocialClass(SocialClass.random());

        LocalDate aliveOnDate = personParameters.getAliveOnDate();
        Integer minAge = personParameters.getMinAge() == null ? 0 : personParameters.getMinAge();
        if (aliveOnDate != null) {
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
                person.setLifespanInDays(lifespan);
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
                person.setLifespanInDays(lifespan);
            }

        } else {
            person.setLifespanInDays(lifeTableService.randomLifeExpectancy(LifeTableService.LifeTablePeriod.VICTORIAN,
                    minAge, personParameters.getMaxAge()));
        }

        person.setDomesticity(randomDomesticity());
        person.setCharisma(randomTrait());
        person.setComeliness(randomTrait());

        return person;
    }


    double randomDomesticity() {
        return DOMESTICITY_BETA.sample();
    }

    private double randomTrait() {
        return TRAIT_BETA.sample();
    }
}
