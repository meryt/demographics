package com.meryt.demographics.service;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.repository.PersonRepository;

@Service
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(@Autowired @NonNull PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public Person save(@NonNull Person person) {
        return personRepository.save(person);
    }
}
