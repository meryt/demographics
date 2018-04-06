package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

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
    @OneToMany(mappedBy = "dwellingPlace", cascade = { CascadeType.ALL })
    private List<HouseholdLocationPeriod> householdPeriods = new ArrayList<>();

    private Double acres;

    public Double getSquareMiles() {
        if (acres == null) {
            return null;
        }

        return acres / ACRES_PER_SQUARE_MILE;
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

    public Set<DwellingPlace> getDwellingPlaces() {
        return Collections.unmodifiableSet(dwellingPlaces);
    }

    /**
     * Adds a new child dwelling place to the list of members
     * @param newMember a DwellingPlace of a type appropriate to belong to this place
     * @throws IllegalArgumentException if the given dwelling place is not a valid child of the this one
     */
    public void addDwellingPlace(@NonNull DwellingPlace newMember) {
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

    /**
     * Gets all households that are in this town as of the date
     */
    public List<Household> getHouseholds(@NonNull LocalDate onDate) {
        return getHouseholdPeriods().stream()
                .filter(hp -> hp.contains(onDate))
                .map(HouseholdLocationPeriod::getHousehold)
                .collect(Collectors.toList());
    }

}
