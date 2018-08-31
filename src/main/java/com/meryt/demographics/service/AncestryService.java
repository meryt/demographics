package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import com.meryt.demographics.domain.family.AncestryRecord;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.family.LeastCommonAncestorRelationship;
import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.repository.AncestryRepository;

@Slf4j
@Service
public class AncestryService {

    private final AncestryRepository ancestryRepository;

    public AncestryService(@Autowired @NonNull AncestryRepository ancestryRepository) {
        this.ancestryRepository = ancestryRepository;
    }

    /**
     * Truncates and rebuilds the ancestry table in the database.
     */
    public void updateAncestryTable() {
        log.info("Truncating and rebuilding ancestry table");
        ancestryRepository.updateAncestryTable();
        log.info("Ancestry table regeneration complete");
    }

    @NonNull
    List<AncestryRecord> getDescendants(long personId) {
        return ancestryRepository.getDescendants(personId);
    }

    @NonNull
    List<LeastCommonAncestorRelationship> getRelatives(long personId, @Nullable Long maxDistance) {
        return ancestryRepository.getRelatives(personId, maxDistance);
    }

    @NonNull
    List<LeastCommonAncestorRelationship> getLivingRelatives(long personId,
                                                             @NonNull LocalDate onDate,
                                                             @Nullable Long maxDistance) {
        return ancestryRepository.getLivingRelatives(personId, onDate, maxDistance);
    }

    /**
     * Calculate the relationship between two people. Considers marital and in-law relationships.
     * <p>
     * The Relationship returned describes the relationship of person1 to person2. E.g. if person1 is the father of
     * person2 then the relationship is "father".
     * <p>
     * So this method fills in the blank of "Person 1 is the ______ of Person 2"
     *
     * @param person1 subject person
     * @param person2 related person
     * @param bloodOnly if true, result only considers blood relationships and will not report relationships due to
     *                  marriage
     * @return the relationship, or null if they are not related in any way
     */
    @Nullable
    public Relationship calculateRelationship(@NonNull Person person1, @NonNull Person person2, boolean bloodOnly) {
        if (person1.getId() == 0 || person2.getId() == 0) {
            // Cannot determine relationship between people that have not been persisted.
            return null;
        }
        if (person1.getId() == person2.getId()) {
            return new Relationship(Relationship.SELF, 0, null, null);
        }
        if (!bloodOnly) {
            Relationship maritalRelationship = determineMaritalRelationship(person1, person2);
            if (maritalRelationship != null) {
                return maritalRelationship;
            }
        }
        Relationship leastCommonAncestorRelationship = determineLeastCommonAncestorRelationship(person1, person2);
        if (leastCommonAncestorRelationship != null) {
            return leastCommonAncestorRelationship;
        }
        // TODO perhaps add in-law relationships

        return null;
    }

    /**
     * Calculate the relationship between two people. Considers marital and in-law relationships.
     * <p>
     * The Relationship returned describes the relationship of person1 to person2. E.g. if person1 is the father of
     * person2 then the relationship is "father".
     * <p>
     * So this method fills in the blank of "Person 1 is the ______ of Person 2"
     *
     * @param person1 subject person
     * @param person2 related person
     * @return the relationship, or null if they are not related in any way
     */
    @Nullable
    public Relationship calculateRelationship(@NonNull Person person1, @NonNull Person person2) {
        return calculateRelationship(person1, person2, false);
    }

    /**
     * Determines whether people are married or otherwise in a relationship (i.e. have a family record) and if so
     * what type
     *
     * @param person1 subject person
     * @param person2 related person
     * @return the relationship, or null if they are not spouses or partners
     */
    @Nullable
    private Relationship determineMaritalRelationship(@NonNull Person person1, @NonNull Person person2) {
        Optional<Family> couplesFamily = person1.getFamilies().stream()
                .filter(f -> person1.isMale()
                        ? f.getWife() != null && person2.getId() == f.getWife().getId()
                        : f.getHusband() != null && person2.getId() == f.getHusband().getId())
                .findFirst();
        if (couplesFamily.isPresent()) {
            if (couplesFamily.get().isMarriage()) {
                return new Relationship(Relationship.SPOUSE, 1, null, null);
            } else {
                return new Relationship(Relationship.PARTNER, 1, null, null);
            }
        }
        return null;
    }

    /**
     * Gets the least common ancestor relationship between the two people, or null if they are not related in any way
     */
    @Nullable
    public LeastCommonAncestorRelationship getLeastCommonAncestor(@NonNull Person person1, @NonNull Person person2) {
        return ancestryRepository.getLeastCommonAncestorInfo(person1.getId(), person2.getId());
    }

    /**
     * Determines a relationship based on least common ancestors, if any
     *
     * @param person1 subject person
     * @param person2 related person
     * @return the relationship, or null if they are not related by blood
     */
    @Nullable
    private Relationship determineLeastCommonAncestorRelationship(@NonNull Person person1, @NonNull Person person2) {
        LeastCommonAncestorRelationship relationship = ancestryRepository.getLeastCommonAncestorInfo(person1.getId(),
                person2.getId());
        if (relationship == null) {
            return null;
        }

        // Check for parental and grandparental relationships
        if (relationship.getSubject1Distance() == 0) {
            String prefix = getGrandPrefix(relationship.getSubject2Distance());
            String relationshipName = person1.isMale() ? prefix + Relationship.FATHER : prefix + Relationship.MOTHER;
            return new Relationship(relationshipName, relationship.getSubject2Distance(), relationship.getSubject1Via(),
                    relationship.getSubject2Via());
        }
        if (relationship.getSubject2Distance() == 0) {
            String prefix = getGrandPrefix(relationship.getSubject1Distance());
            String relationshipName = person1.isMale() ? prefix + Relationship.SON : prefix + Relationship.DAUGHTER;
            return new Relationship(relationshipName, relationship.getSubject1Distance(), relationship.getSubject1Via(),
                    relationship.getSubject2Via());
        }

        // Check for sibling relationships
        if (relationship.getSubject1Distance() == 1 && relationship.getSubject2Distance() == 1) {
            return determineSiblingRelationship(person1, person2);
        }

        // If either (but not both) has a distance of 1, we are looking at an
        // [great]*-aunt or -uncle relationship
        if (relationship.getSubject1Distance() == 1) {
            // aunt or uncle
            String greats = getGreatPrefix(relationship.getSubject2Distance());
            String name = person1.isMale() ? greats + "uncle" : greats + "aunt";
            return new Relationship(name, relationship.getSubject1Distance() + relationship.getSubject2Distance(),
                    relationship.getSubject1Via(), relationship.getSubject2Via());
        } else if (relationship.getSubject2Distance() == 1) {
            // nephew or niece
            String greats = getGreatPrefix(relationship.getSubject1Distance());
            String name = person1.isMale() ? greats + "nephew" : greats + "niece";
            return new Relationship(name, relationship.getSubject1Distance() + relationship.getSubject2Distance(),
                    relationship.getSubject1Via(), relationship.getSubject2Via());
        }

        // Check for cousin relationship (everything else > 1).
        // Cousin relationships are symmetrical.
        int minDistance = Math.min(relationship.getSubject1Distance(), relationship.getSubject2Distance());
        int number = minDistance - 1;
        String ordinal = getOrdinalNumber(number);
        int diff = Math.abs(relationship.getSubject1Distance() - relationship.getSubject2Distance());
        String times;
        switch (diff) {
            case 0: times = ""; break;
            case 1: times = "once"; break;
            case 3: times = "twice"; break;
            default: times = getCardinalNumber(diff) + " times"; break;
        }

        String cous = ordinal + " cousin";
        if (!times.isEmpty()) {
            cous += ", " + times + " removed";
        }
        return new Relationship(cous, relationship.getSubject1Distance() + relationship.getSubject2Distance(),
                relationship.getSubject1Via(), relationship.getSubject2Via());
    }

    private Relationship determineSiblingRelationship(@NonNull Person person1, @NonNull Person person2) {
        if (person1.isSibling(person2)) {
            if (person1.getFamily().getId() == person2.getFamily().getId()) {
                String name = person1.isMale() ? Relationship.BROTHER : Relationship.SISTER;
                return new Relationship(name, 2, null, null);
            } else {
                String name = person1.isMale() ? "half-brother" : "half-sister";
                return new Relationship(name, 2, null, null);
            }
        }
        return null;
    }

    private String getGreatPrefix(int distance) {
        int numGreats = distance - 2;
        String greats;
        if (numGreats > 3) {
            greats = numGreats + " x great-";
        } else {
            greats = StringUtils.repeat("great-", numGreats);
        }
        return greats;
    }

    private String getGrandPrefix(int distance) {
        if (distance < 2) {
            return "";
        }

        String prefix = "grand";
        if (distance > 2) {
            String greats = getGreatPrefix(distance);
            prefix = greats + prefix;
        }
        return prefix;
    }

    private static String getOrdinalNumber(int num) {
        String ord;
        switch (num) {
            case 1: ord = "first"; break;
            case 2: ord = "second"; break;
            case 3: ord = "third"; break;
            case 4: ord = "fourth"; break;
            case 5: ord = "fifth"; break;
            case 6: ord = "sixth"; break;
            case 7: ord = "seventh"; break;
            case 8: ord = "eighth"; break;
            case 9: ord = "ninth"; break;
            case 10: ord = "tenth"; break;
            case 11: ord = "eleventh"; break;
            case 12: ord = "twelfth"; break;
            case 13: ord = "thireenth"; break;
            case 14: ord = "fourteenth"; break;
            case 15: ord = "fifteenth"; break;
            case 16: ord = "sixteenth"; break;
            case 17: ord = "seventeenth"; break;
            case 18: ord = "eighteenth"; break;
            case 19: ord = "nineteenth"; break;
            case 20: ord = "twentieth"; break;
            case 21: ord = "twenty-first"; break;
            case 22: ord = "twenty-second"; break;
            case 23: ord = "twenty-third"; break;
            case 24: ord = "twenty-fourth"; break;
            case 25: ord = "twenty-fifth"; break;
            case 26: ord = "twenty-sixth"; break;
            case 27: ord = "twenty-seventh"; break;
            case 28: ord = "twenty-eighth"; break;
            case 29: ord = "twenty-ninth"; break;
            case 30: ord = "thirtieth"; break;
            case 31: ord = "thirty-first"; break;
            default: ord = "";
        }
        return ord;
    }

    private static String getCardinalNumber(int num) {
        String ord;
        switch (num) {
            case 1: ord = "one"; break;
            case 2: ord = "two"; break;
            case 3: ord = "three"; break;
            case 4: ord = "four"; break;
            case 5: ord = "five"; break;
            case 6: ord = "six"; break;
            case 7: ord = "seven"; break;
            case 8: ord = "eight"; break;
            case 9: ord = "nine"; break;
            case 10: ord = "ten"; break;
            case 11: ord = "eleven"; break;
            case 12: ord = "twelve"; break;
            case 13: ord = "thireen"; break;
            case 14: ord = "fourteen"; break;
            case 15: ord = "fifteen"; break;
            case 16: ord = "sixteen"; break;
            case 17: ord = "seventeen"; break;
            case 18: ord = "eighteen"; break;
            case 19: ord = "nineteen"; break;
            case 20: ord = "twenty"; break;
            case 21: ord = "twenty-one"; break;
            case 22: ord = "twenty-two"; break;
            case 23: ord = "twenty-three"; break;
            case 24: ord = "twenty-four"; break;
            case 25: ord = "twenty-five"; break;
            case 26: ord = "twenty-six"; break;
            case 27: ord = "twenty-seven"; break;
            case 28: ord = "twenty-eight"; break;
            case 29: ord = "twenty-nine"; break;
            case 30: ord = "thirty"; break;
            case 31: ord = "thirty-one"; break;
            default: ord = "";
        }
        return ord;
    }

    String getLogMessageForHeirWithRelationship(@NonNull Person heir, @NonNull Person deceased) {
        Relationship relationship = calculateRelationship(heir, deceased, false);
        return String.format("%d %s, %s %d %s",
                heir.getId(), heir.getName(),
                relationship == null ? "no relation to" : relationship.getName() + " of",
                deceased.getId(), deceased.getName());
    }

    String getCapitalReasonMessageForHeirWithRelationship(@NonNull Person heir, @NonNull Person deceased) {
        Relationship relationship = calculateRelationship(deceased, heir, false);
        return PersonCapitalPeriod.Reason.inheritedCapitalMessage(deceased, relationship);
    }
}
