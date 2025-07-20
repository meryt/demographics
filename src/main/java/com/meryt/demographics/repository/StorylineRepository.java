package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.story.Storyline;

@Repository
public interface StorylineRepository  extends PagingAndSortingRepository<Storyline, Long>, CrudRepository<Storyline, Long> {

}
