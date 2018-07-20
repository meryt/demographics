package com.meryt.demographics.repository;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.Occupation;

@Repository
public interface OccupationRepository extends PagingAndSortingRepository<Occupation, Long> {

    List<Occupation> findByAllowMaleIsTrue();

    List<Occupation> findByAllowFemaleIsTrue();

    List<Occupation> findByIsDomesticServantIsTrue();

    List<Occupation> findByIsFarmLaborerIsTrue();
}
