package com.meryt.demographics.response;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.fertility.Fertility;
import com.meryt.demographics.domain.person.fertility.Maternity;

@Getter
public class FertilityResponse {

    private final long personId;
    private final double fertilityFactor;
    
    // Maternity-specific fields
    private final LocalDate conceptionDate;
    private final LocalDate miscarriageDate;
    private final LocalDate dueDate;
    private final LocalDate lastCycleDate;
    private final LocalDate lastCheckDate;
    private final Boolean carryingIdenticalTwins;
    private final Boolean carryingFraternalTwins;
    private final LocalDate breastfeedingTill;
    private final Boolean hadTwins;
    private final Integer numBirths;
    private final Integer numMiscarriages;
    private final LocalDate lastBirthDate;
    private final Double frequencyFactor;
    private final Double withdrawalFactor;
    private final Boolean havingRelations;
    private final Integer cycleLength;
    
    // Father reference (only for Maternity)
    private final PersonReference father;
    private final Long fatherId;

    public FertilityResponse(@NonNull Fertility fertility) {
        this.personId = fertility.getPersonId();
        this.fertilityFactor = fertility.getFertilityFactor();
        
        if (fertility instanceof Maternity) {
            Maternity maternity = (Maternity) fertility;
            this.conceptionDate = maternity.getConceptionDate();
            this.miscarriageDate = maternity.getMiscarriageDate();
            this.dueDate = maternity.getDueDate();
            this.lastCycleDate = maternity.getLastCycleDate();
            this.lastCheckDate = maternity.getLastCheckDate();
            this.carryingIdenticalTwins = maternity.isCarryingIdenticalTwins();
            this.carryingFraternalTwins = maternity.isCarryingFraternalTwins();
            this.breastfeedingTill = maternity.getBreastfeedingTill();
            this.hadTwins = maternity.isHadTwins();
            this.numBirths = maternity.getNumBirths();
            this.numMiscarriages = maternity.getNumMiscarriages();
            this.lastBirthDate = maternity.getLastBirthDate();
            this.frequencyFactor = maternity.getFrequencyFactor();
            this.withdrawalFactor = maternity.getWithdrawalFactor();
            this.havingRelations = maternity.isHavingRelations();
            this.cycleLength = maternity.getCycleLength();
            this.father = maternity.getFather() != null ? new PersonReference(maternity.getFather()) : null;
            this.fatherId = maternity.getFatherId();
        } else {
            // Paternity - these fields are null
            this.conceptionDate = null;
            this.miscarriageDate = null;
            this.dueDate = null;
            this.lastCycleDate = null;
            this.lastCheckDate = null;
            this.carryingIdenticalTwins = null;
            this.carryingFraternalTwins = null;
            this.breastfeedingTill = null;
            this.hadTwins = null;
            this.numBirths = null;
            this.numMiscarriages = null;
            this.lastBirthDate = null;
            this.frequencyFactor = null;
            this.withdrawalFactor = null;
            this.havingRelations = null;
            this.cycleLength = null;
            this.father = null;
            this.fatherId = null;
        }
    }
}