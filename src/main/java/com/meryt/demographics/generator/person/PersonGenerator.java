package com.meryt.demographics.generator.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.EyeColor;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.HairColor;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.math.FunkyBetaDistribution;
import com.meryt.demographics.request.PersonParameters;
import com.meryt.demographics.service.LifeTableService;
import com.meryt.demographics.service.NameService;
import com.meryt.demographics.service.SocialClassService;
import com.meryt.demographics.service.TraitService;

@Service
public class PersonGenerator {

    private static final double CHILDBIRTH_DEATH_PROBABILITY  = 0.02;
    private static final double RAND_DOMESTICITY_ALPHA        = 4;
	private static final double RAND_DOMESTICITY_BETA         = 5;
	private static final double RAND_TRAIT_ALPHA              = 2;
    private static final double RAND_TRAIT_BETA               = 1.8;

    private static final int LAST_NAME_BEGINNING_YEAR         = 1400;
    private static final int LAST_NAME_START_ADOPTING_YEAR    = 1550;

    private static final double AVG_ADULT_MALE_HEIGHT = 69.6850394;  // abt. 5'10" (177cm)
    private static final double AVG_ADULT_FEMALE_HEIGHT = 64.3700787;  // abt. 5'4.3" (163.5cm)
    private static final double AVG_ADULT_HEIGHT_STD_DEV = 2.8;  // 2.8" for both women and men

    private static final BetaDistribution DOMESTICITY_BETA = new FunkyBetaDistribution(RAND_DOMESTICITY_ALPHA,
            RAND_DOMESTICITY_BETA);
	private static final BetaDistribution TRAIT_BETA = new BetaDistribution(RAND_TRAIT_ALPHA, RAND_TRAIT_BETA);

    private final NameService nameService;
    private final LifeTableService lifeTableService;
    private final TraitService traitService;
    private final SocialClassService socialClassService;

    PersonGenerator(@Autowired NameService nameService,
                    @Autowired LifeTableService lifeTableService,
                    @Autowired TraitService traitService,
                    @Autowired SocialClassService socialClassService) {
        this.nameService = nameService;
        this.lifeTableService = lifeTableService;
        this.traitService = traitService;
        this.socialClassService = socialClassService;
    }

    /**
     * Generates a random person according to some parameters. The person is not saved.
     */
    @NonNull
    public Person generate(@NonNull PersonParameters personParameters) {
        personParameters.validate();

        LocalDate nameDate = (personParameters.getBirthDate() == null && personParameters.getBirthYear() == null)
                ? personParameters.getAliveOnDate()
                : (personParameters.getBirthDate() == null
                    ? LocalDate.of(personParameters.getBirthYear(), 1, 1)
                    : personParameters.getBirthDate());

        Person person = new Person();
        person.setGender(personParameters.getGender() == null ? Gender.random() : personParameters.getGender());
        if (personParameters.getFirstName() != null) {
            person.setFirstName(personParameters.getFirstName());
        } else {
            person.setFirstName(nameService.randomFirstName(person.getGender(), personParameters.getExcludeNames(),
                    nameDate, personParameters.getNameCulture()));
        }

        // People didn't use last names till about 1400. But if one is specified, use it.
        if (!PersonParameters.NO_LAST_NAME.equals(personParameters.getLastName())) {
            if (personParameters.getLastName() != null) {
                person.setLastName(personParameters.getLastName());
            } else {
                person.setLastName(getLastNameForNewPerson(nameDate, personParameters.getFather(),
                        personParameters.getMother(), personParameters.isBornOutOfWedlock()));
            }
        }

        generatePersonLifespan(personParameters, person);

        // Set the social class from the parents if they are present.
        SocialClass socialClass = socialClassService.getCalculatedChildSocialClass(personParameters.getFather(),
                personParameters.getMother(), person, false, person.getDeathDate());

        if (socialClass != null) {
            person.setSocialClass(socialClass);
        } else {
            // Otherwise get a random social class, optionally between two specified classes
            if (personParameters.getMinSocialClass() != null || personParameters.getMaxSocialClass() != null) {
                person.setSocialClass(SocialClass.randomBetween(personParameters.getMinSocialClass(),
                        personParameters.getMaxSocialClass()));
            } else {
                person.setSocialClass(SocialClass.random());
            }
        }

        generateAndSetTraits(personParameters, person);

        if (personParameters.getFather() != null && personParameters.getMother() != null) {
            Person father = personParameters.getFather();
            Person mother = personParameters.getMother();
            Family existingFamily = father.getFamilies().stream()
                    .filter(f -> f.getWife() != null && f.getWife().getId() == mother.getId())
                    .findFirst().orElse(null);
            if (existingFamily != null) {
                existingFamily.addChild(person);
            } else {
                Family family = new Family();
                family.setHusband(father);
                family.setWife(mother);
                family.addChild(person);
            }
        }

        FertilityGenerator fertilityGenerator = new FertilityGenerator();
        if (person.isMale()) {
            person.setPaternity(fertilityGenerator.randomPaternity());
        } else {
            person.setMaternity(fertilityGenerator.randomMaternity(person));
        }

        return person;
    }

    /**
     * Get a last name for a new person. If he has a father and the father has a last name, use it, unless it's a name
     * from nobility and the father is too far removed from a noble ancestor.
     *
     * If the year is after 1400 and the name would otherwise be null, generate a random last name.
     *
     * @param onDate the date on which the name should be generated (as they were not used before around 1400)
     * @param father the father, if available
     * @return a last name or null
     */
    private String getLastNameForNewPerson(@NonNull LocalDate onDate,
                                           @Nullable Person father,
                                           @Nullable Person mother,
                                           boolean isOutOfWedlock) {

        if (isOutOfWedlock && mother != null) {
            String motherLastName = mother.getLastName();
            // A child born out of wedlock to a noble woman has no last name
            if (motherLastName != null && lastNameAppearsNoble(motherLastName)) {
                return null;
            } else {
                return motherLastName;
            }
        }

        String fatherLastName = father == null ? null : father.getLastName();
        String motherLastName = mother == null ? null : mother.getLastName();

        if (fatherLastName == null && motherLastName != null
                && onDate.isAfter(LocalDate.of(LAST_NAME_START_ADOPTING_YEAR, 1, 1))) {
            // When the date starts getting late, use the mother's last name if she has one and the father does not.
            // But if the mother's name is noble and she is too far removed from nobility, don't use it.
            if (lastNameAppearsNoble(motherLastName)) {
                if (personHasTitleMatchingLastName(mother, motherLastName)
                        || personOwnsEstateMatchingLastName(mother, motherLastName, onDate)) {
                    return motherLastName;
                }
                Person grandfather = mother.getFather();
                if (grandfather != null && personHasTitleMatchingLastName(grandfather, motherLastName)) {
                    return motherLastName;
                }
                return null;
            }
            return motherLastName;
        }

        if (fatherLastName != null) {
            if (lastNameAppearsNoble(fatherLastName) && father.getId() != 0) {
                // A name indicating nobility or place name association is only retained if the person is a close and
                // direct descendant of a person holding the title.
                if (personHasTitleMatchingLastName(father, fatherLastName)) {
                    return fatherLastName;
                }
                if (personOwnsEstateMatchingLastName(father, fatherLastName, onDate)) {
                    return fatherLastName;
                }
                Person grandfather = father.getFather();
                if (grandfather != null && personHasTitleMatchingLastName(grandfather, fatherLastName)) {
                    return fatherLastName;
                }
            } else {
                return fatherLastName;
            }
        }

        if (onDate.isAfter(LocalDate.of(LAST_NAME_BEGINNING_YEAR, 1, 1))) {
            return nameService.randomLastName();
        }

        return null;
    }

    private boolean lastNameAppearsNoble(@NonNull String name) {
        String fln = name.toLowerCase();
        return (fln.startsWith("of ") || fln.startsWith("de ") || fln.startsWith("du ") || fln.startsWith("d'")
                || fln.startsWith("des ") || fln.startsWith("le "));
    }

    /**
     * Returns true if any of the person's titles would have led to the given last name (e.g. "Lord of Foo" with the
     * last name "of Foo")
     * @param person a person who may have titles
     * @param lastName a last name
     * @return true if any of the titles ends with the given last name
     */
    private boolean personHasTitleMatchingLastName(@NonNull Person person, @NonNull String lastName) {
        return person.getTitles().stream()
                .anyMatch(t -> t.getTitle().getName().endsWith(lastName));
    }

    /**
     * Returns true if any of the person's owned estates would have led to the given last name (e.g. an estate called
     * "Foo" with the last name "of Foo")
     *
     * @param person a person who may own estates
     * @param lastName a last name
     * @param onDate the date on which the person might own the estate
     * @return true if the last name ends with the name of any owned estate
     */
    private boolean personOwnsEstateMatchingLastName(@NonNull Person person,
                                                     @NonNull String lastName,
                                                     @NonNull LocalDate onDate) {
        return person.getOwnedDwellingPlaces(onDate).stream()
                .anyMatch(dp -> dp.isEstate() && lastName.endsWith(dp.getName()));
    }

    /**
     * Generates a child or children (if twins are requested) for this family, given a birth date. The new children will
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
        if (family.getHusband() == null && family.getWife() == null) {
            throw new IllegalStateException(String.format(
                    "Cannot generate children for family %d because neither parent is present.", family.getId()));
        } else if (family.getHusband() == null) {
            throw new IllegalStateException(String.format(
                    "Cannot generate children for family %d without both parents. %d %s is present but the other parent is not.",
                    family.getId(), family.getWife().getId(), family.getWife().getName()));
        } else if (family.getWife() == null) {
            throw new IllegalStateException(String.format(
                    "Cannot generate children for family %d without both parents. %d %s is present but the other parent is not.",
                    family.getId(), family.getHusband().getId(), family.getHusband().getName()));
        }

        PersonParameters personParameters = new PersonParameters();
        personParameters.setBirthDate(birthDate);

        // Don't name kids after other kids already born and not yet dead
        Set<String> alreadyUsedNames = family.getChildren().stream()
                .filter(p -> p.isLiving(birthDate))
                .map(Person::getFirstName)
                .collect(Collectors.toSet());
        personParameters.getExcludeNames().addAll(alreadyUsedNames);

        personParameters.setFather(family.getHusband());
        personParameters.setMother(family.getWife());
        personParameters.setBornOutOfWedlock(family.getWeddingDate() == null
                || family.getWeddingDate().isAfter(birthDate));

        List<Person> children = new ArrayList<>();
        Person firstChild = generate(personParameters);
        children.add(firstChild);
        family.addChild(firstChild);
        // Add the child's own name to the excluded names in case we generate identical twin
        personParameters.getExcludeNames().add(firstChild.getFirstName());

        if (includeIdenticalTwin) {
            personParameters.setGender(firstChild.getGender());
            Person identicalTwin = generate(personParameters);
            children.add(identicalTwin);
            // Some traits such as physical appearance must match
            matchIdenticalTwinParameters(firstChild, identicalTwin);
            // Add the child's own name to the excluded names in case we generate fraternal twin too
            family.addChild(identicalTwin);
        }
        if (includeFraternalTwin) {
            Person fraternalTwin = generateChildrenForParents(family, birthDate, false, false).get(0);
            children.add(fraternalTwin);
            family.addChild(fraternalTwin);
        }

        // Chance of child death increases with number of children
        double chanceDeath = children.size() * CHILDBIRTH_DEATH_PROBABILITY;
        for (Person child : children) {
            if (PercentDie.roll() <= chanceDeath) {
                child.setDeathDate(birthDate);
            }
            // Set social class in the same loop because a firstborn son may have higher class than the others
            child.setSocialClass(socialClassService.getCalculatedChildSocialClass(family.getHusband(), family.getWife(),
                    child, false, birthDate));
        }

        return children;
    }

    private void generatePersonLifespan(@NonNull PersonParameters personParameters, @NonNull Person person) {
        LocalDate aliveOnDate = personParameters.getAliveOnDate();
        Integer minAge = personParameters.getMinAge() == null ? 0 : personParameters.getMinAge();
        if (personParameters.getBirthDate() != null || personParameters.getBirthYear() != null) {
            if (personParameters.getBirthDate() != null) {
                person.setBirthDate(personParameters.getBirthDate());
            } else {
                int dayOfYear = BetweenDie.roll(0, 364);
                person.setBirthDate(LocalDate.of(personParameters.getBirthYear(), 1, 1).plusDays(dayOfYear));
            }

            if (personParameters.getDeathDate() != null || personParameters.getDeathYear() != null) {
                if (personParameters.getDeathDate() != null) {
                    person.setDeathDate(personParameters.getDeathDate());
                } else {
                    int dayOfYear = BetweenDie.roll(0, 364);
                    person.setDeathDate(LocalDate.of(personParameters.getDeathYear(), 1, 1).plusDays(dayOfYear));
                }
            } else {
                // Get a random death date such that the person is alive on the reference date if born on this
                // date
                long lifespan;
                Integer minAgeYears = aliveOnDate != null ? person.getBirthDate().until(aliveOnDate).getYears() : null;
                do {
                    lifespan = lifeTableService.randomLifeExpectancy(person.getBirthDate(),
                            minAgeYears, null, person.getGender());
                } while (aliveOnDate != null && person.getBirthDate().plusDays(lifespan).isBefore(aliveOnDate));
                person.setDeathDate(person.getBirthDate().plusDays(lifespan));
            }
        } else if (aliveOnDate != null) {
            // Get a random age such that the person is at least minAge / at most maxAge on this reference date.
            // From this we get a birth date (not the actual lifespan).
            long ageAtReference = lifeTableService.randomLifeExpectancy(aliveOnDate, minAge,
                    personParameters.getMaxAge(), person.getGender());
            person.setBirthDate(aliveOnDate.minusDays(ageAtReference));

            // Now get a lifespan at least as old as he was determined to be at the reference date
            long lifespan = lifeTableService.randomLifeExpectancy(aliveOnDate,
                    (int) Math.ceil(ageAtReference / 365.0), null, person.getGender());
            LocalDate deathDate = person.getBirthDate().plusDays(lifespan);
            // From this we can set a death date and the actual lifespan.
            person.setDeathDate(deathDate);
        }
    }

    /**
     * Generate some person traits for the person. If father and/or mother are set on the PersonParameters, they will
     * be used for some traits so that children resemble their parents
     *
     * @param personParameters person parameters (only father & mother are used, if present)
     * @param person the person whose traits will be set
     */
    private void generateAndSetTraits(@NonNull PersonParameters personParameters, @NonNull Person person) {

        // A person gets 1-3 random traits
        person.getTraits().addAll(traitService.randomTraits(new Die(3).roll()));

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

        generateAndSetTraitsFromParents(personParameters, person, father, mother, favoredParent, otherParent);
    }

    /**
     * Generate and set traits for a person given their parents (if available)

     * @param person the person whose traits till be set
     * @param father the person's father, if available
     * @param mother the person's mother, as intelligence is based on the mother
     * @param favoredParent if non-null, be one of the father or mother
     * @param otherParent if non-null, must be the other one of the father or mother
     */
    private void generateAndSetTraitsFromParents(@NonNull PersonParameters personParameters, @NonNull Person person, Person father, Person mother,
                                                 Person favoredParent, Person otherParent) {
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

        person.setHeightInches(randomHeight(person.getGender(), father == null ? null : father.getHeightInches(),
                mother == null ? null : mother.getHeightInches()));

        if (mother != null) {
            person.setIntelligence(randomTrait(mother.getIntelligence(), father == null ? null : father.getIntelligence()));
        } else {
            person.setIntelligence(randomTrait());
        }
        person.setMorality(randomTrait());
        person.setEyeGenes(getEyeGenesFromParents(personParameters, person, father, mother));
        if (person.getEyeColor() == null) {
            person.setEyeColor(EyeColor.randomFromGenes(person.getEyeGenes()));
        }
        person.setHairGenes(getHairGenesFromParents(personParameters, father, mother, person.getEyeColor()));
    }

    /**
     * Copies some properties from twin1 to twin2 when identical twins are created.
     * @param twin1 the twin to use as the template
     * @param twin2 the twin who gets the values applied
     */
    private void matchIdenticalTwinParameters(@NonNull Person twin1, @NonNull Person twin2) {
        twin2.setComeliness(twin1.getComeliness());
        twin2.setStrength(twin1.getStrength());
        twin2.setEyeGenes(twin1.getEyeGenes());
        twin2.setEyeColor(twin1.getEyeColor());
        twin2.setHairGenes(twin1.getHairGenes());
        twin2.setHeightInches(twin1.getHeightInches());
    }

    /**
     * Get a random value for domesticity using a special beta distribution
     *
     * @return a number between 0 and 1
     */
    static double randomDomesticity() {
        return DOMESTICITY_BETA.sample();
    }

    /**
     * Get a random value for a trait given no information about parents
     * @return a number between 0 and 1
     */
    private double randomTrait() {
        return TRAIT_BETA.sample();
    }

    private double randomHeight(@NonNull Gender gender, @Nullable Double fatherHeight, @Nullable Double motherHeight) {
        Double pHeight;
        if (fatherHeight != null && motherHeight != null) {
            pHeight = (fatherHeight + (1.08 * motherHeight)) / 2.0;
        } else if (fatherHeight != null) {
            pHeight = (fatherHeight + (1.08 * AVG_ADULT_FEMALE_HEIGHT)) / 2.0;
        } else if (motherHeight != null) {
            pHeight = (AVG_ADULT_MALE_HEIGHT + (1.08 * motherHeight)) / 2.0;
        } else {
            pHeight = null;
        }

        double avgForGender = gender == Gender.MALE ? AVG_ADULT_MALE_HEIGHT : AVG_ADULT_FEMALE_HEIGHT;

        if (pHeight == null) {
            // Just get a height from random distribution
            return new NormalDistribution(avgForGender, AVG_ADULT_HEIGHT_STD_DEV).sample();
        } else {
            double modifier = gender == Gender.MALE ? 1.0 : 1.08;
            // get a height based on parents' - using Galton's formula
            double childHeight = avgForGender + 0.6115 *
                    // Use male mean since female is adjusted by 1.08
                    ((pHeight - AVG_ADULT_MALE_HEIGHT) / modifier);
            return new NormalDistribution(childHeight, AVG_ADULT_HEIGHT_STD_DEV).sample();
        }
    }

    /**
     * Get a random trait based on parents' traits. If the parentTrait is null, just returns a random value.
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

    /**
     * Gets the eye genes from the parents, else random or using the provided eye color.
     * 
     * @param personParameters - if eye color is specified, will use it if parents eye color is not available
     * @param person - if eye color is specified rather than randomized, the color will be set on this person before returning the genes
     * @param father - the father whose eye color we may check
     * @param mother - the mother whose eye color we may check
     * @return the random or calculated or specified eye color
     */
    private String getEyeGenesFromParents(PersonParameters personParameters, Person person, Person father, Person mother) {
        if (father == null || mother == null || father.getEyeGenes() == null || mother.getEyeGenes() == null) {
            if (personParameters.getEyeColor() != null) {
                person.setEyeColor(EyeColor.valueOf(personParameters.getEyeColor()));
                return EyeColor.genesFromEyeColor(personParameters.getEyeColor());
            }
            return getRandomEyeGenes();
        }
        Die d2 = new Die(2);
        char fatherContribution = father.getEyeGenes().charAt(d2.roll() - 1);
        char motherContribution = mother.getEyeGenes().charAt(d2.roll() - 1);
        String genes = new String(new char[]{fatherContribution, motherContribution});
        if (genes.equals("CT")) {
            // normalize
            genes = "TC";
        }
        return genes;
    }

    private String getRandomEyeGenes() {
        int roll = new Die(4).roll();
        if (roll <= 2) {
            return "CC";
        } else if (roll == 3) {
            return "TC";
        } else {
            return "TT";
        }
    }

    /**
     * Get hair color from parents hair color, or if parents are both null, use the person's eye color to make a
     * blue-eyed person more likely to be blond and a brown-eyed person less likely
     *
     * @param father the person's father
     * @param mother the person's mother
     * @param ownEyeColor the person's eye color
     * @return a string representing the genes
     */
    private String getHairGenesFromParents(@NonNull PersonParameters personParameters,
                                           @Nullable Person father,
                                           @Nullable Person mother,
                                           @NonNull EyeColor ownEyeColor) {
        if (father == null || mother == null || father.getHairGenes() == null || mother.getHairGenes() == null) {
            if (personParameters.getHairColor() != null) {
                return HairColor.getGenesFromHairColor(personParameters.getHairColor());
            }
            return getRandomHairGenes(ownEyeColor);
        }

        Die d2 = new Die(2);
        char fatherBrownBlondContribution = father.getHairGenes().charAt(d2.roll() - 1);
        char motherBrownBlondContribution = mother.getHairGenes().charAt(d2.roll() - 1);
        char fatherRedContribution = father.getHairGenes().charAt(d2.roll() + 1);
        char motherRedContribution = mother.getHairGenes().charAt(d2.roll() + 1);

        String result = "";
        if (fatherBrownBlondContribution == 'b') {
            result += new String(new char[]{motherBrownBlondContribution, fatherBrownBlondContribution});
        } else {
            result += new String(new char[]{fatherBrownBlondContribution, motherBrownBlondContribution});
        }
        if (fatherRedContribution == 'r') {
            result += new String(new char[]{motherRedContribution, fatherRedContribution});
        } else {
            result += new String(new char[]{fatherRedContribution, motherRedContribution});
        }
        return result;
    }

    /**
     * Get random hair color, but if eyes are blue and the first try is not a shade of blond, then try a second time.
     * Likewise if eyes are brown and first try is blond, try a second time.
     *
     * @param ownEyeColor which will cause blond to be favored if the eyes are known to be blue, and blond to be
     *                    unfavored if eyes are known to be brown.
     * @return a gene sequence determining hair color
     */
    private String getRandomHairGenes(@NonNull EyeColor ownEyeColor) {
        String genes = getRandomHairGenes();
        if (ownEyeColor.isBlue() && !genes.startsWith("bb")) {
            genes = getRandomHairGenes();
            if (!genes.startsWith("bb")) {
                return getRandomHairGenes();
            }
        } else if (ownEyeColor.isBrown() && genes.startsWith("bb")) {
            genes = getRandomHairGenes();
            if (genes.startsWith("bb")) {
                return getRandomHairGenes();
            }
        }
        return genes;
    }

    /**
     * Gets a random gene combo for hair color
     */
    private String getRandomHairGenes() {
        switch (new Die(9).roll()) {
            case 1: return "BBRR";
            case 2: return "BBRr";
            case 3: return "BbRR";
            case 4: return "BbRr";
            case 5: return "bbRR";
            case 6: return "bbRr";
            case 7: return "BBrr";
            case 8: return "Bbrr";
            case 9:
            default:
                return "bbrr";
        }
    }
}
