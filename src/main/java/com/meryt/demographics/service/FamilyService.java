package com.meryt.demographics.service;

import java.time.LocalDate;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import lombok.NonNull;
import org.springframework.stereotype.Service;

@Service
public class FamilyService {

    /**
     * Get the calculated social class for a child of this relationship.
     *
     * @param forceCalculation if false, an existing value will not be replaced
     * @return the calculated class
     */
    public SocialClass getCalculatedChildSocialClass(@NonNull Family family,
                                                     Person child,
                                                     boolean forceCalculation,
                                                     LocalDate onDate) {
        Person father = family.getHusband();
        Person mother = family.getWife();
        if (father == null) {
            if (mother == null) {
                return null;
            } else {
                return getCalculatedChildSocialClassFromMother(mother, forceCalculation, onDate);
            }
        } else if (mother == null) {
            return getCalculatedChildSocialClassFromFather(father, forceCalculation, child, onDate);
        } else {
            // Neither parent is null
            SocialClass fathersCalculatedClass = getCalculatedSocialClass(father, forceCalculation, onDate);
            if (fathersCalculatedClass == null) {
                return getCalculatedChildSocialClassFromMother(mother, forceCalculation, onDate);
            }
            SocialClass mothersCalculatedClass = getCalculatedSocialClass(mother, forceCalculation, onDate);
            if (mothersCalculatedClass == null) {
                return getCalculatedChildSocialClassFromFather(father, forceCalculation, child, onDate);
            }
            // Neither parent's class is null
            if (mothersCalculatedClass.getRank() > (fathersCalculatedClass.getRank() + 2)) {
                return SocialClass.fromRank(mothersCalculatedClass.getRank() - 2);
            } else if (mothersCalculatedClass.getRank() > fathersCalculatedClass.getRank()) {
                return fathersCalculatedClass;
            } else {
                return getCalculatedChildSocialClassFromFather(father, forceCalculation, child, onDate);
            }
        }
    }

    private SocialClass getCalculatedChildSocialClassFromMother(@NonNull Person mother,
                                                                boolean forceCalculation,
                                                                LocalDate onDate) {
        SocialClass mothers = getCalculatedSocialClass(mother, forceCalculation, onDate);
        if (mothers == null) {
            return null;
        } else if (SocialClass.LANDOWNER_OR_CRAFTSMAN.getRank() < mothers.getRank()) {
            return SocialClass.fromRank(mothers.getRank() - 2);
        } else if (SocialClass.LANDOWNER_OR_CRAFTSMAN == mothers) {
            return SocialClass.LABORER;
        } else {
            return mothers;
        }
    }

    private SocialClass getCalculatedChildSocialClassFromFather(@NonNull Person father,
                                                                boolean forceCalculation,
                                                                Person child,
                                                                LocalDate onDate) {
        SocialClass fathers = getCalculatedSocialClass(father, forceCalculation, onDate);
        if (fathers == null) {
            return null;
        }
        // If father and grandfather are both gentlemen, child remains a gentleman.
        if (fathers == SocialClass.GENTLEMAN) {
            Family fathersFamily = loadFamily(father.getFamilyId());
            Person grandfather = fathersFamily == null ? null : fathersFamily.getHusband();
            if (grandfather == null) {
                return getChildSocialClassFromFather(father, fathers, child, onDate);
            }
        }

        return null;
    }

    /**
     * Get the child's social class based on the father's. If the is the firstborn living son, he has the same social
     * class, otherwise is minus one.
     *
     * @param father the child's father
     * @param fathersCalculatedClass the calculated class for the father
     * @param child optionally include the child since we may need to know whether he is the firstborn son
     * @param onDate optionally pass in a date for determining whether he is firstborn surviving son
     * @return the calculated child's class based only on the father's
     */
    private SocialClass getChildSocialClassFromFather(@NonNull Person father,
                                                      @NonNull SocialClass fathersCalculatedClass,
                                                      Person child,
                                                      LocalDate onDate) {
        if (child == null) {
            return fathersCalculatedClass.minusOne();
        } else if (onDate != null && father.isLiving(onDate)) {
            return fathersCalculatedClass.minusOne();
        } else if (child.isFirstbornSurvivingSonOfFather(onDate)) {
            return fathersCalculatedClass;
        } else {
            return fathersCalculatedClass.minusOne();
        }
    }

    /**
     * Return the person's social class based on his family's.
     *
     * @param forceCalculation if false, will return the existing value if set
     * @return the calculated social class
     */
    private SocialClass getCalculatedSocialClass(@NonNull Person person,
                                                boolean forceCalculation,
                                                LocalDate onDate) {
        SocialClass socialClass = person.getSocialClass();
        if (socialClass != null && !forceCalculation) {
            return socialClass;
        }

        Family family = loadFamily(person.getFamilyId());
        if (family == null) {
            return socialClass;
        }
        SocialClass calculatedClass = getCalculatedChildSocialClass(family, null, forceCalculation, onDate);
        if (calculatedClass == null) {
            return socialClass;
        } else {
            if (forceCalculation && socialClass != null && socialClass.ordinal() > calculatedClass.ordinal()) {
                return socialClass;
            } else {
                return calculatedClass;
            }
        }
    }

    /**
     * Loads a family from the database
     *
     * @param familyId the ID of the family
     * @return a Family or null if none found
     */
    private Family loadFamily(Long familyId) {
        // TODO implement repository method
        return null;
    }

}
