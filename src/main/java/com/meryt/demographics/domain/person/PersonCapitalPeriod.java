package com.meryt.demographics.domain.person;

import java.time.LocalDate;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import com.meryt.demographics.domain.family.Relationship;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.time.DateRange;

@Entity
@IdClass(PersonCapitalPK.class)
@Table(name = "person_capital")
@Getter
@Setter
public class PersonCapitalPeriod implements DateRange {

    public static class Reason {

        public static String receivedMarriageSettlementMessage() {
            return "Received marriage settlement";
        }

        public static String providedMarriageSettlementMessage(@NonNull Person daughter) {
            return String.format("Provided marriage settlement to %d %s", daughter.getId(), daughter.getName());
        }

        public static String startingCapitalMessage() {
            return "Starting capital";
        }

        public static String builtNewDwellingPlaceMessage(@NonNull DwellingPlace property) {
            return String.format("Built new dwelling %s %d%s", property.getType().getFriendlyName(), property.getId(),
                    property.getName() == null ? "" : property.getName());
        }

        public static String purchasedPropertyMessage(@NonNull DwellingPlace property, @Nullable Person seller) {
            return String.format("Purchased %s %d%s from %s", property.getType().getFriendlyName(), property.getId(),
                    property.getName() == null ? "" : property.getName(),
                    seller == null ? "unidentified person" : seller.getId() + " " + seller.getName());
        }

        public static String soldPropertyMessage(@NonNull DwellingPlace property, @NonNull Person buyer) {
            return String.format("Sold %s %d%s to %d %s", property.getType().getFriendlyName(), property.getId(),
                    property.getName() == null ? "" : property.getName(),
                    buyer.getId(), buyer.getName());
        }

        public static String inheritedCapitalMessage(@NonNull Person deceased, @Nullable Relationship relationship) {
            return String.format("Inherited from %d %s, %s", deceased.getId(), deceased.getName(),
                    relationship == null ? "no relation" : relationship.getName());
        }

        public static String rentsWagesInterestMessage() {
            return "Rents, wages, and/or interest";
        }
    }

    @Id
    @Column(name = "person_id", updatable = false, insertable = false)
    private long personId;

    @JsonIgnore
    @ManyToOne
    @MapsId("person_id")
    @PrimaryKeyJoinColumn(name = "person_id", referencedColumnName = "id")
    private Person person;

    @Id
    private LocalDate fromDate;

    private LocalDate toDate;

    private double capital;

    private String reason;
}
