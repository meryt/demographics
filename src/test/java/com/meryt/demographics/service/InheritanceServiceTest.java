package com.meryt.demographics.service;

import java.time.LocalDate;
import javafx.scene.Parent;
import lombok.NonNull;
import org.junit.Before;
import org.junit.Test;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class InheritanceServiceTest {

    private InheritanceService service;
    private Title title;
    private Person man;
    private Person son;
    private Person son2;
    private Person daughter;
    private Person daughter2;

    @Before
    public void setUp() {
        service = new InheritanceService();
        title = new Title();
        title.setId(1);
        title.setName("Lord Foo");
        title.setSocialClass(SocialClass.BARONET);
        title.setInheritance(TitleInheritanceStyle.HEIRS_MALE_OF_THE_BODY);
        title.setInheritanceRoot(man);

        man = new Person();
        man.setId(1);
        man.setSocialClass(SocialClass.BARONET);
        man.setGender(Gender.MALE);
        man.setBirthDate(LocalDate.of(1700, 1, 1));
        man.setDeathDate(LocalDate.of(1750, 1, 1));
        man.addOrUpdateTitle(title, LocalDate.of(1720, 1, 1), null);

        son = new Person();
        son.setId(2);
        son.setSocialClass(SocialClass.BARONET);
        son.setGender(Gender.MALE);
        son.setBirthDate(LocalDate.of(1730, 1, 1));
        son.setDeathDate(LocalDate.of(1780, 1, 1));

        son2 = new Person();
        son2.setId(3);
        son2.setSocialClass(SocialClass.GENTLEMAN);
        son2.setGender(Gender.MALE);
        son2.setBirthDate(LocalDate.of(1731, 1, 1));
        son2.setDeathDate(LocalDate.of(1790, 1, 1));

        daughter = new Person();
        daughter.setFinishedGeneration(true);
        daughter.setId(4);
        daughter.setSocialClass(SocialClass.GENTLEMAN);
        daughter.setGender(Gender.FEMALE);
        daughter.setBirthDate(LocalDate.of(1730, 1, 1));
        daughter.setDeathDate(LocalDate.of(1780, 1, 1));

        daughter2 = new Person();
        daughter2.setFinishedGeneration(true);
        daughter2.setId(5);
        daughter2.setSocialClass(SocialClass.GENTLEMAN);
        daughter2.setGender(Gender.FEMALE);
        daughter2.setBirthDate(LocalDate.of(1735, 1, 1));
        daughter2.setDeathDate(LocalDate.of(1785, 1, 1));

    }

    @Test
    public void personWithNoFamilyHasNoHeir() {
        assertNull(service.findHeirForPerson(man, man.getDeathDate(), title.getInheritance(), title.getInheritanceRoot()));
    }

    @Test
    public void personWithASonHasAnHeir() {
        addChildToPerson(man, son);

        assertEquals(son, service.findHeirForPerson(man, man.getDeathDate(), title.getInheritance(),
                title.getInheritanceRoot()));
    }

    @Test
    public void personWithTwoSonsHasEldestAsHeir() {
        addChildToPerson(man, son2);
        addChildToPerson(man, son);

        assertEquals(son, service.findHeirForPerson(man, man.getDeathDate(), title.getInheritance(),
                title.getInheritanceRoot()));
    }

    @Test
    public void youngerSonOverDaughter() {
        addChildToPerson(man, son2);
        addChildToPerson(man, daughter);
        assertEquals(son2, service.findHeirForPerson(man, man.getDeathDate(), title.getInheritance(),
                title.getInheritanceRoot()));
    }

    @Test
    public void daughterCannotInheritForHeirsMale() {
        addChildToPerson(man, daughter);
        assertNull(service.findHeirForPerson(man, man.getDeathDate(), title.getInheritance(),
                title.getInheritanceRoot()));
    }

    @Test
    public void singleDaughterCanInheritForHeirs() {
        title.setInheritance(TitleInheritanceStyle.HEIRS_OF_THE_BODY);
        addChildToPerson(man, daughter);
        assertEquals(daughter, service.findHeirForPerson(man, man.getDeathDate(), title.getInheritance(),
                title.getInheritanceRoot()));
    }

    @Test
    public void twoChildlessDaughtersLastLivingInherits() {
        title.setInheritance(TitleInheritanceStyle.HEIRS_OF_THE_BODY);
        addChildToPerson(man, daughter);
        addChildToPerson(man, daughter2);
        assertEquals(daughter2, service.findHeirForPerson(man, man.getDeathDate(), title.getInheritance(),
                title.getInheritanceRoot()));
        assertEquals(daughter.getDeathDate(), daughter2.getTitles().get(0).getFromDate());
    }

    private void addChildToPerson(@NonNull Person parent, @NonNull Person child) {
        Family family = new Family();
        family.addChild(child);
        if (parent.isMale()) {
            parent.addFatheredFamily(family);
        } else {
            parent.addMotheredFamily(family);
        }
    }

}
