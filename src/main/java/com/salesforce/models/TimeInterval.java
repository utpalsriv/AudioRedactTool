package com.salesforce.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a time interval for redaction
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeInterval {
    private double startTime;
    private double endTime;

    /**
     * Gets the duration of this interval in seconds.
     * @return duration in seconds
     */
    public double getDuration() {
        return endTime - startTime;
    }

    @Override
    public String toString() {
        return String.format("TimeInterval{startTime=%.2f, endTime=%.2f, duration=%.2f}", 
            startTime, endTime, getDuration());
    }
} 