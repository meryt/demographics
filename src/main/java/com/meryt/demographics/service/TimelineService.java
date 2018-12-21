package com.meryt.demographics.service;

import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.Lists;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.timeline.TimelineEntry;
import com.meryt.demographics.repository.TimelineEntryRepository;

@Service
public class TimelineService {

    private final TimelineEntryRepository timelineEntryRepository;

    public TimelineService(@NonNull @Autowired TimelineEntryRepository timelineEntryRepository) {
        this.timelineEntryRepository = timelineEntryRepository;
    }

    public TimelineEntry save(TimelineEntry entry) {
        return timelineEntryRepository.save(entry);
    }

    @Nullable
    public TimelineEntry load(long entryId) {
        return timelineEntryRepository.findById(entryId).orElse(null);
    }

    public List<TimelineEntry> loadAll() {
        return Lists.newArrayList(timelineEntryRepository.findAllByOrderByFromDate());
    }
}
