package com.meryt.demographics.service;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import com.meryt.demographics.repository.CheckDateRepository;
import com.meryt.demographics.repository.ConfigurationRepository;

@Service
public class ConfigurationService {

    private final CheckDateRepository checkDateRepository;
    private final ConfigurationRepository configurationRepository;

    public ConfigurationService(@NonNull CheckDateRepository checkDateRepository,
                                @NonNull ConfigurationRepository configurationRepository) {
        this.checkDateRepository = checkDateRepository;
        this.configurationRepository = configurationRepository;
    }

    @Nullable
    public LocalDate getCurrentDate() {
        return checkDateRepository.getCurrentDate();
    }

    public void setCurrentDate(@NonNull LocalDate date) {
        checkDateRepository.setCurrentDate(date);
    }

    public void pauseCheck() {
        configurationRepository.pauseCheck();
    }

    public void unpauseCheck() {
        configurationRepository.unpauseCheck();
    }

    public boolean isPauseCheck() {
        return configurationRepository.isPauseCheck();
    }
}
