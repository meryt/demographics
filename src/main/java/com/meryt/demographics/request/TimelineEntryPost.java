package com.meryt.demographics.request;

import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import com.meryt.demographics.domain.timeline.TimelineEntry;
import com.meryt.demographics.domain.timeline.TimelineEntryCategory;
import com.meryt.demographics.rest.BadRequestException;

@Getter
@Setter
public class TimelineEntryPost {

    private TimelineEntryCategory category;

    /**
     * Text content of the timeline entry
     */
    private String content;
    /**
     * Optional content to display on mouseover; otherwise content is used
     */
    private String title;

    /**
     * The start date of the timeline entry, or date if it is a single point in time (may not be null)
     */
    private LocalDate fromDate;

    /**
     * The end date of the timeline entry, or null if it is a single point in time
     */
    private LocalDate toDate;

    private List<Long> personIds;

    public void validate() {
        if (category == null) {
            throw new BadRequestException("category cannot be null");
        }
        if (content == null || StringUtils.isBlank(content)) {
            throw new BadRequestException("content cannot be null or empty");
        }
        if (fromDate == null) {
            throw new BadRequestException("fromDate cannot be null");
        }
    }

    public TimelineEntry toTimelineEntry() {
        TimelineEntry entry = new TimelineEntry();
        entry.setCategory(category);
        entry.setContent(content);
        entry.setTitle(title);
        entry.setFromDate(fromDate);
        entry.setToDate(toDate);
        return entry;
    }
}
