package com.meryt.demographics.domain.story;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.timeline.TimelineEntry;

@Entity
@Table(name = "storylines")
@Getter
@Setter
public class Storyline {
    @Id
    @SequenceGenerator(name="storylines_id_seq", sequenceName="storylines_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="storylines_id_seq")
    private long id;
    private String name;

    @ManyToMany
    @JoinTable(
            name = "storyline_timeline_entries",
            joinColumns = @JoinColumn(name = "storyline_id"),
            inverseJoinColumns = @JoinColumn(name = "timeline_entry_id")
    )
    @OrderBy("fromDate")
    private List<TimelineEntry> timelineEntries = new ArrayList<>();
}
