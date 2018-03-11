package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.Occupation;

@Repository
public interface OccupationRepository extends CrudRepository<Occupation, Long> {
}
