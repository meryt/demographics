package com.meryt.demographics.service;

import java.time.LocalDate;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import lombok.NonNull;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FamilyServiceTest {

    private FamilyService familyService;

    @Before
    public void setUp() {
        familyService = new FamilyService();
    }

    @Test
    public void getCalculatedChildSocialClassReturnsNullForNullParents() {
        Family family = new Family();
        Person child = new Person();
        assertNull(familyService.getCalculatedChildSocialClass(family, child, false, LocalDate.of(1700, 1, 1)));
    }

    @Test
    public void getCalculatedChildSocialClassFromMotherOnly() {
        Family family = new Family();
        Person mother = new Person();
        mother.setGender(Gender.FEMALE);
        family.setWife(mother);
        Person child = new Person();
        family.getChildren().add(child);

        testSocialClassFromMother(family, null, null);
        testSocialClassFromMother(family, SocialClass.PAUPER, SocialClass.PAUPER);
        testSocialClassFromMother(family, SocialClass.LABORER, SocialClass.LABORER);
        testSocialClassFromMother(family, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.LABORER);
        testSocialClassFromMother(family, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.LABORER);
        testSocialClassFromMother(family, SocialClass.GENTLEMAN, SocialClass.LANDOWNER_OR_CRAFTSMAN);
        testSocialClassFromMother(family, SocialClass.BARONET, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromMother(family, SocialClass.BARON, SocialClass.GENTLEMAN);
        testSocialClassFromMother(family, SocialClass.VISCOUNT, SocialClass.BARONET);
        testSocialClassFromMother(family, SocialClass.EARL, SocialClass.BARON);
        testSocialClassFromMother(family, SocialClass.MARQUESS, SocialClass.VISCOUNT);
        testSocialClassFromMother(family, SocialClass.DUKE, SocialClass.EARL);
        testSocialClassFromMother(family, SocialClass.PRINCE, SocialClass.MARQUESS);
        testSocialClassFromMother(family, SocialClass.MONARCH, SocialClass.DUKE);
    }

    @Test
    public void getCalculatedChildClassFromFatherOnly() {
        Family family = new Family();
        Person father = new Person();
        father.setGender(Gender.MALE);
        family.setHusband(father);
        Person child = new Person();
        family.getChildren().add(child);

        testSocialClassFromFather(family, null, null);
        testSocialClassFromFather(family, SocialClass.PAUPER, SocialClass.PAUPER);
        testSocialClassFromFather(family, SocialClass.LABORER, SocialClass.LABORER);
        testSocialClassFromFather(family, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.LANDOWNER_OR_CRAFTSMAN);
        testSocialClassFromFather(family, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromFather(family, SocialClass.GENTLEMAN, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromFather(family, SocialClass.BARONET, SocialClass.GENTLEMAN);
        testSocialClassFromFather(family, SocialClass.BARON, SocialClass.BARONET);
        testSocialClassFromFather(family, SocialClass.VISCOUNT, SocialClass.BARON);
        testSocialClassFromFather(family, SocialClass.EARL, SocialClass.VISCOUNT);
        testSocialClassFromFather(family, SocialClass.MARQUESS, SocialClass.EARL);
        testSocialClassFromFather(family, SocialClass.DUKE, SocialClass.MARQUESS);
        testSocialClassFromFather(family, SocialClass.PRINCE, SocialClass.DUKE);
        testSocialClassFromFather(family, SocialClass.MONARCH, SocialClass.PRINCE);
    }

    @Test
    public void getCalculatedChildClassFromBothParents() {
        Family family = new Family();
        Person father = new Person();
        father.setGender(Gender.MALE);
        family.setHusband(father);
        Person mother = new Person();
        mother.setGender(Gender.FEMALE);
        family.setWife(mother);
        Person child = new Person();
        family.getChildren().add(child);

        testSocialClassFromBothParents(family, null, null, null);
        testSocialClassFromBothParents(family, SocialClass.PAUPER, SocialClass.PAUPER, SocialClass.PAUPER);
        testSocialClassFromBothParents(family, SocialClass.LABORER, SocialClass.PAUPER, SocialClass.LABORER);
        testSocialClassFromBothParents(family, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.PAUPER, SocialClass.LANDOWNER_OR_CRAFTSMAN);
        testSocialClassFromBothParents(family, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.PAUPER, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromBothParents(family, SocialClass.GENTLEMAN, SocialClass.PAUPER, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromBothParents(family, SocialClass.BARONET, SocialClass.PAUPER, SocialClass.GENTLEMAN);
        testSocialClassFromBothParents(family, SocialClass.BARON, SocialClass.PAUPER, SocialClass.BARONET);
        testSocialClassFromBothParents(family, SocialClass.VISCOUNT, SocialClass.PAUPER, SocialClass.BARON);
        testSocialClassFromBothParents(family, SocialClass.EARL, SocialClass.PAUPER, SocialClass.VISCOUNT);
        testSocialClassFromBothParents(family, SocialClass.MARQUESS, SocialClass.PAUPER, SocialClass.EARL);
        testSocialClassFromBothParents(family, SocialClass.DUKE, SocialClass.PAUPER, SocialClass.MARQUESS);
        testSocialClassFromBothParents(family, SocialClass.PRINCE, SocialClass.PAUPER, SocialClass.DUKE);
        testSocialClassFromBothParents(family, SocialClass.MONARCH, SocialClass.PAUPER, SocialClass.PRINCE);

        // If mother's class is more than 2 higher, we ignore the father
        testSocialClassFromBothParents(family, SocialClass.PAUPER, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.LABORER);
        testSocialClassFromBothParents(family, SocialClass.PAUPER, SocialClass.GENTLEMAN, SocialClass.LANDOWNER_OR_CRAFTSMAN);
        testSocialClassFromBothParents(family, SocialClass.LABORER, SocialClass.BARONET, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromBothParents(family, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.BARON, SocialClass.GENTLEMAN);
        testSocialClassFromBothParents(family, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.VISCOUNT, SocialClass.BARONET);
        testSocialClassFromBothParents(family, SocialClass.GENTLEMAN, SocialClass.EARL, SocialClass.BARON);
        testSocialClassFromBothParents(family, SocialClass.BARONET, SocialClass.MARQUESS, SocialClass.VISCOUNT);
        testSocialClassFromBothParents(family, SocialClass.BARON, SocialClass.DUKE, SocialClass.EARL);
        testSocialClassFromBothParents(family, SocialClass.VISCOUNT, SocialClass.PRINCE, SocialClass.MARQUESS);
        testSocialClassFromBothParents(family, SocialClass.EARL, SocialClass.MONARCH, SocialClass.DUKE);

        // If mother's class is 1 or 2 levels higher we use the father's actual class
        testSocialClassFromBothParents(family, SocialClass.PAUPER, SocialClass.LABORER, SocialClass.PAUPER);
        testSocialClassFromBothParents(family, SocialClass.LABORER, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.LABORER);
        testSocialClassFromBothParents(family, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.LANDOWNER_OR_CRAFTSMAN);
        testSocialClassFromBothParents(family, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.GENTLEMAN, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromBothParents(family, SocialClass.GENTLEMAN, SocialClass.BARONET, SocialClass.GENTLEMAN);
        testSocialClassFromBothParents(family, SocialClass.BARONET, SocialClass.BARON, SocialClass.BARONET);
        testSocialClassFromBothParents(family, SocialClass.BARON, SocialClass.VISCOUNT, SocialClass.BARON);
        testSocialClassFromBothParents(family, SocialClass.VISCOUNT, SocialClass.EARL, SocialClass.VISCOUNT);
        testSocialClassFromBothParents(family, SocialClass.EARL, SocialClass.MARQUESS, SocialClass.EARL);
        testSocialClassFromBothParents(family, SocialClass.MARQUESS, SocialClass.DUKE, SocialClass.MARQUESS);
        testSocialClassFromBothParents(family, SocialClass.DUKE, SocialClass.PRINCE, SocialClass.DUKE);
        testSocialClassFromBothParents(family, SocialClass.PRINCE, SocialClass.MONARCH, SocialClass.PRINCE);
    }

    private void testSocialClassFromMother(@NonNull Family family, SocialClass mothers, SocialClass expected) {
        family.getWife().setSocialClass(mothers);
        assertEquals(expected, familyService.getCalculatedChildSocialClass(family, family.getChildren().get(0),
                false, LocalDate.of(1700, 1, 1)));
    }

    private void testSocialClassFromFather(@NonNull Family family, SocialClass fathers, SocialClass expected) {
        family.getHusband().setSocialClass(fathers);
        assertEquals(expected, familyService.getCalculatedChildSocialClass(family, family.getChildren().get(0),
                false, LocalDate.of(1700, 1, 1)));

    }

    private void testSocialClassFromBothParents(@NonNull Family family,
                                                SocialClass fathers,
                                                SocialClass mothers,
                                                SocialClass expected) {
        family.getHusband().setSocialClass(fathers);
        family.getWife().setSocialClass(mothers);
        assertEquals(expected, familyService.getCalculatedChildSocialClass(family, family.getChildren().get(0),
                false, LocalDate.of(1700, 1, 1)));
    }

}
