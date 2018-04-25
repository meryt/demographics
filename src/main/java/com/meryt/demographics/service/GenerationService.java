package com.meryt.demographics.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.generator.family.MatchMaker;
import com.meryt.demographics.generator.random.Die;
import com.meryt.demographics.request.FamilyParameters;
import com.meryt.demographics.request.GenerationPost;
import com.meryt.demographics.request.InitialGenerationPost;
import com.meryt.demographics.request.PersonFamilyPost;

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

    public GenerationService(@Autowired @NonNull PersonService personService,
                             @Autowired @NonNull FamilyService familyService,
                             @Autowired @NonNull FamilyGenerator familyGenerator,
                             @Autowired @NonNull AncestryService ancestryService) {
        this.personService = personService;
        this.familyService = familyService;
        this.familyGenerator = familyGenerator;
        this.ancestryService = ancestryService;
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
            ancestryService.updateAncestryTable();
            result.add(family);
        }

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

        if (generationPost.getOutputToFile() != null) {
            writeGenerationsToFile(generationPost.getOutputToFile());
        }

        return results;
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
        if (person.isMale()) {
            for (Person child : person.getChildren().stream()
                    .filter(p -> p.getAgeInYears(p.getDeathDate()) > 13)
                    .collect(Collectors.toList())) {
                writeFamily(out, child, personDepth + 1);
            }
        }
    }

    private String writePersonEntry(@NonNull Person person) {
        return String.format("%d %s %d-%d",
                person.getId(),
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
}
