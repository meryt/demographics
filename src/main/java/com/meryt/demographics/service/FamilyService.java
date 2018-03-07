package com.meryt.demographics.service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.generator.family.FamilyGenerator;
import com.meryt.demographics.request.FamilyParameters;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FamilyService {

    private final FamilyGenerator familyGenerator;

    public FamilyService(@Autowired FamilyGenerator familyGenerator) {
        this.familyGenerator = familyGenerator;
    }

    public Family generateFamily(@NonNull FamilyParameters familyParameters) {
        return familyGenerator.generate(familyParameters);
    }

    /**
     * Get the calculated social class for a child of this relationship.
     *
     * @param forceCalculation if false, an existing value will not be replaced
     * @return the calculated class
     */
    public SocialClass getCalculatedChildSocialClass(@NonNull Family family,
                                                     Person child,
                                                     boolean forceCalculation) {
        Person father = family.getHusband();
        Person mother = family.getWife();
        if (father == null) {
            if (mother == null) {
                return null;
            } else {
                return getCalculatedChildSocialClassFromMother(mother, forceCalculation);
            }
        } else {
            if (mother == null) {
                return getCalculatedChildSocialClassFromFather(father, forceCalculation, child);
            }
        }
        // TODO
        return null;
    }

    private SocialClass getCalculatedChildSocialClassFromMother(@NonNull Person mother, boolean forceCalculation) {
        SocialClass mothers = getCalculatedSocialClass(mother, forceCalculation);
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

    private SocialClass getCalculatedChildSocialClassFromFather(@NonNull Person father, boolean forceCalculation,
                                                                Person child) {
        SocialClass fathers = getCalculatedSocialClass(father, forceCalculation);
        if (fathers == null) {
            return null;
        }
        // If father and grandfather are both gentlemen, child remains a gentleman.
        if (fathers == SocialClass.GENTLEMAN) {
            Family fathersFamily = loadFamily(father.getFamilyId());
            //if (fathersFamily != null && )
        }

        return null;
    }

    /**
     * Return the person's social class based on his family's.
     *
     * @param forceCalculation if false, will return the existing value if set
     * @return the calculated social class
     */
    public SocialClass getCalculatedSocialClass(@NonNull Person person, boolean forceCalculation) {
        SocialClass socialClass = person.getSocialClass();
        if (socialClass != null && !forceCalculation) {
            return socialClass;
        }

        Family family = loadFamily(person.getFamilyId());
        if (family == null) {
            return socialClass;
        }
        SocialClass calculatedClass = getCalculatedChildSocialClass(family, null, forceCalculation);
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
    public Family loadFamily(Long familyId) {
        // TODO implement repository method
        return null;
    }

}
