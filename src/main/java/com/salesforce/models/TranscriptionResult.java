package com.salesforce.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the complete transcription result including original text, redacted text, and PII intervals.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionResult {
    private String originalTranscription;
    private String redactedTranscription;
    private List<TimeInterval> piiIntervals;
    private List<PIIEntity> piiEntities;
    
    public TranscriptionResult(String originalTranscription, List<TimeInterval> piiIntervals, List<PIIEntity> piiEntities) {
        this.originalTranscription = originalTranscription;
        this.piiIntervals = piiIntervals;
        this.piiEntities = piiEntities;
        this.redactedTranscription = generateRedactedTranscription(originalTranscription, piiEntities);
    }
    
    /**
     * Generates redacted transcription by replacing PII entities with [REDACTED] markers
     */
    private String generateRedactedTranscription(String original, List<PIIEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return original;
        }
        
        String redacted = original;
        // Sort entities by start time in descending order to avoid index shifting issues
        List<PIIEntity> sortedEntities = entities.stream()
                .sorted((e1, e2) -> Double.compare(e2.getStartTime(), e1.getStartTime()))
                .toList();
        
        for (PIIEntity entity : sortedEntities) {
            String content = entity.getContent();
            if (content != null && !content.trim().isEmpty()) {
                redacted = redacted.replace(content, "[REDACTED]");
            }
        }
        
        return redacted;
    }
} 