package com.salesforce.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Represents a Personally Identifiable Information entity detected in the transcript.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PIIEntity {
    private String type;
    private double startTime;
    private double endTime;
    private String content;
    private double confidence;

    public String getType() {
        return Objects.requireNonNull(type, "PII type cannot be null");
    }

    public String getContent() {
        return content != null ? content : "";
    }

    /**
     * Gets the duration of this PII entity in seconds.
     * @return duration in seconds
     */
    public double getDuration() {
        return endTime - startTime;
    }

    /**
     * Checks if this entity has high confidence (above 0.8).
     * @return true if confidence is high
     */
    public boolean isHighConfidence() {
        return confidence > 0.8;
    }

    @Override
    public String toString() {
        return "PIIEntity{" +
                "type='" + getType() + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", content='" + getContent() + '\'' +
                ", confidence=" + confidence +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PIIEntity piiEntity = (PIIEntity) obj;
        return Double.compare(piiEntity.startTime, startTime) == 0 &&
               Double.compare(piiEntity.endTime, endTime) == 0 &&
               Double.compare(piiEntity.confidence, confidence) == 0 &&
               Objects.equals(getType(), piiEntity.getType()) &&
               Objects.equals(getContent(), piiEntity.getContent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), startTime, endTime, getContent(), confidence);
    }
} 