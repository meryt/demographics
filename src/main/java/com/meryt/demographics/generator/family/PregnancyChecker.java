package com.meryt.demographics.generator.family;

import java.time.LocalDate;
import java.util.List;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.generator.PersonGenerator;
import lombok.NonNull;
import lombok.Setter;

@Setter
public class PregnancyChecker {

    private final PersonGenerator personGenerator;
    private final Person mother;
    private final boolean allowMaternalDeath;
    private final Maternity maternity;

    public PregnancyChecker(@NonNull PersonGenerator personGenerator, @NonNull Person mother, boolean allowMaternalDeath) {
        this.mother = mother;
        this.allowMaternalDeath = allowMaternalDeath;
        this.maternity = (Maternity) mother.getFertility();
        this.personGenerator = personGenerator;
    }

    public void checkDateRange(@NonNull Person father,
                               @NonNull LocalDate startDate,
                               @NonNull LocalDate endDate,
                               boolean stopAtFirstChild) {

        boolean stopOnNextRound = false;
        LocalDate currentDay = startDate;
        LocalDate endBeforeDate = endDate.plusDays(1);
        this.maternity.setFather(father);

        while (!stopOnNextRound && (currentDay.isBefore(endBeforeDate)) && mother.isLiving(currentDay)) {
            checkDay(currentDay);
            currentDay = currentDay.plusDays(1);
        }
    }

    private void checkDay(@NonNull LocalDate day) {
        if (maternity.isPregnant(day)) {
            if (day.isEqual(maternity.getDueDate())) {
                giveBirth(day);
            }
        }
    }

    private void giveBirth(@NonNull LocalDate day) {
        createChildren(maternity.getFather(), mother, day, maternity.isCarryingIdenticalTwins(),
                maternity.isCarryingFraternalTwins());
    }

    private void createChildren(@NonNull Person father,
                                @NonNull Person mother,
                                @NonNull LocalDate birthDate,
                                boolean includeIdenticalTwin,
                                boolean includeFraternalTwin) {
        List<Person> children = personGenerator.generateChildrenForParents(father, mother, birthDate,
                includeIdenticalTwin, includeFraternalTwin);
        // TDOO

    }
}
