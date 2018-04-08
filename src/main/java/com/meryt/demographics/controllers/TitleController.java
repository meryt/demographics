package com.meryt.demographics.controllers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.domain.title.Title;
import com.meryt.demographics.response.TitleResponse;
import com.meryt.demographics.service.TitleService;

@RestController
public class TitleController {

    private final TitleService titleService;

    public TitleController(@Autowired @NonNull TitleService titleService) {
        this.titleService = titleService;
    }

    @RequestMapping("/api/titles")
    public List<TitleResponse> getTitles() {
        Iterable<Title> titles = titleService.findAll();
        return StreamSupport.stream(titles.spliterator(), false)
                .map(TitleResponse::new)
                .collect(Collectors.toList());
    }

}
