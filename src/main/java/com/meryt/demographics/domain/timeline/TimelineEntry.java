package com.meryt.demographics.domain.timeline;

import java.time.LocalDate;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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
