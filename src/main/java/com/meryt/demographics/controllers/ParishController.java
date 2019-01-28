package com.meryt.demographics.controllers;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.generator.ParishGenerator;
import com.meryt.demographics.request.ParishParameters;
import com.meryt.demographics.response.DwellingPlaceDetailResponse;
import com.meryt.demographics.service.ConfigurationService;
import com.meryt.demographics.service.DwellingPlaceService;

@RestController
public class ParishController {

    private final DwellingPlaceService dwellingPlaceService;
    private final ConfigurationService configurationService;
    private final ParishGenerator parishGenerator;

    public ParishController(@Autowired DwellingPlaceService dwellingPlaceService,
                            @Autowired ConfigurationService configurationService,
                            @Autowired @NonNull ParishGenerator parishGenerator) {
        this.dwellingPlaceService = dwellingPlaceService;
        this.configurationService = configurationService;
        this.parishGenerator = parishGenerator;
    }

    @RequestMapping("/api/parishes/random")
    public DwellingPlaceDetailResponse randomParish(@RequestBody ParishParameters parishParameters) {
        Parish parish = parishGenerator.generateParish(parishParameters);
        configurationService.setCurrentDate(parishParameters.getFamilyParameters().getReferenceDate());
        return new DwellingPlaceDetailResponse(dwellingPlaceService.save(parish),
                parishParameters.getFamilyParameters().getReferenceDate(), null);
    }
}
