package com.meryt.demographics.domain.person;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "traits")
public class Trait {
    @Id
    @SequenceGenerator(name="traits_id_seq", sequenceName="traits_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="traits_id_seq")
    private long id;

    private int rating;

    private String name;
}
