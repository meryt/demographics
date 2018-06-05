package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.place.DwellingPlaceOwnerPK;
import com.meryt.demographics.domain.place.DwellingPlaceOwnerPeriod;

@Repository
public interface DwellingPlaceOwnerRepository extends CrudRepository<DwellingPlaceOwnerPeriod, DwellingPlaceOwnerPK> {

}
