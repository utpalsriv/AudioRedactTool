package com.salesforce;

import com.salesforce.service.TranscribeWithPII;
import com.salesforce.models.TimeInterval;
import com.salesforce.models.TranscriptionResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class TranscribeWithPIITest {

    public static void main(String[] args) {
        log.info("=== TranscribeWithPII Service Test ===");

        // Test audio file path - update this to your actual audio file path
        String audioFilePath = "/Users/a.bhati/Downloads/original-audio.wav";
        
        // If command line argument is provided, use it instead
        if (args.length > 0) {
            audioFilePath = args[0];
        }
        
        log.info("Testing with audio file: {}", audioFilePath);
        
        try {
            // Create the service instance
            TranscribeWithPII transcribeWithPII = new TranscribeWithPII();
            
            // Call the service method
            log.info("Starting PII detection...");
            TranscriptionResult result = 
                transcribeWithPII.transcribeAndDetectPII(audioFilePath);
            
            // Display results
            log.info("=== Transcription Results ===");
            log.info("Original Transcription: {}", result.getOriginalTranscription());
            log.info("Redacted Transcription: {}", result.getRedactedTranscription());
            
            List<TimeInterval> piiIntervals = result.getPiiIntervals();
            log.info("=== PII Detection Results ===");
            if (piiIntervals.isEmpty()) {
                log.info("No PII intervals detected in the audio file.");
            } else {
                log.info("Found {} PII intervals:", piiIntervals.size());
                for (int i = 0; i < piiIntervals.size(); i++) {
                    TimeInterval interval = piiIntervals.get(i);
                    log.info("  {}. Start: {}s, End: {}s, Duration: {}s", 
                        i + 1, 
                        String.format("%.2f", interval.getStartTime()), 
                        String.format("%.2f", interval.getEndTime()),
                        String.format("%.2f", interval.getDuration()));
                }
            }
            log.info("=== Test Completed Successfully ===");
            
        } catch (Exception e) {
            log.error("Error during PII detection test: {}", e.getMessage(), e);
            log.error("Test failed. Please check the audio file path and ensure the transcription service is accessible.");
            System.exit(1);
        }
    }
} 