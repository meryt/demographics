package com.meryt.demographics.service;

import java.util.List;
import javax.annotation.Nullable;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;
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

    /**
     * Finds a place by ID or returns null if none found
     */
    @Nullable
    public DwellingPlace load(long placeId) {
        return dwellingPlaceRepository.findById(placeId).orElse(null);
    }

    public List<DwellingPlace> loadByType(DwellingPlaceType type) {
        return dwellingPlaceRepository.findByType(type);
    }

}
