package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;

import com.meryt.demographics.domain.place.DwellingPlace;

public interface DwellingPlaceRepository extends CrudRepository<DwellingPlace, Long> {
}
