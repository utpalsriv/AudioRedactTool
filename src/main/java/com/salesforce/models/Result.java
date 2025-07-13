package com.salesforce.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a single transcription result with timing and content information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private double startTime;
    private double endTime;
    private boolean isPartial;
    private List<Alternative> alternatives;
    private List<String> languageIdentification;
    private boolean speechFinal;

    public List<Alternative> getAlternatives() {
        return alternatives != null ? alternatives : Collections.emptyList();
    }

    public List<String> getLanguageIdentification() {
        return languageIdentification != null ? languageIdentification : Collections.emptyList();
    }

    /**
     * Gets the duration of this result in seconds.
     * @return duration in seconds
     */
    public double getDuration() {
        return endTime - startTime;
    }

    /**
     * Gets the primary transcript text from the first alternative.
     * @return transcript text or empty string if no alternatives
     */
    public String getTranscript() {
        return getFirstAlternative()
                .map(Alternative::getTranscript)
                .orElse("");
    }

    /**
     * Gets PII entities from the first alternative.
     * @return list of PII entities or empty list if none available
     */
    public List<PIIEntity> getPIIEntities() {
        return getFirstAlternative()
                .map(Alternative::getEntities)
                .orElse(Collections.emptyList());
    }

    /**
     * Gets the first alternative if available.
     * @return Optional containing the first alternative
     */
    public Optional<Alternative> getFirstAlternative() {
        return getAlternatives().isEmpty() ? Optional.empty() : Optional.of(getAlternatives().get(0));
    }

    @Override
    public String toString() {
        return "Result{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", transcript='" + getTranscript() + '\'' +
                ", isPartial=" + isPartial +
                ", speechFinal=" + speechFinal +
                ", alternativesCount=" + getAlternatives().size() +
                '}';
    }
} 