package com.salesforce.utils;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class StreamingBenchmarkUtils {
    
    // Audio format constants
    public static final int SAMPLE_RATE = 16000;
    public static final int BITS_PER_SAMPLE = 16;
    public static final int CHANNELS = 1;
    public static final int CHUNK_SIZE = 2048;
    public static final int CHUNK_DURATION_MS = 50;
    
    /**
     * Calculate chunk duration in milliseconds
     */
    public static long calculateChunkDurationMS() {
        return CHUNK_DURATION_MS;
    }
    
    /**
     * Get the chunk size in bytes
     */
    public static int getChunkSize() {
        return CHUNK_SIZE;
    }
    
    /**
     * Convert audio file to 16kHz PCM format
     */
    public static byte[] convertAudioTo16kHzPCM(String audioPath) throws UnsupportedAudioFileException, IOException {
        File audioFile = new File(audioPath);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
        
        // Create target format: 16kHz, 16-bit, mono
        AudioFormat targetFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            SAMPLE_RATE,
            BITS_PER_SAMPLE,
            CHANNELS,
            CHANNELS * BITS_PER_SAMPLE / 8,
            SAMPLE_RATE,
            false
        );
        
        // Convert to target format
        AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
        
        // Read all bytes
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = convertedStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        
        convertedStream.close();
        audioInputStream.close();
        
        return byteArrayOutputStream.toByteArray();
    }
} 