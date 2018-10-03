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

        public static String receivedMarriageSettlementMessage(double amount) {
            return String.format("Received marriage settlement of %.2f", amount);
        }

        public static String providedMarriageSettlementMessage(@NonNull Person daughter, double amount) {
            return String.format("Provided marriage settlement of %.2f to %d %s", amount, daughter.getId(),
                    daughter.getName());
        }

        public static String startingCapitalMessage() {
            return "Starting capital";
        }

        public static String builtNewDwellingPlaceMessage(@NonNull DwellingPlace property) {
            return String.format("Built new dwelling %s %d%s for %.2f",
                    property.getType().getFriendlyName(),
                    property.getId(),
                    property.getName() == null ? "" : property.getName(),
                    property.getValue());
        }

        public static String purchasedPropertyMessage(@NonNull DwellingPlace property,
                                                      @Nullable Person seller,
                                                      double amount) {
            return String.format("Purchased %s %d %s from %s for %.2f",
                    property.getType().getFriendlyName(),
                    property.getId(),
                    property.getName() == null ? "" : property.getName(),
                    seller == null ? "unidentified person" : seller.getId() + " " + seller.getName(),
                    amount);
        }

        public static String soldPropertyMessage(@NonNull DwellingPlace property, @NonNull Person buyer, double amount) {
            return String.format("Sold %s %d%s to %d %s for %.2f", property.getType().getFriendlyName(), property.getId(),
                    property.getName() == null ? "" : property.getName(),
                    buyer.getId(), buyer.getName(), amount);
        }

        public static String inheritedCapitalMessage(@NonNull Person deceased, @Nullable Relationship relationship) {
            return String.format("Inherited from %d %s, %s", deceased.getId(), deceased.getName(),
                    relationship == null ? "no relation" : relationship.getName());
        }

        public static String livingExpensesMessage(double amount) {
            return String.format("Expenses of %.2f", amount);
        }

        public static String rentsMessage(@NonNull DwellingPlace place, double amount) {
            return String.format("Income of %.2f from %d %s", amount, place.getId(), place.getFriendlyName());
        }

        public static String receivedDwellingRentMessage(double amount) {
            return String.format("Received rent of %.2f", amount);
        }

        public static String paidDwellingRentMessage(double amount) {
            return String.format("Paid rent of %.2f", amount);
        }

        public static String wagesMessage(double amount) {
            return String.format("Wages of %.2f", amount);
        }

        public static String interestMessage(double amount) {
            return String.format("Interest on investments of %.2f", amount);
        }

        public static String loweredSocialClassMessage() {
            return "Bankruptcy";
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
