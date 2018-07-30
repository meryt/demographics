package com.meryt.demographics.repository;

import java.time.LocalDate;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.title.Title;

@Repository
public interface TitleRepository extends PagingAndSortingRepository<Title, Long> {

    List<Title> findAllByOrderByNameAsc();

    List<Title> findAllByNextAbeyanceCheckDateIsLessThanEqualAndExtinctIsFalse(@NonNull LocalDate date);
}
