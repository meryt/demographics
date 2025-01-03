package com.meryt.demographics.service;

import java.time.LocalDate;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

@Service
public class SocialClassService {
    /**
     * Get the calculated social class for a child of this relationship.
     *
     * @param forceCalculation if false, an existing value will not be replaced
     * @return the calculated class
     */
    public SocialClass getCalculatedChildSocialClass(Person father,
                                                     Person mother,
                                                     Person child,
                                                     boolean forceCalculation,
                                                     LocalDate onDate) {
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
        SocialClass fathersClass = getCalculatedSocialClass(father, forceCalculation, onDate);
        if (fathersClass == null) {
            return null;
        }
        // If father is a gentleman and grandfather higher than gentleman, child remains a gentleman.
        if (fathersClass == SocialClass.GENTLEMAN) {
            Family fathersFamily = father.getFamily();
            Person grandfather = fathersFamily == null ? null : fathersFamily.getHusband();
            if (grandfather == null) {
                return getChildSocialClassFromFather(fathersClass, child, onDate);
            } else {
                SocialClass grandfathers = getCalculatedSocialClass(grandfather, forceCalculation, onDate);
                if (grandfathers.getRank() > SocialClass.GENTLEMAN.getRank()) {
                    return SocialClass.GENTLEMAN;
                } else {
                    return getChildSocialClassFromFather(fathersClass, child, onDate);
                }
            }
        }

        // If the father has a high nobility, the child loses 1 point unless firstborn
        else if (fathersClass.getRank() > SocialClass.GENTLEMAN.getRank()) {
            return getChildSocialClassFromFather(fathersClass, child, onDate);
        }

        // Otherwise if father is lowborn, child is at same rank
        else {
            return fathersClass;
        }
    }

    /**
     * Get the child's social class based on the father's. If the is the firstborn living son, he has the same social
     * class, otherwise is minus one.
     *
     * @param fathersCalculatedClass the calculated class for the father
     * @param child optionally include the child since we may need to know whether he is the firstborn son
     * @param onDate optionally pass in a date for determining whether he is firstborn surviving son
     * @return the calculated child's class based only on the father's
     */
    private SocialClass getChildSocialClassFromFather(@NonNull SocialClass fathersCalculatedClass,
                                                      Person child,
                                                      LocalDate onDate) {
        if (child == null || onDate == null || !child.isFirstbornSurvivingSonOfFather(onDate)) {
            return fathersCalculatedClass.minusOne();
        } else {
            return fathersCalculatedClass;
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

        Family family = person.getFamily();
        if (family == null) {
            return socialClass;
        }
        SocialClass calculatedClass = getCalculatedChildSocialClass(family.getHusband(), family.getWife(), null,
                forceCalculation, onDate);
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
}
