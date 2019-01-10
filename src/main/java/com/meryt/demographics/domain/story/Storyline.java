package com.meryt.demographics.domain.story;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
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
