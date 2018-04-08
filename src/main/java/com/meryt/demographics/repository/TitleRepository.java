package com.meryt.demographics.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.title.Title;

@Repository
public interface TitleRepository extends PagingAndSortingRepository<Title, Long> {
}
