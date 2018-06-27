package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.response.DwellingPlacePointer;

@Getter
@Setter
@Entity
@Table(name = "dwelling_places")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="dwelling_place_type")
public abstract class DwellingPlace {

    public static final double ACRES_PER_SQUARE_MILE = 640;

    @Id
    @SequenceGenerator(name="dwelling_places_id_seq", sequenceName="dwelling_places_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="dwelling_places_id_seq")
    private long id;

    private String name;

    @ManyToOne
    private DwellingPlace parent;

    @OneToMany(mappedBy = "parent", cascade = { CascadeType.ALL })
    private final Set<DwellingPlace> dwellingPlaces = new HashSet<>();

    /**
     * A list of all the households who have ever been located directly in this dwelling place, over time
     */
    @OneToMany(mappedBy = "dwellingPlace", cascade = { CascadeType.MERGE })
    private List<HouseholdLocationPeriod> householdPeriods = new ArrayList<>();

    /**
     * A list of all the owners of this dwelling place, over time
     */
    @OneToMany(mappedBy = "dwellingPlace", cascade = { CascadeType.MERGE })
    private List<DwellingPlaceOwnerPeriod> ownerPeriods = new ArrayList<>();

    private Double acres;

    private Double value;

    @Enumerated(EnumType.STRING)
    @Column(name = "dwelling_place_type", updatable = false, insertable = false)
    @Setter(AccessLevel.PROTECTED)
    private DwellingPlaceType type;

    private boolean entailed;

    public Double getSquareMiles() {
        if (acres == null) {
            return null;
        }

        return acres / ACRES_PER_SQUARE_MILE;
    }

    public String getFriendlyName() {
        return name == null ? getType().getFriendlyName() + " " + id : name;
    }

    public boolean isHouse() {
        return getType() == DwellingPlaceType.DWELLING;
    }

    public boolean isEstate() {
        return getType() == DwellingPlaceType.ESTATE;
    }

    public boolean isFarm() {
        return getType() == DwellingPlaceType.FARM;
    }

    public boolean isEstateOrFarm() {
        return isEstate() || isFarm();
    }

    public double getNullSafeValue() {
        return value == null ? 0.0 : value;
    }

    /**
     * Gets the population of all households directly in this dwelling place as well as the population of all dwelling
     * places contained in this dwelling place
     */
    public long getPopulation(@NonNull LocalDate onDate) {
        return dwellingPlaces.stream().mapToLong(d -> d.getPopulation(onDate)).sum() + getDirectPopulation(onDate);
    }

    /**
     * Gets only the population of households that are living directly in this dwelling place on this date (is not
     * recursive, unlike getPopulation)
     */
    public long getDirectPopulation(@NonNull LocalDate onDate) {
        return householdPeriods.stream().mapToLong(h -> h.getHousehold().getPopulation(onDate)).sum();
    }

    /**
     * Gets all the households in this dwelling place, both direct members of this place as well as of places this
     * place contains.
     */
    public List<Household> getAllHouseholds(@NonNull LocalDate onDate) {
        List<Household> directHouseholds = getHouseholds(onDate);
        directHouseholds.addAll(getIndirectHouseholds(onDate));
        return directHouseholds;
    }

    /**
     * Recursively get households for child places
     */
    private List<Household> getIndirectHouseholds(@NonNull LocalDate onDate) {
        return getDwellingPlaces().stream()
                .map(place -> place.getAllHouseholds(onDate))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<Person> getAllResidents(@NonNull LocalDate onDate) {
        return getAllHouseholds(onDate).stream()
                .flatMap(h -> h.getInhabitants(onDate).stream())
                .collect(Collectors.toList());
    }

    /**
     * Recursively get the friendly location name of this dwelling place by taking its name (if any) and appending the
     * result of its parent.
     * @return a string, or null if neither the place nor any of its parents has a name
     */
    @Nullable
    public String getLocationString() {
        String name = getName();
        String location = getParent() == null ? null : getParent().getLocationString();
        if (name == null) {
            return location;
        } else if (location == null) {
            return name;
        } else {
            return name + ", " + location;
        }
    }

    /**
     * Get all households where the head is at least the given social class
     *
     * @param onDate the date to check for household residency
     * @param minSocialClass the minimum social class to qualify as "leading"
     * @param recursive if true, recurses into dwelling places inside of this dwelling place; otherwise shows only
     *                  households directly dwelling in this place
     * @return a list of households, possibly empty
     */
    @NonNull
    public List<Household> getLeadingHouseholds(@NonNull LocalDate onDate,
                                                @NonNull SocialClass minSocialClass,
                                                boolean recursive) {
        List<Household> households = recursive ? getAllHouseholds(onDate) : getHouseholds(onDate);
        return households.stream()
                .filter(h -> {
                    Person head = h.getHead(onDate);
                    return (head != null && head.getSocialClass().getRank() >= minSocialClass.getRank());
                })
                .collect(Collectors.toList());
    }

    public Set<DwellingPlace> getDwellingPlaces() {
        return Collections.unmodifiableSet(dwellingPlaces);
    }

    /**
     * Adds a new child dwelling place to the list of members
     * @param newMember a DwellingPlace of a type appropriate to belong to this place
     * @throws IllegalArgumentException if the given dwelling place is not a valid child of the this one
     */
    public final void addDwellingPlace(@NonNull DwellingPlace newMember) {
        if (!getType().canContain(newMember.getType())) {
            throw new IllegalArgumentException(String.format("A %s cannot contain a %s", getType().name(),
                    newMember.getType().name()));
        }
        dwellingPlaces.add(newMember);
        newMember.setParent(this);
    }

    /**
     * Removes a child dwelling place from the set of members. Also sets its parent to null if its parent was
     * previously this.
     *
     * @param member the child to remove
     */
    public void removeDwellingPlace(@NonNull DwellingPlace member) {
        dwellingPlaces.remove(member);
        if (member.getParent().equals(this)) {
            member.setParent(null);
        }
    }

    private Set<DwellingPlace> getRecursiveDwellingPlaces() {
        return Stream.concat(
                Stream.of(this),
                getDwellingPlaces().stream()
                    .map(DwellingPlace::getRecursiveDwellingPlaces)
                    .flatMap(Collection::stream)
                ).collect(Collectors.toSet());
    }

    public Set<DwellingPlace> getRecursiveDwellingPlaces(@NonNull DwellingPlaceType ofType) {
        return getRecursiveDwellingPlaces().stream()
                .filter(dp -> dp.getType() == ofType)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all households that are directly a member of this dwelling place as of the date
     */
    public List<Household> getHouseholds(@NonNull LocalDate onDate) {
        return getHouseholdPeriods().stream()
                .filter(hp -> hp.contains(onDate))
                .map(HouseholdLocationPeriod::getHousehold)
                .collect(Collectors.toList());
    }

    /**
     * Get the person who owns the property on this date. (May be empty.)
     * @param onDate the date to find an owner; there may be 0 or 1 persons owning the property on this date.
     * @return the owning person or persons, or empty list if no one owns it at this time
     */
    @NotNull
    public List<Person> getOwners(@NonNull LocalDate onDate) {
        return getOwnerPeriods().stream()
                .filter(p -> p.contains(onDate))
                .map(DwellingPlaceOwnerPeriod::getOwner)
                .collect(Collectors.toList());
    }

    /**
     * Makes a person the owner of this dwelling place, starting from the given date, and optionally ending on the
     * given date
     * @param person the owner
     * @param fromDate the start date
     * @param toDate the end date (may be null)
     */
    public void addOwner(@NonNull Person person, @NonNull LocalDate fromDate, LocalDate toDate) {
        for (DwellingPlaceOwnerPeriod period : getOwnerPeriods()) {
            if (period.contains(fromDate)) {
                period.setToDate(fromDate);
            }
        }

        DwellingPlaceOwnerPeriod newPeriod = new DwellingPlaceOwnerPeriod();
        newPeriod.setDwellingPlaceId(getId());
        newPeriod.setDwellingPlace(this);
        newPeriod.setPersonId(person.getId());
        newPeriod.setOwner(person);
        newPeriod.setFromDate(fromDate);
        newPeriod.setToDate(toDate);
        person.getOwnedDwellingPlaces().add(newPeriod);
        getOwnerPeriods().add(newPeriod);
    }

    public void mergeHouseholdPeriod(@NonNull HouseholdLocationPeriod period) {
        getHouseholdPeriods().stream()
                .filter(p -> p.getFromDate().equals(period.getFromDate()) && p.getHouseholdId() == period.getHouseholdId())
                .findFirst().ifPresent(oldPeriod -> householdPeriods.remove(oldPeriod));
        householdPeriods.add(period);
    }

    public Map<Occupation, List<Person>> getPeopleWithOccupations(@NonNull LocalDate onDate) {
        return getAllHouseholds(onDate).stream()
                .map(h -> h.getInhabitants(onDate))
                .flatMap(Collection::stream)
                .filter(p -> p.getOccupation(onDate) != null)
                .collect(Collectors.groupingBy(p -> p.getOccupation(onDate)));
    }

}
