package com.meryt.demographics.service;

import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.repository.TitleRepository;

@Service
public class TitleService {

    private final TitleRepository titleRepository;

    public TitleService(@Autowired @NonNull TitleRepository titleRepository) {
        this.titleRepository = titleRepository;
    }

    @Nullable
    public Title load(long titleId) {
        return titleRepository.findById(titleId).orElse(null);
    }

    @NonNull
    public Title save(@NonNull Title title) {
        return titleRepository.save(title);
    }

    @NonNull
    public Iterable<Title> findAll() {
        return titleRepository.findAll();
    }
}
