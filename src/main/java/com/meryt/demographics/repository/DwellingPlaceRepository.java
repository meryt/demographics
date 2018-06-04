package com.meryt.demographics.repository;

import java.util.List;
import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;

public interface DwellingPlaceRepository extends CrudRepository<DwellingPlace, Long> {

    List<DwellingPlace> findByType(@NonNull DwellingPlaceType type);
}
