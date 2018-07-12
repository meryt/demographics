package com.meryt.demographics.service;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import com.meryt.demographics.repository.CheckDateRepository;

@Service
public class CheckDateService {

    private final CheckDateRepository checkDateRepository;

    public CheckDateService(@NonNull CheckDateRepository checkDateRepository) {
        this.checkDateRepository = checkDateRepository;
    }

    @Nullable
    public LocalDate getCurrentDate() {
        return checkDateRepository.getCurrentDate();
    }

    public void setCurrentDate(@NonNull LocalDate date) {
        checkDateRepository.setCurrentDate(date);
    }
}
