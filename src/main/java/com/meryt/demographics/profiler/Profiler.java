package com.meryt.demographics.profiler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Profiler {

    private Map<String, Profile> profiles = new LinkedHashMap<>();

    private List<String> mostRecentlyStarted = new ArrayList<>();

    public void start(@NonNull String profileName) {
        getOrInit(profileName).start();
        mostRecentlyStarted.add(profileName);
    }

    public void stop() {
        if (mostRecentlyStarted.isEmpty()) {
            throw new IllegalStateException("No profile was recently started");
        }
        stop(mostRecentlyStarted.get(mostRecentlyStarted.size() - 1));
    }

    public void stop(@NonNull String profileName) {
        getOrFail(profileName).stop();
        for (int i = mostRecentlyStarted.size() - 1; i >= 0; i--) {
            if (mostRecentlyStarted.get(i).equals(profileName)) {
                mostRecentlyStarted.remove(i);
                break;
            }
        }
    }

    public void logResults() {
        String result = profiles.values().stream()
                .map(Profile::getResult)
                .collect(Collectors.joining(" "));
        log.info(result);
    }

    @NonNull
    private Profile getOrInit(@NonNull String name) {
        if (!profiles.containsKey(name)) {
            profiles.put(name, new Profile(name));
        }
        return profiles.get(name);
    }

    private Profile getOrFail(@NonNull String name) {
        if (!profiles.containsKey(name)) {
            throw new IllegalStateException(String.format("No profile for %s has already started", name));
        }
        return profiles.get(name);
    }

    private static class Profile {
        private boolean running;
        private long startTime;
        @Getter
        private long elapsed = 0;

        private final String name;

        private Profile(@NonNull String name) {
            this.name = name;
        }

        void start() {
            if (running) {
                throw new IllegalStateException(name + " profiler is already started");
            }
            running = true;
            startTime = System.currentTimeMillis();
        }

        void stop() {
            if (!running) {
                throw new IllegalStateException(name + " profiler is not running");
            }
            running = false;
            elapsed += System.currentTimeMillis() - startTime;
        }

        String getResult() {
            return String.format("%s=%d", name, elapsed);
        }
    }

}
