package com.meryt.demographics.service;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.generator.FamilyGenerator;
import com.meryt.demographics.request.FamilyParameters;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FamilyService {

    private final FamilyGenerator familyGenerator;

    public FamilyService(@Autowired FamilyGenerator familyGenerator) {
        this.familyGenerator = familyGenerator;
    }

    public Family generateFamily(@NonNull FamilyParameters familyParameters) {
        return familyGenerator.generate(familyParameters);
    }

}
