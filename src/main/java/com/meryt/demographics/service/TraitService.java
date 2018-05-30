package com.meryt.demographics.service;

import java.util.List;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.person.Trait;
import com.meryt.demographics.repository.TraitRepository;

@Service
public class TraitService {

    private final TraitRepository traitRepository;

    public TraitService(@Autowired @NonNull TraitRepository traitRepository) {
        this.traitRepository = traitRepository;
    }

    public List<Trait> randomTraits(int num) {
        return traitRepository.randomTraits(num);
    }

}
