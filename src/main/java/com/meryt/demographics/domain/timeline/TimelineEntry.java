package com.meryt.demographics.domain.timeline;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "timeline_entries")
@Getter
@Setter
public class TimelineEntry {

    @Id
    @SequenceGenerator(name="timeline_entries_id_seq", sequenceName="timeline_entries_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="timeline_entries_id_seq")
    private long id;

    @Enumerated(EnumType.STRING)
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
}
