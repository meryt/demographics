package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.List;
import lombok.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.data.util.Pair;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.domain.title.TitleInheritanceStyle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class HeirServiceTest {

    private PersonService mockPersonService;

    private HeirService service;
    private Title title;
    private Person man;
    private Person son;
    private Person son2;
    private Person daughter;
    private Person daughter2;
    private Person grandson1;

    @Before
    public void setUp() {
        mockPersonService = mock(PersonService.class);

        service = new HeirService(mockPersonService);
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

        grandson1 = new Person();
        grandson1.setId(6);
        grandson1.setSocialClass(SocialClass.GENTLEMAN);
        grandson1.setGender(Gender.MALE);
        grandson1.setBirthDate(LocalDate.of(1750, 1, 1));
        grandson1.setDeathDate(LocalDate.of(1778, 1, 1));

    }

    @Test
    public void personWithNoFamilyHasNoHeir() {
        List<Person> heirs = service.findPotentialHeirsForPerson(man, man.getDeathDate(), title.getInheritance(), true,
                false);
        assertTrue(heirs.isEmpty());
    }

    @Test
    public void personWithASonHasAnHeir() {
        addChildToPerson(man, son);

        List<Person> heirs = service.findPotentialHeirsForPerson(man, man.getDeathDate(), title.getInheritance(), true,
                false);
        assertEquals(1, heirs.size());
        assertEquals(son, heirs.get(0));
    }

    @Test
    public void personWithTwoSonsHasEldestAsHeir() {
        addChildToPerson(man, son2);
        addChildToPerson(man, son);

        List<Person> heirs = service.findPotentialHeirsForPerson(man, man.getDeathDate(), title.getInheritance(), true,
                false);

        assertEquals(1, heirs.size());
        assertEquals(son, heirs.get(0));
    }

    @Test
    public void youngerSonDoesNotInheritUntilElderIsFinishedGeneration() {
        addChildToPerson(man, son);
        addChildToPerson(man, son2);
        // The elder son dies a year before his dad. But he may have still have children, so the younger son should
        // not inherit yet.
        son.setFinishedGeneration(false);
        son.setDeathDate(man.getDeathDate().minusYears(1));

        List<Person> heirs = service.findPotentialHeirsForPerson(man, man.getDeathDate(), title.getInheritance(), true,
                false);
        assertEquals(1, heirs.size());
        assertEquals(son, heirs.get(0));
    }

    @Test
    public void youngerSonOverDaughter() {
        addChildToPerson(man, son2);
        addChildToPerson(man, daughter);
        List<Person> heirs = service.findPotentialHeirsForPerson(man, man.getDeathDate(), title.getInheritance(), true,
                false);

        assertEquals(1, heirs.size());
        assertEquals(son2, heirs.get(0));
    }

    @Test
    public void daughterCannotInheritForHeirsMale() {
        addChildToPerson(man, daughter);
        List<Person> heirs = service.findPotentialHeirsForPerson(man, man.getDeathDate(), title.getInheritance(), true,
                false);
        assertTrue(heirs.isEmpty());
    }

    @Test
    public void singleDaughterCanInheritForHeirs() {
        title.setInheritance(TitleInheritanceStyle.HEIRS_OF_THE_BODY);
        addChildToPerson(man, daughter);
        List<Person> heirs = service.findPotentialHeirsForPerson(man, man.getDeathDate(), title.getInheritance(), true,
                false);

        assertEquals(1, heirs.size());
        assertEquals(daughter, heirs.get(0));
    }

    @Test
    public void twoChildlessDaughtersLastLivingInherits() {
        title.setInheritance(TitleInheritanceStyle.HEIRS_OF_THE_BODY);
        addChildToPerson(man, daughter);
        addChildToPerson(man, daughter2);
        List<Person> heirs = service.findPotentialHeirsForPerson(man, man.getDeathDate(), title.getInheritance(), true,
                false);
        assertEquals(2, heirs.size());
        Pair<Person, LocalDate> heir = service.findHeirForPerson(man, man.getDeathDate(), title.getInheritance(), true,
                false);
        assertNotNull(heir);
        assertEquals(daughter2, heir.getFirst());
        assertEquals(daughter.getDeathDate(), heir.getSecond());
    }

    @Test
    public void twoDaughtersLastLivingInheritsIfNoAdultChildren() {
        title.setInheritance(TitleInheritanceStyle.HEIRS_OF_THE_BODY);
        addChildToPerson(man, daughter);
        addChildToPerson(man, daughter2);
        addChildToPerson(daughter2, grandson1);
        grandson1.setDeathDate(grandson1.getBirthDate().plusYears(3));
        Pair<Person, LocalDate> heir = service.findHeirForPerson(man, man.getDeathDate(), title.getInheritance(), true,
                false);
        assertNotNull(heir);
        assertEquals(daughter2, heir.getFirst());
        assertEquals(daughter.getDeathDate(), heir.getSecond());
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
