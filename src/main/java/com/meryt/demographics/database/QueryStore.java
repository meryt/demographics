package com.meryt.demographics.database;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.NonNull;

public class QueryStore {

    private final String repositoryName;

    public QueryStore(@NonNull String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getQuery(@NonNull String queryName) {
        String filePath = getFilePath(queryName);

        try {
            URL url = Resources.getResource(filePath);
            return Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to open querystore file %s", filePath), e);
        }
    }

    private String getFilePath(@NonNull String queryName) {
        return "db/querystore/" + repositoryName + "/" + queryName + ".sql";
    }
}
