package com.meryt.demographics.domain.place;

import java.time.LocalDate;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.time.DateRange;

@Entity
@IdClass(DwellingPlaceOwnerPK.class)
@Table(name = "dwelling_place_owners")
@Getter
@Setter
public class DwellingPlaceOwnerPeriod implements DateRange {

    public enum ReasonToPurchase {
        MARRIAGE("Purchased upon marriage"),
        EVICTION("Purchased upon eviction"),
        MOVE_TO_PARISH("Moved to parish"),
        CREATED_ESTATE("Created estate");

        @Getter
        private final String message;

        ReasonToPurchase(String message) {
            this.message = message;
        }
    }

    public static class Reason {
        public static String inheritedReason(@NonNull Person deceased, @Nullable Relationship relationship) {
            return String.format("Inherited from %d %s, %s", deceased.getId(), deceased.getName(),
                    relationship == null ? "no relation" : relationship.getName());
        }

        public static String builtNewHouseUponInheritanceMessage(@NonNull Person deceased, @Nullable Relationship relationship) {
            return String.format("Built new house after inheriting money from %d %s, %s", deceased.getId(), deceased.getName(),
                    relationship == null ? "no relation" : relationship.getName());
        }

        public static String purchasedHouseUponInheritanceMessage(@NonNull Person deceased, @Nullable Relationship relationship) {
            return String.format("Purchased after inheriting money from %d %s, %s", deceased.getId(), deceased.getName(),
                    relationship == null ? "no relation" : relationship.getName());
        }

        public static String purchasedHouseUponEmploymentMessage() {
            return "Purchased upon employment";
        }

        public static String inheritedAsTitleHolderMessage(@NonNull Title title) {
            return String.format("Inherited as new %d %s", title.getId(), title.getName());
        }

        public static String tookUnownedHouseMessage() {
            return "Took ownership of unowned place";
        }

        public static String foundedFarmForRuralHouseMessage() {
            return "Founded farm for rural house";
        }
    }

    @Id
    @Column(name = "dwelling_place_id", updatable = false, insertable = false)
    private long dwellingPlaceId;

    @Id
    @Column(name = "person_id", updatable = false, insertable = false)
    private long personId;

    @Id
    private LocalDate fromDate;

    private LocalDate toDate;

    private String reason;

    @JsonIgnore
    @ManyToOne
    @MapsId("dwelling_place_id")
    @JoinColumn(name = "dwelling_place_id", referencedColumnName = "id")
    private DwellingPlace dwellingPlace;

    @JsonIgnore
    @ManyToOne
    @MapsId("person_id")
    @JoinColumn(name = "person_id", referencedColumnName = "id")
    private Person owner;

}
