package com.meryt.demographics.service;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.repository.DwellingPlaceRepository;

@Service
public class DwellingPlaceService {

    private final DwellingPlaceRepository dwellingPlaceRepository;

    public DwellingPlaceService(@Autowired DwellingPlaceRepository dwellingPlaceRepository) {
        this.dwellingPlaceRepository = dwellingPlaceRepository;
    }

    public DwellingPlace save(@NonNull DwellingPlace dwellingPlace) {
        return dwellingPlaceRepository.save(dwellingPlace);
    }

}
