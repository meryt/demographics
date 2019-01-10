package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.story.Storyline;
import com.meryt.demographics.domain.timeline.TimelineEntry;
import com.meryt.demographics.domain.timeline.TimelineEntryCategory;

@Getter
public class TimelineEntryResponse {

    private final long id;
    private final TimelineEntryCategory category;
    private final String content;
    private final String title;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private List<StorylineResponse> storylines;

    public TimelineEntryResponse(@NonNull TimelineEntry entry) {
        id = entry.getId();
        category = entry.getCategory();
        content = entry.getContent();
        title = entry.getTitle();
        fromDate = entry.getFromDate();
        toDate = entry.getToDate();
    }

    public void addStoryline(@NonNull Storyline storyline) {
        if (storylines == null) {
            storylines = new ArrayList<>();
        }
        StorylineResponse storylineResponse = new StorylineResponse(storyline);
        if (!storylines.contains(storylineResponse)) {
            storylines.add(storylineResponse);
        }
    }

}
