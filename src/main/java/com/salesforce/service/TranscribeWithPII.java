package com.salesforce.service;

import com.google.gson.Gson;
import com.salesforce.utils.TranscriptionWebSocketClient;
import com.salesforce.models.TranscriptEvent;
import com.salesforce.models.Result;
import com.salesforce.models.PIIEntity;
import com.salesforce.models.TimeInterval;
import com.salesforce.models.TranscriptionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.HashSet;
import java.util.Set;

import static com.salesforce.utils.StreamingBenchmarkUtils.*;
import static java.lang.Thread.sleep;

@Slf4j
@Service
public class TranscribeWithPII {

    private static final long CHUNK_DURATION_MS = calculateChunkDurationMS();
    private static final Gson gson = new Gson();

    /**
     * Transcribes an audio file and returns transcription results with PII detection
     * @param audioFilePath Path to the audio file to transcribe
     * @return TranscriptionResult object containing original transcription, redacted transcription, and PII intervals
     * @throws Exception if transcription fails
     */
    public TranscriptionResult transcribeAndDetectPII(String audioFilePath) throws Exception {
        CompletableFuture<Void> openFuture = new CompletableFuture<>();
        CompletableFuture<Void> closeFuture = new CompletableFuture<>();

        log.info("Starting transcription for file: {}", audioFilePath);
        String websocketUrl = "wss://einstein-transcribe.sfproxy.einstein.perf2-uswest2.aws.sfdc.cl/transcribe/v1/tenant/stream";

        // Create WebSocket client
        final TranscriptionWebSocketClient clientEndPoint = new TranscriptionWebSocketClient(
                new URI(websocketUrl + "?engine=aws&media-encoding=pcm&media-sample-rate-hertz=16000&content-redaction-type=PII&pii-entity-types=ALL"),
                Map.of("x-sfdc-app-context", "EinsteinGPT"),
                openFuture,
                closeFuture);

        final List<PIIEntity> allPIIEntities = new ArrayList<>();
        final Set<String> processedSegments = new HashSet<>();
        final StringBuilder finalTranscription = new StringBuilder();
        final boolean[] finalTranscriptionReceived = {false};

        clientEndPoint.addMessageHandler(message -> {
            try {
                log.debug("Received message: {}", message);
                
                TranscriptEvent event = gson.fromJson(message, TranscriptEvent.class);
                if(event != null && event.getTranscript() != null && event.getTranscript().hasResults() && !event.getTranscript().getResults().isEmpty()) {
                    Result transcriptResult = event.getTranscript().getResults().get(0);
                    
                    if (!transcriptResult.isPartial()) {
                        String transcript = transcriptResult.getTranscript();
                        
                        // Create a unique key for this segment to avoid duplicates
                        String segmentKey = transcriptResult.getStartTime() + "-" + transcriptResult.getEndTime() + "-" + transcript.hashCode();
                        
                        // Only process if we haven't seen this segment before
                        if (!processedSegments.contains(segmentKey)) {
                            processedSegments.add(segmentKey);
                            
                            // Extract PII entities directly from the parsed event
                            List<PIIEntity> piiEntities = event.getAllPIIEntities();
                            if (!piiEntities.isEmpty()) {
                                log.info("=== PII INTERVALS FOUND ===");
                                for (PIIEntity entity : piiEntities) {
                                    log.info("PII Entity: {}", entity.toString());
                                }
                                log.info("==========================");
                            }
                            
                            // Add only unique PII intervals (deduplicate by content and timing)
                            for (PIIEntity newEntity : piiEntities) {
                                boolean isDuplicate = false;
                                for (PIIEntity existingEntity : allPIIEntities) {
                                    if (existingEntity.getContent().equals(newEntity.getContent()) &&
                                        Math.abs(existingEntity.getStartTime() - newEntity.getStartTime()) < 1.0) {
                                        isDuplicate = true;
                                        break;
                                    }
                                }
                                if (!isDuplicate) {
                                    allPIIEntities.add(newEntity);
                                }
                            }
                            
                            finalTranscription.append(transcript).append(" ");
                            finalTranscriptionReceived[0] = true;
                            
                            log.info("=== SEGMENT TRANSCRIPTION ===");
                            log.info("Time: {}s - {}s", transcriptResult.getStartTime(), transcriptResult.getEndTime());
                            log.info("Text: {}", transcript);
                            log.info("============================");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing message: {}", e.getMessage(), e);
            }
        });

        log.info("Connecting to WebSocket...");
        clientEndPoint.connect();
        
        try {
            openFuture.get();
            log.info("WebSocket connected successfully!");
        } catch (Exception e) {
            log.error("Failed to connect to WebSocket: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect to transcription service", e);
        }

        log.info("Sending audio stream...");
        try {
            sendAudioStream(clientEndPoint, audioFilePath);
            
            // Wait for final transcription
            int waitCount = 0;
            while (!finalTranscriptionReceived[0] && waitCount < 30) {
                sleep(1000);
                waitCount++;
            }
            
        } catch (Exception e) {
            log.error("Error during audio streaming: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to stream audio", e);
        }

        // Log final results
        log.info("\n=== COMPLETE TRANSCRIPTION ===");
        log.info("{}", finalTranscription.toString());

        log.info("\n=== PII INTERVALS FOR BEEP INSERTION ===");
        if (!allPIIEntities.isEmpty()) {
            log.info("Total PII intervals: {}", allPIIEntities.size());
            for (PIIEntity entity : allPIIEntities) {
                log.info(" {}s - {}s ({}: {})",
                    String.format("%.2f", entity.getStartTime()), 
                    String.format("%.2f", entity.getEndTime()), 
                    entity.getType(), entity.getContent());
            }
        } else {
            log.info("No PII intervals detected");
        }

        try {
            closeFuture.get();
            log.info("Transcription completed successfully.");
        } catch (Exception e) {
            log.error("Error waiting for completion: {}", e.getMessage(), e);
        }

        // Convert PII entities to TimeInterval objects
        List<TimeInterval> timeIntervals = new ArrayList<>();
        for (PIIEntity entity : allPIIEntities) {
            timeIntervals.add(new TimeInterval(entity.getStartTime() - 0.25, entity.getEndTime()));
        }

        return new TranscriptionResult(finalTranscription.toString(), timeIntervals, allPIIEntities);
    }

    /**
     * Transcribes an audio file and returns time intervals where PII was detected
     * @param audioFilePath Path to the audio file to transcribe
     * @return List of TimeInterval objects representing PII detection intervals
     * @throws Exception if transcription fails
     * @deprecated Use transcribeAndDetectPII() which returns TranscriptionResult instead
     */
    @Deprecated
    public List<TimeInterval> transcribeAndDetectPIIIntervals(String audioFilePath) throws Exception {
        TranscriptionResult result = transcribeAndDetectPII(audioFilePath);
        return result.getPiiIntervals();
    }

    private void sendAudioStream(TranscriptionWebSocketClient clientEndPoint, String audioPath) throws UnsupportedAudioFileException, IOException, InterruptedException {
        log.info("Loading audio file: {}", audioPath);
        byte[] audioBytesData = convertAudioTo16kHzPCM(audioPath);
        log.info("Audio file loaded, size: {} bytes", audioBytesData.length);

        int chunkSize = getChunkSize();
        long startTime = System.currentTimeMillis();
        int chunkCount = 0;

        for (int i = 0, j=0; i < audioBytesData.length; i += chunkSize, j++) {
            if (!clientEndPoint.isOpen()) {
                log.error("WebSocket connection lost! Stopping audio stream.");
                break;
            }

            int end = Math.min(i + chunkSize, audioBytesData.length);
            byte[] chunk = Arrays.copyOfRange(audioBytesData, i, end);
            byte[] fullSizedChunk = new byte[CHUNK_SIZE];
            System.arraycopy(chunk, 0, fullSizedChunk, 0, chunk.length);

            long elapsed = System.currentTimeMillis() - startTime;
            long desiredSendTime = (j + 1) * CHUNK_DURATION_MS;
            if (elapsed < desiredSendTime) {
                sleep(desiredSendTime - elapsed);
            }

            try {
                clientEndPoint.send(ByteBuffer.wrap(fullSizedChunk));
                chunkCount++;
            } catch (Exception e) {
                log.error("Error sending chunk {}: {}", chunkCount, e.getMessage(), e);
                break;
            }

            // Reduced logging - only show every 10 chunks
            if (chunkCount % 10 == 0) {
                log.debug("Sent {} chunks...", chunkCount);
            }
        }

        log.info("Sending final chunk...");
        try {
            if (clientEndPoint.isOpen()) {
                clientEndPoint.send(ByteBuffer.allocate(0));
                sleep(1000);
            }
        } catch (Exception e) {
            log.error("Error sending final chunk: {}", e.getMessage(), e);
        }
        log.info("Total chunks sent: {}", chunkCount);
    }
} 