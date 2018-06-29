package com.meryt.demographics.domain.title;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.person.SocialClass;

@Getter
@Setter
@Entity
@Table(name = "titles")
public class Title {

    @Id
    @SequenceGenerator(name="titles_id_seq", sequenceName="titles_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="titles_id_seq")
    private long id;

    private String name;

    private boolean extinct;

    @Enumerated(EnumType.STRING)
    private SocialClass socialClass;

    @Enumerated(EnumType.STRING)
    private Peerage peerage;

    @Enumerated(EnumType.STRING)
    private TitleInheritanceStyle inheritance;

    @ManyToOne
    private Person inheritanceRoot;

    @OneToMany(mappedBy = "title", cascade = { CascadeType.ALL })
    @OrderBy("from_date")
    private List<PersonTitlePeriod> titleHolders = new ArrayList<>();

    private LocalDate nextAbeyanceCheckDate;

}
