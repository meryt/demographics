package com.meryt.demographics.domain.title;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.Person;
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

    @Enumerated(EnumType.STRING)
    private SocialClass socialClass;

    @Enumerated(EnumType.STRING)
    private Peerage peerage;

    @Enumerated(EnumType.STRING)
    private TitleInheritanceStyle inheritance;

    @ManyToOne(cascade = { CascadeType.ALL })
    private Person inheritanceRoot;

}
