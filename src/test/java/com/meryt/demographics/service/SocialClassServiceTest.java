package com.meryt.demographics.service;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SocialClassServiceTest {

    private SocialClassService socialClassService;

    @BeforeEach
    public void setUp() {
        socialClassService = new SocialClassService();
    }

    @Test
    public void getCalculatedChildSocialClassReturnsNullForNullParents() {
        Person child = new Person();
        assertNull(socialClassService.getCalculatedChildSocialClass(null, null, child, false, LocalDate.of(1700, 1, 1)));
    }

    @Test
    public void getCalculatedChildSocialClassFromMotherOnly() {
        Person mother = new Person();
        mother.setGender(Gender.FEMALE);

        testSocialClassFromMother(mother, null, null);
        testSocialClassFromMother(mother, SocialClass.PAUPER, SocialClass.PAUPER);
        testSocialClassFromMother(mother, SocialClass.LABORER, SocialClass.LABORER);
        testSocialClassFromMother(mother, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.LABORER);
        testSocialClassFromMother(mother, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.LABORER);
        testSocialClassFromMother(mother, SocialClass.GENTLEMAN, SocialClass.LANDOWNER_OR_CRAFTSMAN);
        testSocialClassFromMother(mother, SocialClass.BARONET, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromMother(mother, SocialClass.BARON, SocialClass.GENTLEMAN);
        testSocialClassFromMother(mother, SocialClass.VISCOUNT, SocialClass.BARONET);
        testSocialClassFromMother(mother, SocialClass.EARL, SocialClass.BARON);
        testSocialClassFromMother(mother, SocialClass.MARQUESS, SocialClass.VISCOUNT);
        testSocialClassFromMother(mother, SocialClass.DUKE, SocialClass.EARL);
        testSocialClassFromMother(mother, SocialClass.PRINCE, SocialClass.MARQUESS);
        testSocialClassFromMother(mother, SocialClass.MONARCH, SocialClass.DUKE);
    }

    @Test
    public void getCalculatedChildClassFromFatherOnly() {
        Person father = new Person();
        father.setGender(Gender.MALE);
        Person child = new Person();

        testSocialClassFromFather(father, child, null, null);
        testSocialClassFromFather(father, child, SocialClass.PAUPER, SocialClass.PAUPER);
        testSocialClassFromFather(father, child, SocialClass.LABORER, SocialClass.LABORER);
        testSocialClassFromFather(father, child, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.LANDOWNER_OR_CRAFTSMAN);
        testSocialClassFromFather(father, child, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromFather(father, child, SocialClass.GENTLEMAN, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromFather(father, child, SocialClass.BARONET, SocialClass.GENTLEMAN);
        testSocialClassFromFather(father, child, SocialClass.BARON, SocialClass.BARONET);
        testSocialClassFromFather(father, child, SocialClass.VISCOUNT, SocialClass.BARON);
        testSocialClassFromFather(father, child, SocialClass.EARL, SocialClass.VISCOUNT);
        testSocialClassFromFather(father, child, SocialClass.MARQUESS, SocialClass.EARL);
        testSocialClassFromFather(father, child, SocialClass.DUKE, SocialClass.MARQUESS);
        testSocialClassFromFather(father, child, SocialClass.PRINCE, SocialClass.DUKE);
        testSocialClassFromFather(father, child, SocialClass.MONARCH, SocialClass.PRINCE);
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

        testSocialClassFromBothParents(father, mother, child, null, null, null);
        testSocialClassFromBothParents(father, mother, child, SocialClass.PAUPER, SocialClass.PAUPER, SocialClass.PAUPER);
        testSocialClassFromBothParents(father, mother, child, SocialClass.LABORER, SocialClass.PAUPER, SocialClass.LABORER);
        testSocialClassFromBothParents(father, mother, child, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.PAUPER, SocialClass.LANDOWNER_OR_CRAFTSMAN);
        testSocialClassFromBothParents(father, mother, child, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.PAUPER, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromBothParents(father, mother, child, SocialClass.GENTLEMAN, SocialClass.PAUPER, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromBothParents(father, mother, child, SocialClass.BARONET, SocialClass.PAUPER, SocialClass.GENTLEMAN);
        testSocialClassFromBothParents(father, mother, child, SocialClass.BARON, SocialClass.PAUPER, SocialClass.BARONET);
        testSocialClassFromBothParents(father, mother, child, SocialClass.VISCOUNT, SocialClass.PAUPER, SocialClass.BARON);
        testSocialClassFromBothParents(father, mother, child, SocialClass.EARL, SocialClass.PAUPER, SocialClass.VISCOUNT);
        testSocialClassFromBothParents(father, mother, child, SocialClass.MARQUESS, SocialClass.PAUPER, SocialClass.EARL);
        testSocialClassFromBothParents(father, mother, child, SocialClass.DUKE, SocialClass.PAUPER, SocialClass.MARQUESS);
        testSocialClassFromBothParents(father, mother, child, SocialClass.PRINCE, SocialClass.PAUPER, SocialClass.DUKE);
        testSocialClassFromBothParents(father, mother, child, SocialClass.MONARCH, SocialClass.PAUPER, SocialClass.PRINCE);

        // If mother's class is more than 2 higher, we ignore the father
        testSocialClassFromBothParents(father, mother, child, SocialClass.PAUPER, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.LABORER);
        testSocialClassFromBothParents(father, mother, child, SocialClass.PAUPER, SocialClass.GENTLEMAN, SocialClass.LANDOWNER_OR_CRAFTSMAN);
        testSocialClassFromBothParents(father, mother, child, SocialClass.LABORER, SocialClass.BARONET, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromBothParents(father, mother, child, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.BARON, SocialClass.GENTLEMAN);
        testSocialClassFromBothParents(father, mother, child, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.VISCOUNT, SocialClass.BARONET);
        testSocialClassFromBothParents(father, mother, child, SocialClass.GENTLEMAN, SocialClass.EARL, SocialClass.BARON);
        testSocialClassFromBothParents(father, mother, child, SocialClass.BARONET, SocialClass.MARQUESS, SocialClass.VISCOUNT);
        testSocialClassFromBothParents(father, mother, child, SocialClass.BARON, SocialClass.DUKE, SocialClass.EARL);
        testSocialClassFromBothParents(father, mother, child, SocialClass.VISCOUNT, SocialClass.PRINCE, SocialClass.MARQUESS);
        testSocialClassFromBothParents(father, mother, child, SocialClass.EARL, SocialClass.MONARCH, SocialClass.DUKE);

        // If mother's class is 1 or 2 levels higher we use the father's actual class
        testSocialClassFromBothParents(father, mother, child, SocialClass.PAUPER, SocialClass.LABORER, SocialClass.PAUPER);
        testSocialClassFromBothParents(father, mother, child, SocialClass.LABORER, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.LABORER);
        testSocialClassFromBothParents(father, mother, child, SocialClass.LANDOWNER_OR_CRAFTSMAN, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.LANDOWNER_OR_CRAFTSMAN);
        testSocialClassFromBothParents(father, mother, child, SocialClass.YEOMAN_OR_MERCHANT, SocialClass.GENTLEMAN, SocialClass.YEOMAN_OR_MERCHANT);
        testSocialClassFromBothParents(father, mother, child, SocialClass.GENTLEMAN, SocialClass.BARONET, SocialClass.GENTLEMAN);
        testSocialClassFromBothParents(father, mother, child, SocialClass.BARONET, SocialClass.BARON, SocialClass.BARONET);
        testSocialClassFromBothParents(father, mother, child, SocialClass.BARON, SocialClass.VISCOUNT, SocialClass.BARON);
        testSocialClassFromBothParents(father, mother, child, SocialClass.VISCOUNT, SocialClass.EARL, SocialClass.VISCOUNT);
        testSocialClassFromBothParents(father, mother, child, SocialClass.EARL, SocialClass.MARQUESS, SocialClass.EARL);
        testSocialClassFromBothParents(father, mother, child, SocialClass.MARQUESS, SocialClass.DUKE, SocialClass.MARQUESS);
        testSocialClassFromBothParents(father, mother, child, SocialClass.DUKE, SocialClass.PRINCE, SocialClass.DUKE);
        testSocialClassFromBothParents(father, mother, child, SocialClass.PRINCE, SocialClass.MONARCH, SocialClass.PRINCE);
    }

    private void testSocialClassFromMother(Person mother,
                                           SocialClass mothers,
                                           SocialClass expected) {
        mother.setSocialClass(mothers);
        assertEquals(expected, socialClassService.getCalculatedChildSocialClass(null, mother, null,
                false, LocalDate.of(1700, 1, 1)));
    }

    private void testSocialClassFromFather(Person father,
                                           Person child,
                                           SocialClass fathers,
                                           SocialClass expected) {
        father.setSocialClass(fathers);
        assertEquals(expected, socialClassService.getCalculatedChildSocialClass(father, null, child,
                false, LocalDate.of(1700, 1, 1)));

    }

    private void testSocialClassFromBothParents(Person father,
                                                Person mother,
                                                Person child,
                                                SocialClass fathers,
                                                SocialClass mothers,
                                                SocialClass expected) {
        father.setSocialClass(fathers);
        mother.setSocialClass(mothers);
        assertEquals(expected, socialClassService.getCalculatedChildSocialClass(father, mother, child,
                false, LocalDate.of(1700, 1, 1)));
    }

}
