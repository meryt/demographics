package com.meryt.demographics.domain.title;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
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
import lombok.NonNull;
import lombok.Setter;
import org.thymeleaf.util.StringUtils;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.place.DwellingPlace;

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

    @OneToMany(mappedBy = "entailedTitle", cascade = { CascadeType.ALL })
    private final Set<DwellingPlace> entailedProperties = new HashSet<>();

    public boolean singleFemaleMayInherit() {
        return !inheritance.isMalesOnly() && peerage == Peerage.SCOTLAND;
    }

    @Nullable
    public Person getHolder(@NonNull LocalDate onDate) {
        return titleHolders.stream()
                .filter(ptp -> ptp.contains(onDate))
                .findFirst()
                .map(PersonTitlePeriod::getPerson).orElse(null);
    }

    @Override
    public String toString() {
        return String.format("%d %s%s (Peerage of %s)", id, name, (extinct ? " (extinct)" : ""),
                (peerage == null ? "null" : StringUtils.capitalizeWords(peerage.name().toLowerCase())));
    }

}
