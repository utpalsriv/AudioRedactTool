package com.salesforce.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Represents the main transcript container with results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transcript {
    private List<Result> results;

    public List<Result> getResults() {
        return results != null ? results : Collections.emptyList();
    }

    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    @Override
    public String toString() {
        return "Transcript{" +
                "results=" + results +
                '}';
    }
} 