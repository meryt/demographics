package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dwelling_places")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="dwelling_place_type")
abstract public class DwellingPlace {

    @Id
    @SequenceGenerator(name="dwelling_places_id_seq", sequenceName="dwelling_places_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="dwelling_places_id_seq")
    private int id;

    private String name;

    @JsonIgnore
    @ManyToOne
    private DwellingPlace parent;

    @OneToMany(mappedBy = "parent", cascade = { CascadeType.ALL })
    private final Set<DwellingPlace> dwellingPlaces = new HashSet<>();

    private Double acres;

    public long getPopulation(@NonNull LocalDate onDate) {
        // FIXME should also return the population of the households contained by this dwelling place
        return dwellingPlaces.stream().mapToLong(d -> d.getPopulation(onDate)).sum();
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

    public void addHousehold(@NonNull Household household) {

    }
}
