package com.salesforce.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Represents a transcription alternative with text and PII entities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alternative {
    private String transcript;
    private List<Object> items;
    private List<PIIEntity> entities;

    public String getTranscript() {
        return transcript != null ? transcript : "";
    }

    public List<Object> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    public List<PIIEntity> getEntities() {
        return entities != null ? entities : Collections.emptyList();
    }

    /**
     * Checks if this alternative contains any PII entities.
     * @return true if entities are present
     */
    public boolean hasPIIEntities() {
        return !getEntities().isEmpty();
    }

    @Override
    public String toString() {
        return "Alternative{" +
                "transcript='" + getTranscript() + '\'' +
                ", entitiesCount=" + getEntities().size() +
                '}';
    }
} 