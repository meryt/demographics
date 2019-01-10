package com.meryt.demographics.response;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.story.Storyline;

@Getter
public class StorylineResponse {

    private final long id;
    private final String name;

    public StorylineResponse(@NonNull Storyline storyline) {
        this.id = storyline.getId();
        this.name = storyline.getName();
    }
}
