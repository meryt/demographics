package com.meryt.demographics.service;

import java.util.List;
import com.google.common.collect.Lists;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.story.Storyline;
import com.meryt.demographics.repository.StorylineRepository;

@Service
public class StorylineService {

    private final StorylineRepository storylineRepository;

    public StorylineService(@NonNull @Autowired StorylineRepository storylineRepository) {
        this.storylineRepository = storylineRepository;
    }

    public List<Storyline> loadAll() {
        return Lists.newArrayList(storylineRepository.findAll());
    }

}
