package com.meryt.demographics.service;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.place.Region;
import com.meryt.demographics.repository.TownTemplateRepository;

@Service
public class TownTemplateService {

    private final TownTemplateRepository townTemplateRepository;

    public TownTemplateService(@NonNull @Autowired TownTemplateRepository townTemplateRepository) {
        this.townTemplateRepository = townTemplateRepository;
    }

    public String getUnusedMapId(@NonNull Region region) {
        return townTemplateRepository.getUnusedMapId(region.getName().toLowerCase().equals("england"));
    }
}
