package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdInhabitantPeriod;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;
import com.meryt.demographics.repository.HouseholdInhabitantRepository;
import com.meryt.demographics.repository.HouseholdLocationRepository;
import com.meryt.demographics.repository.HouseholdRepository;
import com.meryt.demographics.time.LocalDateComparator;

@Service
@Slf4j
public class HouseholdService {

    private static final int MIN_HEAD_OF_HOUSEHOLD_AGE = 16;

    private final HouseholdRepository householdRepository;
    private final HouseholdInhabitantRepository householdInhabitantRepository;
    private final HouseholdLocationRepository householdLocationRepository;

    public HouseholdService(@Autowired @NonNull HouseholdRepository householdRepository,
                            @Autowired @NonNull HouseholdInhabitantRepository householdInhabitantRepository,
                            @Autowired @NonNull HouseholdLocationRepository householdLocationRepository) {
        this.householdRepository = householdRepository;
        this.householdInhabitantRepository = householdInhabitantRepository;
        this.householdLocationRepository = householdLocationRepository;
    }

    public Household save(@NonNull Household household) {
        return householdRepository.save(household);
    }

    public HouseholdInhabitantPeriod save(@NonNull HouseholdInhabitantPeriod householdInhabitantPeriod) {
        return householdInhabitantRepository.save(householdInhabitantPeriod);
    }

    public HouseholdLocationPeriod save(@NonNull HouseholdLocationPeriod householdLocationPeriod) {
        return householdLocationRepository.save(householdLocationPeriod);
    }

    /**
     * Finds a household by ID or returns null if none found
     */
    @Nullable
    public Household load(long householdId) {
        return householdRepository.findById(householdId).orElse(null);
    }

    public void delete(@NonNull HouseholdInhabitantPeriod householdInhabitantPeriod) {
        householdInhabitantRepository.delete(householdInhabitantPeriod);
    }

    public void delete(@NonNull HouseholdLocationPeriod householdLocationPeriod) {
        householdLocationRepository.delete(householdLocationPeriod);
    }

    /**
     * Finds all households that are not in any location.
     *
     * @param onDate the date to use for the search
     * @return a list of 0 or more households that are not in any type of location
     */
    public List<Household> loadHouseholdsWithoutLocations(@NonNull LocalDate onDate) {
        return householdRepository.findHouseholdsWithoutLocations(onDate);
    }

    /**
     * Finds all households that are in a location, but are not in a location of type DWELLING. Does not find
     * "floating" households that are not in any location.
     *
     * @param onDate the date to use for the search
     * @return a list of 0 or more households that are not in a location of type DWELLING
     */
    public List<Household> loadHouseholdsWithoutHouses(@NonNull LocalDate onDate) {
        return householdRepository.findHouseholdsWithoutHouses(onDate);
    }

    private List<Household> loadHouseholdsWithoutInhabitantsInLocations(@NonNull LocalDate onDate) {
        return householdRepository.loadHouseholdsWithoutInhabitantsInLocations(onDate);
    }

    /**
     * Finds all households that are in a location on the date but have no inhabitants. They then have their location
     * end date set to the max end date of the last person who lived in them.
     *
     * @param onDate the date on which to do the search (presumably the current date)
     * @return a list of households that were modified
     */
    public List<Household> cleanUpHouseholdsWithoutInhabitantsInLocations(@NonNull LocalDate onDate) {
        List<Household> households = loadHouseholdsWithoutInhabitantsInLocations(onDate);
        List<Household> modifiedHouseholds = new ArrayList<>();
        for (Household household : households) {
            LocalDate maxDate = household.getInhabitantPeriods().stream()
                    .map(HouseholdInhabitantPeriod::getToDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);
            if (maxDate != null && maxDate.isBefore(onDate)) {
                HouseholdLocationPeriod period = household.getHouseholdLocationPeriod(onDate);
                log.info(String.format("Household %d has no residents since %s; resetting end date of location",
                        household.getId(), maxDate));
                if (period != null && (maxDate.isAfter(period.getFromDate()) || maxDate.equals(period.getToDate()))) {
                    period.setToDate(maxDate);
                    save(period);
                    save(household);
                    modifiedHouseholds.add(household);
                }
            }
        }
        return modifiedHouseholds;
    }

    /**
     * Use this method to find and set a new head of household as of the given date. Normally used when the current
     * head of the household dies. Uses the oldest male over 16, and failing that, the oldest female over 16. If no one
     * is eligible, does nothing.
     *
     * The method attempts to set the start of the headship from the end of the previous headship, if it does not
     * happen to equal to the onDate parameter, or from the start of the residency of the new head, if that date
     * falls after the headship of the previous head.
     *
     * @param household the household whose head needs to be reset
     * @param onDate the date the previous head died or left
     */
    @Nullable
    public Person resetHeadAsOf(@NonNull Household household, @NonNull LocalDate onDate) {
        List<Person> inhabitantsByAge = household.getInhabitants(onDate).stream()
                .filter(p -> p.getBirthDate() != null && p.isLiving(onDate) &&
                        p.getAgeInYears(onDate) >= MIN_HEAD_OF_HOUSEHOLD_AGE)
                .sorted(Comparator.comparing(Person::getGender).thenComparing(Person::getBirthDate))
                .collect(Collectors.toList());
        if (inhabitantsByAge.isEmpty()) {
            return null;
        }

        if (household.getHead(onDate) != null) {
            // should never happen...
            endPersonResidence(household, household.getHead(onDate), onDate);
        }

        HouseholdInhabitantPeriod lastHeadsPeriod = household.getInhabitantPeriods().stream()
                .filter(HouseholdInhabitantPeriod::isHouseholdHead)
                .filter(hip -> hip.getToDate() != null && (hip.getToDate().isBefore(onDate) || hip.getToDate().equals(onDate)))
                .max(Comparator.comparing(HouseholdInhabitantPeriod::getToDate))
                .orElse(null);

        Person newHead = inhabitantsByAge.get(0);
        final long newHeadId = newHead.getId();

        HouseholdInhabitantPeriod newHeadsPeriod = household.getInhabitantPeriods().stream()
                .filter(hip -> hip.getPerson().getId() == newHeadId && hip.contains(onDate))
                .findFirst().orElse(null);

        LocalDate startOfHeadship;
        if (newHeadsPeriod == null) {
            // Should never happen, but...
            if (lastHeadsPeriod == null || lastHeadsPeriod.getToDate() == null) {
                startOfHeadship = onDate;
            } else {
                startOfHeadship = LocalDateComparator.min(lastHeadsPeriod.getToDate(), onDate);
            }
        } else {
            if (lastHeadsPeriod == null || lastHeadsPeriod.getToDate() == null) {
                startOfHeadship = onDate;
            } else {
                startOfHeadship = LocalDateComparator.max(newHeadsPeriod.getFromDate(), lastHeadsPeriod.getToDate());
            }
        }

        newHead = endPersonResidence(household, newHead, startOfHeadship);

        newHead = addPersonToHousehold(newHead, household, startOfHeadship, true);
        save(household);
        return newHead;
    }

    public Person addPersonToHousehold(@NonNull Person person,
                                       @NonNull Household household,
                                       @NonNull LocalDate fromDate,
                                       boolean isHead) {

        HouseholdInhabitantPeriod newPeriod = new HouseholdInhabitantPeriod();
        newPeriod.setFromDate(fromDate);
        newPeriod.setToDate(person.getDeathDate());

        for (HouseholdInhabitantPeriod period : person.getHouseholds()) {
            if (period.rangeEquals(newPeriod)) {
                // There is an existing exact match for this new open-ended range. Just move the household.
                // First remove them from the existing household.
                period.getHousehold().getInhabitantPeriods().remove(period);
                save(period.getHousehold());
                // Then set the new household.
                period.setHousehold(household);
                household.getInhabitantPeriods().add(period);
                period.setHouseholdHead(isHead);
                save(household);
                save(period);
                return person;
            } else if (period.getFromDate().isBefore(fromDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(fromDate))) {
                period.setToDate(fromDate);
                save(period.getHousehold());
                save(period);
            }
        }

        newPeriod.setHousehold(household);

        household.addInhabitantPeriod(newPeriod);

        newPeriod.setPerson(person);
        newPeriod.setPersonId(person.getId());
        newPeriod.setHouseholdHead(isHead);
        person.getHouseholds().add(newPeriod);
        save(household);
        return person;
    }

    /**
     * Tell this household that the person has moved. Cap his current residency period (if any)
     * @param person   the person
     * @param asOfDate the date upon which the person moved to another household
     */
    public Person endPersonResidence(@NonNull Household household,
                                     @NonNull Person person,
                                     @NonNull LocalDate asOfDate) {
        List<HouseholdInhabitantPeriod> periodsToDelete = new ArrayList<>();

        boolean anyModified = false;
        for (HouseholdInhabitantPeriod period : household.getInhabitantPeriods()) {
            if (period.getPersonId() == person.getId() && period.getFromDate().equals(asOfDate)) {
                periodsToDelete.add(period);
            } else if (period.getPersonId() == person.getId() &&
                    period.getFromDate().isBefore(asOfDate) &&
                    (period.getToDate() == null || period.getToDate().isAfter(asOfDate))) {
                period.setToDate(asOfDate);
                save(period);
                anyModified = true;
            }
        }

        if (anyModified) {
            save(household);
        }

        for (HouseholdInhabitantPeriod period : periodsToDelete) {
            household.getInhabitantPeriods().remove(period);
            person.getHouseholds().remove(period);
            delete(period);
            save(household);
        }

        return person;
    }

    public List<Person> addChildrenToHousehold(@NonNull Person parent,
                                               @NonNull Household household,
                                               @NonNull LocalDate onDate) {
        List<Person> movedChildren = new ArrayList<>();
        for (Person child : parent.getLivingChildren(onDate)) {
            if (child.getFamilies().isEmpty() && child.getAgeInYears(onDate) < 16
                    && !household.equals(child.getHousehold(onDate))) {
                movedChildren.add(addPersonToHousehold(child, household, onDate, false));
            }
        }
        return movedChildren;
    }

    public List<Person> addStepchildrenToHousehold(@NonNull Person stepParent,
                                                   @NonNull Family stepParentsFamily,
                                                   @NonNull Household stepParentsHousehold) {
        List<Person> movedChildren = new ArrayList<>();
        Person spouse = stepParentsFamily.getHusband().equals(stepParent)
                ? stepParentsFamily.getWife()
                : stepParentsFamily.getHusband();
        if (spouse.getFamilies().size() == 1) {
            return movedChildren;
        }
        LocalDate moveInDate = stepParentsFamily.getWeddingDate();
        if (moveInDate == null) {
            return movedChildren;
        }

        for (Family otherFamily : spouse.getFamilies()) {
            if (otherFamily.equals(stepParentsFamily)) {
                continue;
            }
            List<Person> stepchildren = otherFamily.getChildren().stream()
                    .filter(p -> p.isLiving(moveInDate)
                            && p.getAgeInYears(moveInDate) < 16)
                    .collect(Collectors.toList());
            for (Person stepchild : stepchildren) {
                Household currentHousehold = stepchild.getHousehold(moveInDate);
                if (currentHousehold != null) {
                    stepchild = endPersonResidence(currentHousehold, stepchild, moveInDate);
                }
                movedChildren.add(addPersonToHousehold(stepchild, stepParentsHousehold, moveInDate, false));
            }
        }
        return movedChildren;
    }

    /**
     * Creates a household for the given person, making him or her the head.
     *
     * @param head the person who will be the head
     * @param asOfDate the date from which the person will live in the household
     * @param includeHomelessFamilyMembers if true, homeless
     * @return
     */
    public Household createHouseholdForHead(@NonNull Person head,
                                            @NonNull LocalDate asOfDate,
                                            boolean includeHomelessFamilyMembers) {
        Household household = new Household();
        household = save(household);
        addPersonToHousehold(head, household, asOfDate, true);

        if (!includeHomelessFamilyMembers) {
            return household;
        } else {
            return addHomelessFamilyMembersToHousehold(head, household, asOfDate, true);
        }

    }

    public Household addHomelessFamilyMembersToHousehold(@NonNull Person person,
                                                         @NonNull Household household,
                                                         @NonNull LocalDate onDate,
                                                         boolean includeSiblings) {
        Person spouse = person.getSpouse(onDate);
        if (spouse != null) {
            addPersonToHousehold(spouse, household, onDate, false);
        }
        for (Person child : person.getChildren()) {
            // We want to add all living children who do not have families of their own
            if (child.isLiving(onDate) && child.getFamilies().isEmpty()) {
                addPersonToHousehold(child, household, onDate, false);
            }
        }
        if (spouse != null && spouse.getFamilies().size() > 1) {
            for (Person child : spouse.getChildren()) {
                if (child.isLiving(onDate) && child.getFamilies().isEmpty()) {
                    addPersonToHousehold(child, household, onDate, false);
                }
            }
        }

        if (includeSiblings && person.getFamily() != null) {
            List<Person> siblings = person.getSiblings();
            for (Person sibling : siblings) {
                if (sibling.isLiving(onDate) && sibling.getFamilies().isEmpty()
                        && sibling.getResidence(onDate) == null) {
                    addPersonToHousehold(sibling, household, onDate, false);
                    addHomelessFamilyMembersToHousehold(sibling, household, onDate, false);
                }
            }
        }

        return household;
    }

}
