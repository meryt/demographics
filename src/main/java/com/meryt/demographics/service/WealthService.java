package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.Dwelling;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.generator.WealthGenerator;
import com.meryt.demographics.generator.random.BetweenDie;

@Slf4j
@Service
public class WealthService {

    private enum ExpenseType {
        LIVING_EXPENSES,
        RENT
    }

    private final DwellingPlaceService dwellingPlaceService;
    private final HouseholdDwellingPlaceService householdDwellingPlaceService;
    private final PersonService personService;

    public WealthService(@NonNull @Autowired DwellingPlaceService dwellingPlaceService,
                         @NonNull @Autowired PersonService personService,
                         @NonNull @Autowired HouseholdDwellingPlaceService householdDwellingPlaceService) {
        this.dwellingPlaceService = dwellingPlaceService;
        this.householdDwellingPlaceService = householdDwellingPlaceService;
        this.personService = personService;
    }

    void distributeCapital(@NonNull LocalDate onDate, double goodYearFactor) {
        List<DwellingPlace> estatesAndFarms = dwellingPlaceService.loadByType(DwellingPlaceType.ESTATE);
        estatesAndFarms.addAll(dwellingPlaceService.loadByType(DwellingPlaceType.FARM));

        for (DwellingPlace estateOrFarm : estatesAndFarms) {
            distributeEstateRentsAndFarmIncome(estateOrFarm, onDate, goodYearFactor);
        }

        for (DwellingPlace parish : dwellingPlaceService.loadByType(DwellingPlaceType.PARISH)) {
            List<Person> peopleWithWages = parish.getAllResidents(onDate).stream()
                    .filter(p -> p.getOccupation(onDate) != null ||
                            (p.getSocialClassRank() <= SocialClass.YEOMAN_OR_MERCHANT.getRank()
                                    && p.getAgeInYears(onDate) >= 14))
                    .collect(Collectors.toList());

            for (Person person : peopleWithWages) {
                distributeWages(person.getOccupation(onDate), person, onDate, goodYearFactor);
            }

            List<Person> gentry = parish.getAllResidents(onDate).stream()
                    .filter(p -> p.getSocialClass().getRank() >= SocialClass.YEOMAN_OR_MERCHANT.getRank()
                        && p.getOccupation(onDate) == null)
                    .collect(Collectors.toList());
            for (Person gentleman : gentry) {
                distributeInterestOnCapital(gentleman, onDate);
            }

            for (DwellingPlace dwelling : parish.getRecursiveDwellingPlaces(DwellingPlaceType.DWELLING)) {
                distributeDwellingRents((Parish) parish, (Dwelling) dwelling, onDate);
            }

            for (Household household : parish.getRecursiveHouseholds(onDate)) {
                payHouseholdExpenses(household, onDate);
            }
        }
    }

    private void payHouseholdExpenses(@NonNull Household household, @NonNull LocalDate onDate) {
        SocialClass householdClass = household.getMaxSocialClass(onDate);
        if (householdClass == null) {
            return;
        }
        double averageExpenses = WealthGenerator.getYearlyCostOfLivingPerHousehold(householdClass);
        // expenses are +/- 20% of the expected value
        double actualExpenses = averageExpenses +
                ((new BetweenDie()).roll(-2000, 2000) * 0.0001) * averageExpenses;

        if (log.isDebugEnabled()) {
            log.debug(String.format("%s of rank %s has %.2f in expenses and %.2f in capital",
                    household.getFriendlyName(onDate), householdClass.getFriendlyName(), actualExpenses,
                    household.getCapital(onDate)));
        }

        householdPayExpense(household, actualExpenses, onDate, ExpenseType.LIVING_EXPENSES);

        // If the household didn't have enough to pay its bills this year, check to see if they are so
        // deeply in debt that they can no longer maintain their social class.
        if (householdClass.getRank() > SocialClass.PAUPER.getRank()) {
            Double newCapital = household.getCapital(onDate);
            double actualNewCapital = newCapital == null ? 0.0 : newCapital;
            if (actualNewCapital < 0 && (actualNewCapital * -1) > (3 * averageExpenses)) {
                // The household is now in debt for more than three times the average expenses. Reduce the
                // social class by 1 rank.
                for (Person member : household.getInhabitants(onDate)) {
                    if (member.getSocialClass() == householdClass) {
                        member.setSocialClass(householdClass.minusOne());
                        log.info(String.format(
                                "The household of %d %s is so deep in debt that %s social class sank to %s",
                                member.getId(), member.getName(), member.isMale() ? "his" : "her",
                                householdClass.minusOne().getFriendlyName()));
                        member.addCapital(-1 * member.getCapitalNullSafe(onDate), onDate, PersonCapitalPeriod.Reason.loweredSocialClassMessage());
                        personService.save(member);
                    }
                }
            }
        }
    }

    /**
     * For houses where the owner is not an inhabitant, the inhabitant households must pay rent to the owner. The
     * rent depends on the value of the house, and each household in the house pays the same share.
     */
    private void distributeDwellingRents(@NonNull Parish parish, @NonNull Dwelling dwelling, @NonNull LocalDate onDate) {
        List<Person> owners = dwelling.getOwners(onDate);
        if (owners.isEmpty()) {
            return;
        }
        List<Person> residents = dwelling.getAllResidents(onDate);
        if (residents.isEmpty()) {
            return;
        }
        for (Person owner : owners) {
            if (residents.contains(owner)) {
                return;
            }
        }
        double rent = dwelling.getNullSafeValue() / 30;
        List<Household> households = dwelling.getHouseholds(onDate);
        double rentPerHousehold = rent / households.size();
        for (Household household : households) {
            householdPayExpense(household, rentPerHousehold, onDate, ExpenseType.RENT);
            if (household.getCapital(onDate) < 0) {
                householdDwellingPlaceService.maybeMoveIndebtedHouseholdToEmptyHouse(parish, household, onDate);
            }
        }
        double rentPerOwner = rent / owners.size();
        for (Person owner : owners) {
            owner.addCapital(rentPerOwner, onDate, PersonCapitalPeriod.Reason.receivedDwellingRentMessage(rentPerOwner));
            personService.save(owner);
        }
    }

    private void householdPayExpense(@NonNull Household household,
                                     double expense,
                                     @NonNull LocalDate onDate,
                                     @NonNull ExpenseType expenseType) {
        List<Person> members = new ArrayList<>();
        Person householdHead = household.getHead(onDate);
        if (householdHead != null) {
            members.add(householdHead);
        }
        for (Person member : household.getInhabitants(onDate)) {
            if (!members.contains(member)) {
                members.add(member);
            }
        }

        if (members.isEmpty()) {
            return;
        }

        double actualExpenses = expense;
        // Loop over people starting with head of household. Each will pay as much as he can, in order of
        // decreasing age, until all expenses have been paid.
        for (Person member : members) {
            if (actualExpenses <= 0) {
                break;
            }
            double personCapital = member.getCapitalNullSafe(onDate);
            if (personCapital >= actualExpenses) {
                member.addCapital(-1 * actualExpenses, onDate, getReasonFromExpenseType(expenseType, actualExpenses));
                personService.save(member);
                actualExpenses = 0.0;
            } else if (personCapital > 0) {
                member.addCapital(-1 * personCapital, onDate, getReasonFromExpenseType(expenseType, personCapital));
                personService.save(member);
                actualExpenses -= personCapital;
            }
        }
        if (actualExpenses > 0) {
            // The head will go into debt if the expenses exceed the capital
            Person head = members.get(0);
            head.addCapital(-1 * actualExpenses, onDate, getReasonFromExpenseType(expenseType, actualExpenses));
            personService.save(head);
        }
    }

    private String getReasonFromExpenseType(@NonNull ExpenseType expenseType, double amount) {
        String reason;
        switch (expenseType) {
            case LIVING_EXPENSES:
                reason = PersonCapitalPeriod.Reason.livingExpensesMessage(amount);
                break;
            case RENT:
                reason = PersonCapitalPeriod.Reason.paidDwellingRentMessage(amount);
                break;
            default:
                log.warn("Unknown ExpenseType " + expenseType.name());
                reason = "Unknown";
        }
        return reason;
    }

    private void distributeEstateRentsAndFarmIncome(@NonNull DwellingPlace estate,
                                                    @NonNull LocalDate onDate,
                                                    double goodYearFactor) {
        List<Person> owners = estate.getOwners(onDate);
        if (owners.isEmpty()) {
            log.info(String.format("No rents were distributed for %d %s, as it has no owner", estate.getId(),
                    estate.getLocationString()));
            return;
        }
        // Estates have a rate of return of -1% to 7%. Farms have a rate of -2% to 5%.
        int minReturn = estate.getType() == DwellingPlaceType.FARM ? -2 : -1;
        int maxReturn = estate.getType() == DwellingPlaceType.FARM ? 5 : 7;

        double value = estate.getValue();
        double individualFactor = (new BetweenDie()).roll(minReturn * 100, maxReturn * 100) * 0.0001;
        double baseRent = value * individualFactor;
        double adjustedRent = adjustForGoodOrBadYear(baseRent, goodYearFactor);

        double individualRent = adjustedRent / owners.size();
        for (Person owner : owners) {
            owner.addCapital(individualRent, onDate, PersonCapitalPeriod.Reason.rentsMessage(estate, individualRent));
            log.debug(String.format("%d %s received %.2f from rents on %d %s", owner.getId(), owner.getName(),
                    individualRent, estate.getId(), estate.getLocationString()));
            personService.save(owner);
        }
    }

    private void distributeWages(@Nullable Occupation occupation,
                                 @NonNull Person person,
                                 @NonNull LocalDate onDate,
                                 double goodYearFactor) {
        Pair<Integer, Integer> range = WealthGenerator.getYearlyIncomeValueRange(person.getSocialClass());
        int value = new BetweenDie().roll(range.getFirst(), range.getSecond());
        double adjustedWage = adjustForGoodOrBadYear(value, goodYearFactor);
        person.addCapital(adjustedWage, onDate, PersonCapitalPeriod.Reason.wagesMessage(adjustedWage));
        log.debug(String.format("%d %s received %.2f from his wages as a %s", person.getId(), person.getName(),
                adjustedWage, occupation == null ? "laborer" : occupation.getName()));
        personService.save(person);
    }

    private void distributeInterestOnCapital(@NonNull Person person, @NonNull LocalDate onDate) {
        Double currentCapital = person.getCapital(onDate);
        if (currentCapital == null || currentCapital < 0.0) {
            return;
        }

        double rateOfReturn = new BetweenDie().roll(-200, 400) * 0.0001;
        double interest = currentCapital * rateOfReturn;
        person.addCapital(interest, onDate, PersonCapitalPeriod.Reason.interestMessage(interest));
        log.debug(String.format("%d %s received %.2f from interest on capital", person.getId(), person.getName(),
                interest));
        personService.save(person);
    }

    /**
     * Given a wage or a rent and a "good year" factor indicating how good or bad the year was for business, return
     * an adjusted wage or rent.
     *
     * If the wage or rent is negative but it is a good year (the good year factor is positive) the absolute value
     * of the wage or rent will be reduced, so the person takes a smaller hit.  If it's negative and it's a bad year,
     * the absolute value will be increased, making the hit even worse. The converse is true for a positive value.
     *
     * @param wageOrRent the base rage or rent received this year
     * @param goodYearFactor the factor by which the wage/rent is increased (or decreased if this value is negative) to
     *                       reflect how good or bad for business the year was.
     * @return the adjusted wage or rent
     */
    private double adjustForGoodOrBadYear(double wageOrRent, double goodYearFactor) {
        if (wageOrRent < 0 && goodYearFactor < 0) {
            return wageOrRent * (1.0 - goodYearFactor);
        } else if (wageOrRent < 0 && goodYearFactor >= 0) {
            return wageOrRent * (1.0 - goodYearFactor);
        } else if (wageOrRent >= 0 && goodYearFactor < 0) {
            return wageOrRent * (1.0 + goodYearFactor);
        } else if (wageOrRent >= 0 && goodYearFactor >= 0) {
            return wageOrRent * (1.0 + goodYearFactor);
        } else {
            return wageOrRent * (1.0 + goodYearFactor);
        }
    }

}
