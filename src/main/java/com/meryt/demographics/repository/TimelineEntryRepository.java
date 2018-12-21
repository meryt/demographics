package com.meryt.demographics.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.timeline.TimelineEntry;

@Repository
public interface TimelineEntryRepository  extends CrudRepository<TimelineEntry, Long> {

    List<TimelineEntry> findAllByOrderByFromDate();

}
