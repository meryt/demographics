package com.meryt.demographics.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.title.Peerage;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.family.MatchMaker;
import com.meryt.demographics.generator.random.BetweenDie;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.request.GenerationPost;
import com.meryt.demographics.request.InitialGenerationPost;
import com.meryt.demographics.request.PersonFamilyPost;
import com.meryt.demographics.request.RandomFamilyParameters;

/**
 * Service used for processing entire generations of randomly generated people
 */
@Service
@Slf4j
public class GenerationService {

    private final PersonService personService;
    private final FamilyService familyService;
    private final FamilyGenerator familyGenerator;
    private final TitleService titleService;

    public GenerationService(@Autowired @NonNull PersonService personService,
                             @Autowired @NonNull FamilyService familyService,
                             @Autowired @NonNull FamilyGenerator familyGenerator,
                             @Autowired @NonNull TitleService titleService) {
        this.personService = personService;
        this.familyService = familyService;
        this.familyGenerator = familyGenerator;
        this.titleService = titleService;
    }

    /**
     * Create one or more founding families. A husband, wife, and possibly children will be generated. If the husband
     * has a rank of baronet or higher, a random title will be generated and assigned to him.
     *
     * @param generationPost defines options for the families in this generation generation
     * @return a list of all the families generated
     */
    public List<Family> seedInitialGeneration(@NonNull InitialGenerationPost generationPost) {
        List<String> lastNames = generationPost.getLastNames();
        if (lastNames == null) {
            lastNames = new ArrayList<>();
        }
        List<String> scottishLastNames = generationPost.getScottishLastNames();
        if (scottishLastNames == null) {
            scottishLastNames = lastNames;
        }
        RandomFamilyParameters familyParameters = generationPost.getFamilyParameters();
        // Persisting during family generation causes hibernate to lose visibility of newly created persons when
        // doing updateTitles and writing the output file. (The data is written to the DB but not visible to the
        // Hibernate session here. That causes titles to be incorrectly inherited or marked extinct.)
        familyParameters.setPersist(false);
        int numFamilies = generationPost.getNumFamilies();
        List<Family> result = new ArrayList<>();
        for (int i = 0; i < numFamilies; i++) {
            Peerage peerage;
            double percentScottish = generationPost.getPercentScottish() == null ? 0.3 : generationPost.getPercentScottish();
            if (new PercentDie().roll() <= percentScottish) {
                peerage = Peerage.SCOTLAND;
            } else {
                peerage = Peerage.ENGLAND;
            }

            if (peerage == Peerage.SCOTLAND && !scottishLastNames.isEmpty()) {
                int index = new Die(scottishLastNames.size()).roll() - 1;
                String lastName = scottishLastNames.remove(index);
                familyParameters.setFounderLastName(lastName);
            } else if (!lastNames.isEmpty()) {
                int index = new Die(lastNames.size()).roll() - 1;
                String lastName = lastNames.remove(index);
                familyParameters.setFounderLastName(lastName);
            } else {
                familyParameters.setFounderLastName(null);
            }
            Family family = familyGenerator.generate(familyParameters);
            if (family == null) {
                continue;
            }
            family.getHusband().setFounder(true);
            family = familyService.save(family);
            if (family.getHusband().getSocialClass().getRank() >= SocialClass.BARONET.getRank()) {
                addRandomTitleToFounder(family.getHusband(), peerage);
            }

            result.add(family);
        }

        updateTitles();

        if (generationPost.getOutputToFile() != null) {
            writeGenerationsToFile(generationPost.getOutputToFile());
        }

        return result;
    }

    /**
     * Process the next generation. This takes all persons that are not yet marked as finished generation, and attempts
     * to create a family for them within their lifetime. If they cannot find a spouse or die before their spouse, they
     * are marked as finished generation. It also updates titles with the next heirs, if any can be identified, and
     * finally writes the output to a file if a filename is specified.
     *
     * @param generationPost options for processing this generation
     * @return a list of the new families that were created
     */
    public List<Family> processGeneration(@NonNull GenerationPost generationPost) {
        generationPost.validate();

        LocalDate untilDate = generationPost.getPersonFamilyPost().getUntilDate();
        boolean onlyNonResidents = untilDate != null && generationPost.getOnlyNonResidents() != null
                && generationPost.getOnlyNonResidents();

        List<Person> unfinishedPersons;
        if (generationPost.getOnlyForPerson() != null) {
            Person person = personService.load(generationPost.getOnlyForPerson());
            if (person == null) {
                throw new IllegalArgumentException("No person exists for ID " + generationPost.getOnlyForPerson());
            }
            unfinishedPersons = new ArrayList<>();
            unfinishedPersons.add(person);
        } else if (onlyNonResidents) {
            // Only load people who have never been in a household
            unfinishedPersons = personService.loadUnfinishedNonResidents();
        } else {
            unfinishedPersons = personService.loadUnfinishedPersons();
        }

        return processGeneration(unfinishedPersons, generationPost);
    }

    public List<Family> processGeneration(@NonNull List<Person> unfinishedPersons,
                                          @NonNull GenerationPost generationPost) {

        PersonFamilyPost personFamilyPost = generationPost.getPersonFamilyPost();
        if (!personFamilyPost.isPersist()) {
            throw new IllegalArgumentException("Cannot process generation unless persist is true");
        }

        LocalDate untilDate = generationPost.getPersonFamilyPost().getUntilDate();
        boolean shouldLoopUntilReferenceDate = untilDate != null;

        List<Family> results = new ArrayList<>();
        for (int i = 0; i < unfinishedPersons.size(); i++) {
            Person person = unfinishedPersons.get(i);

            // Loop in case he got married but his spouse died before the reference date
            boolean personHadAFamilyInLastRound;
            do {
                RandomFamilyParameters familyParameters = new RandomFamilyParameters();
                familyParameters.setMinHusbandAge(personFamilyPost.getMinHusbandAge());
                familyParameters.setMinWifeAge(personFamilyPost.getMinWifeAge());
                familyParameters.setReferenceDate(personFamilyPost.getUntilDate() == null
                        ? person.getDeathDate()
                        : personFamilyPost.getUntilDate());
                // Persisting during family generation causes hibernate to lose visibility of newly created persons when
                // doing updateTitles and writing the output file. (The data is written to the DB but not visible to the
                // Hibernate session here. That causes titles to be incorrectly inherited or marked extinct.)
                familyParameters.setPersist(false);
                familyParameters.setAllowExistingSpouse(personFamilyPost.isAllowExistingSpouse());
                familyParameters.setMinSpouseSelection(personFamilyPost.getMinSpouseSelection());
                // Allows a woman pregnant at time of husband's death to give birth
                familyParameters.setCycleToDeath(true);

                Family family = null;
                if (person.isMale() || (person.getFamilies().size() < 2 &&
                        person.getLivingChildren(MatchMaker.getDateToStartMarriageSearch(person,
                                familyParameters.getMinHusbandAgeOrDefault(),
                                familyParameters.getMinWifeAgeOrDefault())).isEmpty())) {
                    // For women, only generate a new family if the person has been married no more than once and has no
                    // living children when she starts the search.
                    family = familyGenerator.generate(person, familyParameters);
                }
                if (family == null) {
                    personHadAFamilyInLastRound = false;
                    if (untilDate == null || person.getDeathDate().isBefore(untilDate) || person.isSurvivedByASpouse()) {
                        person.setFinishedGeneration(true);
                        personService.save(person);
                    }
                } else {
                    personHadAFamilyInLastRound = true;
                    if (shouldLoopUntilReferenceDate) {
                        // Add any newly created people so they too can be caught up to the reference date
                        if (family.getHusband().getId() == 0) {
                            unfinishedPersons.add(family.getHusband());
                        }
                        if (family.getWife().getId() == 0) {
                            unfinishedPersons.add(family.getWife());
                        }
                        unfinishedPersons.addAll(family.getChildren());
                    }
                    family = familyService.save(family);
                    results.add(family);
                }
            } while (shouldLoopUntilReferenceDate && !person.isFinishedGeneration() && personHadAFamilyInLastRound
                    && !person.isMarriedNowOrAfter(untilDate));
        }

        if (generationPost.getSkipTitleUpdate() == null || !generationPost.getSkipTitleUpdate()) {
            updateTitles();
        }

        if (generationPost.getOutputToFile() != null) {
            writeGenerationsToFile(generationPost.getOutputToFile());
        }

        return results;
    }

    /**
     * Generates a title for the person, using his last name as the title name. The peerage and inheritance style
     * are random.
     *
     * @param founder the person who gets a title (does not need to be male)
     */
    private void addRandomTitleToFounder(@NonNull Person founder, @NonNull Peerage peerage) {
        Title title = new Title();
        title.setSocialClass(founder.getSocialClass());
        title.setPeerage(peerage);

        title.setInheritanceRoot(founder);
        String namePrefix = "Lord ";
        switch (founder.getSocialClass()) {
            case DUKE:
            case MARQUESS:
            case EARL:
            case VISCOUNT:
                namePrefix = StringUtils.capitalize(founder.getSocialClass().getFriendlyName()) + " ";
                break;
            case BARON:
                if (title.getPeerage() == Peerage.SCOTLAND) {
                    // Scots "Barons" are called Lords of Parliament
                    namePrefix = "Lord ";
                } else {
                    namePrefix = StringUtils.capitalize(founder.getSocialClass().getFriendlyName()) + " ";
                }
                break;
        }
        title.setName(namePrefix + founder.getLastName());

        int roll = new Die(4).roll();
        if (roll == 1) {
            title.setInheritance(TitleInheritanceStyle.HEIRS_GENERAL);
        } else if (roll == 2) {
            title.setInheritance(TitleInheritanceStyle.HEIRS_MALE_GENERAL);
        } else if (roll == 3) {
            title.setInheritance(TitleInheritanceStyle.HEIRS_OF_THE_BODY);
        } else {
            title.setInheritance(TitleInheritanceStyle.HEIRS_MALE_OF_THE_BODY);
        }
        title = titleService.save(title);
        int maxYear = founder.getDeathDate().getYear() - 1;
        int minYear = founder.getBirthDate().getYear() + 15;
        BetweenDie betweenDie = new BetweenDie();
        // Get the first of the month
        LocalDate fromDate = LocalDate.of(betweenDie.roll(minYear, maxYear),
                betweenDie.roll(1, 12), 1);
        // Get a random date in that month
        fromDate = LocalDate.of(fromDate.getYear(), fromDate.getMonthValue(),
                betweenDie.roll(1, fromDate.getMonth().length(false)));
        founder.addOrUpdateTitle(title, fromDate, null);
        personService.save(founder);
    }

    private void updateTitles() {
        boolean foundAny;
        do {
            foundAny = false;
            for (Title title : titleService.findAll()) {
                if (title.isExtinct()) {
                    continue;
                }
                if (titleService.updateTitleHeirs(title) != null) {
                    foundAny = true;
                }
            }
        } while (foundAny);
    }

    public void writeGenerationsToFile(@NonNull String filePath) {
        try (FileWriter writer = new FileWriter(filePath, false); BufferedWriter out = new BufferedWriter(writer)) {

            out.write("## syntax=descendants\n\n");

            List<Person> founders = personService.loadFounders();
            List<Person> foundersWithoutTitles = founders.stream()
                    .filter(p -> p.getTitles().isEmpty())
                    .collect(Collectors.toList());

            List<Person> foundersWithTitles = founders.stream()
                    .filter(p -> !p.getTitles().isEmpty())
                    .sorted(Comparator.comparing((Person p) -> p.getTitles().get(0).getTitle().isExtinct())
                            .thenComparing((Person p) -> p.getTitles().get(0).getTitle().getName().toLowerCase()))
                    .collect(Collectors.toList());
            for (Person founder : foundersWithTitles) {
                Set<Long> alreadyWrittenPersons = new HashSet<>();
                writeFamily(out, founder, 0, founder, alreadyWrittenPersons);
                out.write("\n");
            }

            for (Person founder : foundersWithoutTitles) {
                Set<Long> alreadyWrittenPersons = new HashSet<>();
                writeFamily(out, founder, 0, founder, alreadyWrittenPersons);
                out.write("\n");
            }

        } catch (IOException e) {
            log.error("Failed to write to file " + filePath, e);
        }
    }

    private void writeFamily(BufferedWriter out,
                             @NonNull Person person,
                             int personDepth,
                             @NonNull Person founder,
                             @NonNull Set<Long> alreadyWrittenPersons)
            throws IOException {
        String spouseEntries = writeSpouseEntries(person, alreadyWrittenPersons);
        out.write(String.format("%s%s%s%s\n",
                personDepth == 0 ? "" : Strings.repeat("    ", personDepth) + "+-- ",
                writePersonEntry(person, alreadyWrittenPersons),
                spouseEntries.isEmpty() ? "" : " m. " + spouseEntries,
                person.isFinishedGeneration() ? "." : ""));

        for (Person child : person.getChildren().stream()
                .filter(p -> p.getAgeInYears(p.getDeathDate()) > 13 || !p.getTitles().isEmpty())
                .collect(Collectors.toList())) {
            if (person.isMale()
                    || !person.getTitles().isEmpty() // always write if person has a title
                    || hasNoPaternalLineToFounder(child)
                    || hasSameTitleAsFounder(child, founder)
                    || anyChildHasSameTitleAsFounder(child, founder, alreadyWrittenPersons)
                    || anyGrandchildHasSameTitleAsFounder(child, founder, alreadyWrittenPersons)
                    || anyGreatGrandchildHasSameTitleAsFounder(child, founder, alreadyWrittenPersons)) {
                // We always write a man's descendants beneath him. We only write a female's descendants beneath her
                // if the children do not have a direct patrilineal line to some founder, meaning they won't appear
                // in the output otherwise, or if they have the founder's title.
                writeFamily(out, child, personDepth + 1, founder, alreadyWrittenPersons);
            }
        }
    }

    private String writePersonEntry(@NonNull Person person, @NonNull Set<Long> alreadyWrittenPersons) {
        alreadyWrittenPersons.add(person.getId());
        return String.format("%d %s%s %d-%d",
                person.getId(),
                writePersonTitles(person),
                person.getName(),
                person.getBirthDate().getYear(),
                person.getDeathDate().getYear());
    }

    private String writeSpouseEntries(@NonNull Person person, @NonNull Set<Long> alreadyWrittenPersons) {
        StringBuilder s = new StringBuilder();
        for (Person spouse : person.getSpouses()) {
            if (s.length() > 0) {
                s.append(", ");
            }
            s.append(writePersonEntry(spouse, alreadyWrittenPersons));
        }
        return s.toString();
    }

    private String writePersonTitles(@NonNull Person person) {
        String titles = person.getTitles().stream()
                .sorted(Comparator.comparing(t -> t.getTitle().getSocialClass().getRank()))
                .map(t -> String.format("%s%s - %s%s",
                        (t.getTitle().isExtinct() ? "*" : ""),
                        t.getTitle().getId(),
                        t.getTitle().getName(),
                        (t.getTitle().getInheritance().isMalesOnly() ? " (M)": "")))
                .collect(Collectors.joining(", "));

        if (titles != null && !titles.isEmpty()) {
            return "[" + titles + "] ";
        } else {
            return "";
        }
    }

    /**
     * Determines whether this person has a direct patrilineal relationship to one of the founders. If not, he may
     * appear beneath his mother's descendants so he does not get lost.
     */
    private boolean hasNoPaternalLineToFounder(@NonNull Person person) {
        if (person.isFounder()) {
            return false;
        }
        Person father = person;
        while ((father = father.getFather()) != null) {
            if (father.isFounder()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if this person has a title matching the first (and presumably only) title of the given founder
     */
    private boolean hasSameTitleAsFounder(@NonNull Person person, @NonNull Person founder) {
        if (person.equals(founder)) {
            return true;
        }
        List<PersonTitlePeriod> personTitles = person.getTitles();
        if (personTitles.isEmpty()) {
            return false;
        }
        List<PersonTitlePeriod> founderTitles = founder.getTitles();
        if (founderTitles.isEmpty()) {
            return false;
        }
        Title title = founderTitles.get(0).getTitle();
        return personTitles.stream().anyMatch(pt -> pt.getTitle().getId() == title.getId());
    }

    /**
     * Returns true if any of this person's children has the same title as this founder's title
     */
    private boolean anyChildHasSameTitleAsFounder(@NonNull Person person,
                                                  @NonNull Person founder,
                                                  @NonNull Set<Long> alreadyWrittenPersons) {
        if (person.equals(founder)) {
            return true;
        }
        return person.getChildren().stream()
                .filter(p -> !alreadyWrittenPersons.contains(p.getId()))
                .anyMatch(child -> hasSameTitleAsFounder(child, founder));
    }

    /**
     * Returns true if any of this person's grandchildren has the same title as this founder's title
     */
    private boolean anyGrandchildHasSameTitleAsFounder(@NonNull Person person,
                                                       @NonNull Person founder,
                                                       @NonNull Set<Long> alreadyWrittenPersons) {
        if (person.equals(founder)) {
            return true;
        }
        return person.getChildren().stream()
                .filter(child -> !alreadyWrittenPersons.contains(child.getId()))
                .flatMap(l -> l.getChildren().stream())
                .filter(grandchild -> !alreadyWrittenPersons.contains(grandchild.getId()))
                .anyMatch(grandchild -> hasSameTitleAsFounder(grandchild, founder));
    }

    /**
     * Returns true if any of this person's grandchildren has the same title as this founder's title
     */
    private boolean anyGreatGrandchildHasSameTitleAsFounder(@NonNull Person person,
                                                            @NonNull Person founder,
                                                            @NonNull Set<Long> alreadyWrittenPersons) {
        if (person.equals(founder)) {
            return true;
        }
        return person.getChildren().stream()
                .flatMap(l -> l.getChildren().stream())
                .anyMatch(grandchild -> anyGrandchildHasSameTitleAsFounder(grandchild, founder, alreadyWrittenPersons));
    }
}
