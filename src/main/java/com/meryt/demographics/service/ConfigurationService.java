package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.meryt.demographics.repository.CheckDateRepository;
import com.meryt.demographics.repository.ConfigurationRepository;
import com.meryt.demographics.rest.ConflictException;

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
    public LocalDate parseDate(@Nullable String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        if (date.equalsIgnoreCase("current")) {
            LocalDate currentDate = getCurrentDate();
            if (currentDate == null) {
                throw new ConflictException("Unable to use current date: No current date is set in the database");
            }
            return currentDate;
        }

        return LocalDate.parse(date);
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

    public Map<String, String> getAllConfiguration() {
        return configurationRepository.getAllConfiguration();
    }
}
