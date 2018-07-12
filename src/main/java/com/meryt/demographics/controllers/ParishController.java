package com.meryt.demographics.controllers;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.place.Parish;
import com.meryt.demographics.generator.ParishGenerator;
import com.meryt.demographics.request.ParishParameters;
import com.meryt.demographics.response.DwellingPlaceResponse;
import com.meryt.demographics.service.CheckDateService;
import com.meryt.demographics.service.DwellingPlaceService;

@RestController
public class ParishController {

    private final DwellingPlaceService dwellingPlaceService;
    private final CheckDateService checkDateService;
    private final ParishGenerator parishGenerator;

    public ParishController(@Autowired DwellingPlaceService dwellingPlaceService,
                            @Autowired CheckDateService checkDateService,
                            @Autowired @NonNull ParishGenerator parishGenerator) {
        this.dwellingPlaceService = dwellingPlaceService;
        this.checkDateService =  checkDateService;
        this.parishGenerator = parishGenerator;
    }

    @RequestMapping("/api/parishes/random")
    public DwellingPlaceResponse randomParish(@RequestBody ParishParameters parishParameters) {
        Parish parish = parishGenerator.generateParish(parishParameters);
        checkDateService.setCurrentDate(parishParameters.getFamilyParameters().getReferenceDate());
        if (parishParameters.isPersist()) {
            return new DwellingPlaceResponse(dwellingPlaceService.save(parish),
                    parishParameters.getFamilyParameters().getReferenceDate());
        } else {
            return new DwellingPlaceResponse(parish, parishParameters.getFamilyParameters().getReferenceDate());
        }
    }
}
