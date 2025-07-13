package com.salesforce.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a transcription event containing audio transcription results and PII entities.
 * This class models the response structure from the transcription service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptEvent {
    private Transcript transcript;

    /**
     * Gets all transcription results from this event.
     * @return list of results, or empty list if none available
     */
    public List<Result> getResults() {
        return transcript != null ? transcript.getResults() : Collections.emptyList();
    }

    /**
     * Gets all PII entities from this event.
     * @return list of PII entities, or empty list if none available
     */
    public List<PIIEntity> getAllPIIEntities() {
        return getResults().stream()
                .map(Result::getPIIEntities)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Checks if this event contains any transcription results.
     * @return true if results are present and non-empty
     */
    public boolean hasResults() {
        return transcript != null && transcript.hasResults();
    }

    /**
     * Gets the first available transcript text from this event.
     * @return transcript text or empty string if none available
     */
    public String getFirstTranscript() {
        return getResults().stream()
                .map(Result::getTranscript)
                .filter(text -> !text.isEmpty())
                .findFirst()
                .orElse("");
    }

    @Override
    public String toString() {
        return "TranscriptEvent{" +
                "transcript=" + transcript +
                '}';
    }
} 