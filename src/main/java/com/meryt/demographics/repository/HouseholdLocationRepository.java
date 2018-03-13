package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.place.HouseholdLocationPK;
import com.meryt.demographics.domain.place.HouseholdLocationPeriod;

@Repository
public interface HouseholdLocationRepository extends CrudRepository<HouseholdLocationPeriod, HouseholdLocationPK> {
}
