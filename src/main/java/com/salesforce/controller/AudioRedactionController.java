package com.salesforce.controller;

import com.salesforce.models.TimeInterval;
import com.salesforce.models.TranscriptionResult;
import com.salesforce.service.TranscribeWithPII;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.salesforce.service.AudioRedactionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioRedactionController {

    private final AudioRedactionService audioRedactionService;
    private final TranscribeWithPII transcribeWithPIIService;
    private final String uploadDir = "uploads";
    private final String outputDir = "outputs";


    @PostMapping("/redact")
    public ResponseEntity<Map<String, Object>> redactAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replacementMethod", defaultValue = "beep") String replacementMethod,
            @RequestParam(value = "beepFrequency", required = false) Float beepFrequency,
            @RequestParam(value = "beepDuration", required = false) Float beepDuration,
            @RequestParam(value = "beepVolume", required = false) Float beepVolume,
            @RequestParam(value = "soundType", defaultValue = "beep") String soundType) {

        try {
            log.info("Received redact request - method: {}, frequency: {}, duration: {}, volume: {}, sound type: {}", 
                    replacementMethod, beepFrequency, beepDuration, beepVolume, soundType);
            
            // Create directories if they don't exist
            createDirectories();

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueId = UUID.randomUUID().toString();
            String inputFileName = uniqueId + "_input" + fileExtension;
            String outputFileName = uniqueId + "_redacted.wav";

            // Save uploaded file using absolute paths
            Path inputPath = getAbsolutePath(uploadDir).resolve(inputFileName);
            file.transferTo(inputPath.toFile());

            TranscriptionResult transcriptionResult = 
                transcribeWithPIIService.transcribeAndDetectPII(inputPath.toAbsolutePath().toString());

            // Process audio
            String inputFile = inputPath.toString();
            String outputFile = getAbsolutePath(outputDir).resolve(outputFileName).toString();

            // Use default values for beep parameters if not provided
            float frequency = (beepFrequency != null) ? beepFrequency : 1000.0f;
            float duration = (beepDuration != null) ? beepDuration : 0.5f;
            float volume = (beepVolume != null) ? beepVolume : 0.3f;

            log.info("Processing with - frequency: {}, duration: {}, volume: {}, sound type: {}", frequency, duration, volume, soundType);

            audioRedactionService.redactAudio(inputFile, outputFile, transcriptionResult.getPiiIntervals(), replacementMethod, frequency, duration, volume, soundType);

            // Clean up input file
            Files.deleteIfExists(inputPath);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("fileId", uniqueId);
            response.put("originalTranscription", transcriptionResult.getOriginalTranscription());
            response.put("redactedTranscription", transcriptionResult.getRedactedTranscription());
            response.put("piiIntervals", transcriptionResult.getPiiIntervals());
            response.put("piiEntities", transcriptionResult.getPiiEntities());
            response.put("message", "Audio redacted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing audio redaction request", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadRedactedAudio(@PathVariable String fileId) {
        try {
            Path filePath = getAbsolutePath(outputDir).resolve(fileId + "_redacted.wav");
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error downloading file", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/cleanup/{fileId}")
    public ResponseEntity<String> cleanupFile(@PathVariable String fileId) {
        try {
            Path filePath = getAbsolutePath(outputDir).resolve(fileId + "_redacted.wav");
            Files.deleteIfExists(filePath);
            return ResponseEntity.ok("File cleaned up successfully");
        } catch (Exception e) {
            log.error("Error cleaning up file", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    private void createDirectories() throws IOException {
        Files.createDirectories(getAbsolutePath(uploadDir));
        Files.createDirectories(getAbsolutePath(outputDir));
    }

    private Path getAbsolutePath(String relativePath) {
        // Get the current working directory and resolve the relative path
        return Paths.get(System.getProperty("user.dir")).resolve(relativePath);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".wav";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private List<TimeInterval> parseIntervals(String intervalsJson) {
        log.info("Parsing intervals from: '{}'", intervalsJson);
        
        // Simple parsing: "start1,end1;start2,end2"
        List<TimeInterval> intervals = java.util.Arrays.stream(intervalsJson.split(";"))
                .map(interval -> {
                    String[] parts = interval.split(",");
                    double start = Double.parseDouble(parts[0].trim());
                    double end = Double.parseDouble(parts[1].trim());
                    log.info("Parsed interval: {} to {} seconds", start, end);
                    return new TimeInterval(start, end);
                }).collect(Collectors.toList());
        
        log.info("Total intervals parsed: {}", intervals.size());
        return intervals;
    }
}