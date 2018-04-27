package com.meryt.demographics.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.request.GenerationPost;
import com.meryt.demographics.request.InitialGenerationPost;
import com.meryt.demographics.request.PersonFamilyPost;
import com.meryt.demographics.time.LocalDateComparator;

/**
 * Service used for processing entire generations of randomly generated people
 */
@Service
@Slf4j
public class GenerationService {

    private final PersonService personService;
    private final FamilyService familyService;
    private final FamilyGenerator familyGenerator;
    private final AncestryService ancestryService;
    private final TitleService titleService;
    private final InheritanceService inheritanceService;

    public GenerationService(@Autowired @NonNull PersonService personService,
                             @Autowired @NonNull FamilyService familyService,
                             @Autowired @NonNull FamilyGenerator familyGenerator,
                             @Autowired @NonNull AncestryService ancestryService,
                             @Autowired @NonNull TitleService titleService,
                             @Autowired @NonNull InheritanceService inheritanceService) {
        this.personService = personService;
        this.familyService = familyService;
        this.familyGenerator = familyGenerator;
        this.ancestryService = ancestryService;
        this.titleService = titleService;
        this.inheritanceService = inheritanceService;
    }

    public List<Family> seedInitialGeneration(@NonNull InitialGenerationPost generationPost) {
        List<String> lastNames = generationPost.getLastNames();
        if (lastNames == null) {
            lastNames = new ArrayList<>();
        }
        FamilyParameters familyParameters = generationPost.getFamilyParameters();
        int numFamilies = generationPost.getNumFamilies();
        List<Family> result = new ArrayList<>();
        for (int i = 0; i < numFamilies; i++) {
            if (!lastNames.isEmpty()) {
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
            if (familyParameters.isPersist()) {
                family = familyService.save(family);
            }
            if (family.getHusband().getSocialClass().getRank() >= SocialClass.BARONET.getRank()) {
                addRandomTitleToFounder(family.getHusband());
            }

            ancestryService.updateAncestryTable();
            result.add(family);
        }

        updateTitles();

        if (generationPost.getOutputToFile() != null) {
            writeGenerationsToFile(generationPost.getOutputToFile());
        }

        return result;
    }

    public List<Family> processGeneration(@NonNull GenerationPost generationPost) {
        generationPost.validate();
        PersonFamilyPost personFamilyPost = generationPost.getPersonFamilyPost();
        if (!personFamilyPost.isPersist()) {
            throw new IllegalArgumentException("Cannot process generation unless persist is true");
        }

        List<Person> unfinishedPersons = personService.loadUnfinishedPersons();

        List<Family> results = new ArrayList<>();
        for (Person person : unfinishedPersons) {

            FamilyParameters familyParameters = new FamilyParameters();
            familyParameters.setMinHusbandAge(personFamilyPost.getMinHusbandAge());
            familyParameters.setMinWifeAge(personFamilyPost.getMinWifeAge());
            familyParameters.setReferenceDate(personFamilyPost.getUntilDate() == null
                    ? person.getDeathDate()
                    : personFamilyPost.getUntilDate());
            familyParameters.setPersist(personFamilyPost.isPersist());
            familyParameters.setAllowExistingSpouse(personFamilyPost.isAllowExistingSpouse());
            familyParameters.setMinSpouseSelection(personFamilyPost.getMinSpouseSelection());

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
                person.setFinishedGeneration(true);
                personService.save(person);
            } else {
                family = familyService.save(family);
                results.add(family);
                ancestryService.updateAncestryTable();
            }
        }

        updateTitles();

        if (generationPost.getOutputToFile() != null) {
            writeGenerationsToFile(generationPost.getOutputToFile());
        }

        return results;
    }

    private void addRandomTitleToFounder(@NonNull Person founder) {
        Title title = new Title();
        title.setInheritanceRoot(founder);
        title.setName("Lord " + founder.getLastName());
        title.setSocialClass(founder.getSocialClass());
        if (new Die(10).roll() <= 2) {
            title.setPeerage(Peerage.SCOTLAND);
        } else {
            title.setPeerage(Peerage.ENGLAND);
        }
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
        for (Title title : titleService.findAll()) {
            Optional<PersonTitlePeriod> latestHolder = title.getTitleHolders().stream()
                    .max(Comparator.comparing(PersonTitlePeriod::getFromDate));
            if (latestHolder.isPresent()) {
                Person currentHolder = latestHolder.get().getPerson();
                if (!currentHolder.isFinishedGeneration()) {
                    continue;
                }
                log.info("Looking for heir to " + title.getName());
                List<Person> nextHolders = inheritanceService.findHeirForPerson(currentHolder,
                        currentHolder.getDeathDate(), title.getInheritance(), title.getInheritanceRoot());
                log.info(nextHolders.size() + " possible heir(s) found");
                if (nextHolders.size() == 1) {
                    Person nextHolder = nextHolders.get(0);
                    // The person may not have been born when the current title-holder died (e.g. he inherited via
                    // his mother), so in that case he inherited at birth.
                    LocalDate dateObtained = LocalDateComparator.max(currentHolder.getDeathDate(),
                            nextHolder.getBirthDate());
                    log.info(String.format("Adding title for %s starting %s", nextHolder, dateObtained));
                    nextHolder.addOrUpdateTitle(title, dateObtained, null);
                    if (Strings.isNullOrEmpty(nextHolder.getLastName())) {
                        String lastNameFromTitle = title.getName().replaceAll("^[^ ]+", "");
                        nextHolder.setLastName(lastNameFromTitle);
                    }
                    personService.save(nextHolder);
                }
            }
        }
    }

    private void writeGenerationsToFile(@NonNull String filePath) {
        try (FileWriter writer = new FileWriter(filePath, false); BufferedWriter out = new BufferedWriter(writer)) {

            out.write("## syntax=descendants\n\n");

            for (Person founder : personService.loadFounders()) {
                writeFamily(out, founder, 0);
                out.write("\n");
            }

        } catch (IOException e) {
            log.error("Failed to write to file " + filePath, e);
        }
    }

    private void writeFamily(BufferedWriter out, @NonNull Person person, int personDepth) throws IOException {
        String spouseEntries = writeSpouseEntries(person);
        out.write(String.format("%s%s%s%s\n",
                personDepth == 0 ? "" : Strings.repeat("    ", personDepth) + "+-- ",
                writePersonEntry(person),
                spouseEntries.isEmpty() ? "" : " m. " + spouseEntries,
                person.isFinishedGeneration() ? "." : ""));

        for (Person child : person.getChildren().stream()
                .filter(p -> p.getAgeInYears(p.getDeathDate()) > 13)
                .collect(Collectors.toList())) {
            if (person.isMale() || hasNoPaternalLineToFounder(child)) {
                // We always write a man's descendants beneath him. We only write a female's descendants beneath her
                // if the children do not have a direct patrilineal line to some founder, meaning they won't appear
                // in the output otherwise.
                writeFamily(out, child, personDepth + 1);
            }
        }
    }

    private String writePersonEntry(@NonNull Person person) {
        return String.format("%d %s%s %d-%d",
                person.getId(),
                writerPersonTitles(person),
                person.getName(),
                person.getBirthDate().getYear(),
                person.getDeathDate().getYear());
    }

    private String writeSpouseEntries(@NonNull Person person) {
        StringBuilder s = new StringBuilder();
        for (Person spouse : person.getSpouses()) {
            if (s.length() > 0) {
                s.append(", ");
            }
            s.append(writePersonEntry(spouse));
        }
        return s.toString();
    }

    private String writerPersonTitles(@NonNull Person person) {
        String titles = person.getTitles().stream()
                .sorted(Comparator.comparing(t -> t.getTitle().getSocialClass().getRank()))
                .map(t -> t.getTitle().getName())
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
}
