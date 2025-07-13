package com.salesforce.service;

import com.salesforce.models.TimeInterval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.*;
import java.util.List;

@Slf4j
@Service
public class AudioRedactionService {

    /**
     * Redacts audio by replacing specified time intervals with beep sounds
     *
     * @param inputFile          Input audio file (WAV or MP3)
     * @param outputFile         Output audio file
     * @param redactionIntervals List of time intervals to redact (in seconds)
     * @param beepFrequency      Frequency of the beep sound (in Hz)
     * @param beepDuration       Duration of each beep (in seconds)
     * @param beepVolume         Volume of the beep sound (0.0 to 1.0)
     * @param soundType          Type of sound to generate ("beep", "chime", "soft", "gentle")
     */
    public void redactAudioWithBeep(String inputFile, String outputFile,
                                    List<TimeInterval> redactionIntervals,
                                    float beepFrequency, float beepDuration, float beepVolume, String soundType) {
        try {
            // Load the audio file
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(inputFile));
            AudioFormat format = audioInputStream.getFormat();

            log.info("Processing audio file: {} with format: {}", inputFile, format);

            // Convert to PCM if needed
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        format.getSampleRate(),
                        16,
                        format.getChannels(),
                        format.getChannels() * 2,
                        format.getSampleRate(),
                        false
                );
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                format = targetFormat;
            }

            // Read all audio data
            byte[] audioData = readAudioData(audioInputStream);

            // Generate replacement sound based on type
            byte[] replacementData = generateReplacementSound(format, beepFrequency, beepDuration, beepVolume, soundType);

            // Apply redactions
            byte[] redactedData = applyRedactions(audioData, format, redactionIntervals, replacementData);

            // Write output file
            writeAudioFile(redactedData, format, outputFile);

            log.info("Audio redaction completed. Output saved to: {}", outputFile);

        } catch (Exception e) {
            log.error("Error during audio redaction", e);
            throw new RuntimeException("Failed to redact audio", e);
        }
    }

    /**
     * Redacts audio by replacing specified time intervals with silence
     *
     * @param inputFile          Input audio file (WAV or MP3)
     * @param outputFile         Output audio file
     * @param redactionIntervals List of time intervals to redact (in seconds)
     */
    public void redactAudioWithSilence(String inputFile, String outputFile,
                                       List<TimeInterval> redactionIntervals) {
        try {
            // Load the audio file
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(inputFile));
            AudioFormat format = audioInputStream.getFormat();

            log.info("Processing audio file: {} with format: {}", inputFile, format);

            // Convert to PCM if needed
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        format.getSampleRate(),
                        16,
                        format.getChannels(),
                        format.getChannels() * 2,
                        format.getSampleRate(),
                        false
                );
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                format = targetFormat;
            }

            // Read all audio data
            byte[] audioData = readAudioData(audioInputStream);

            // Apply redactions with silence
            byte[] redactedData = applyRedactionsWithSilence(audioData, format, redactionIntervals);

            // Write output file
            writeAudioFile(redactedData, format, outputFile);

            log.info("Audio redaction with silence completed. Output saved to: {}", outputFile);

        } catch (Exception e) {
            log.error("Error during audio redaction with silence", e);
            throw new RuntimeException("Failed to redact audio with silence", e);
        }
    }

    /**
     * Redacts audio with specified replacement method (beep or silence)
     *
     * @param inputFile          Input audio file (WAV or MP3)
     * @param outputFile         Output audio file
     * @param redactionIntervals List of time intervals to redact (in seconds)
     * @param replacementMethod  "beep" or "silence"
     * @param beepFrequency      Frequency of the beep sound (in Hz) - only used if replacementMethod is "beep"
     * @param beepDuration       Duration of each beep (in seconds) - only used if replacementMethod is "beep"
     * @param beepVolume         Volume of the beep sound (0.0 to 1.0) - only used if replacementMethod is "beep"
     * @param soundType          Type of sound to generate ("beep", "chime", "soft", "gentle") - only used if replacementMethod is "beep"
     */
    public void redactAudio(String inputFile, String outputFile,
                            List<TimeInterval> redactionIntervals,
                            String replacementMethod,
                            float beepFrequency, float beepDuration, float beepVolume, String soundType) {
        log.info("Redacting audio with method: {}, frequency: {} Hz, duration: {} seconds, volume: {}, sound type: {}", 
                replacementMethod, beepFrequency, beepDuration, beepVolume, soundType);
        log.info("Redaction intervals: {}", redactionIntervals);
        
        if ("silence".equalsIgnoreCase(replacementMethod)) {
            redactAudioWithSilence(inputFile, outputFile, redactionIntervals);
        } else {
            redactAudioWithBeep(inputFile, outputFile, redactionIntervals, beepFrequency, beepDuration, beepVolume, soundType);
        }
    }

    private byte[] readAudioData(AudioInputStream audioInputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = audioInputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }

        return baos.toByteArray();
    }

    private byte[] generateBeep(AudioFormat format, float frequency, float duration, float volume) {
        log.info("Generating beep with frequency: {} Hz, duration: {} seconds, volume: {}", frequency, duration, volume);
        
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int bitsPerSample = format.getSampleSizeInBits();
        int bytesPerSample = bitsPerSample / 8;

        int numSamples = (int) (sampleRate * duration);
        byte[] beepData = new byte[numSamples * channels * bytesPerSample];

        // Log a few sample values for debugging
        int debugSamples = Math.min(10, numSamples);
        
        for (int i = 0; i < numSamples; i++) {
            double time = (double) i / sampleRate;
            double amplitude = Math.sin(2 * Math.PI * frequency * time);

            // Convert to 16-bit PCM with configurable volume
            short sample = (short) (amplitude * 32767 * volume);
            
            // Log first few samples for debugging
            if (i < debugSamples) {
                log.info("Sample {}: amplitude={}, volume={}, final_sample={}", i, amplitude, volume, sample);
            }

            for (int ch = 0; ch < channels; ch++) {
                int index = (i * channels + ch) * bytesPerSample;
                beepData[index] = (byte) (sample & 0xFF);
                beepData[index + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        log.info("Generated beep data with {} samples, {} channels, {} bytes per sample", numSamples, channels, bytesPerSample);
        return beepData;
    }

    private byte[] applyRedactions(byte[] audioData, AudioFormat format,
                                   List<TimeInterval> redactionIntervals, byte[] beepData) {
        log.info("Applying redactions to {} intervals with beep data of {} bytes", redactionIntervals.size(), beepData.length);
        
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        int bytesPerFrame = channels * bytesPerSample;

        byte[] result = audioData.clone();

        for (TimeInterval interval : redactionIntervals) {
            int startSample = (int) (interval.getStartTime() * sampleRate);
            int endSample = (int) (interval.getEndTime() * sampleRate);

            int startByte = startSample * bytesPerFrame;
            int endByte = endSample * bytesPerFrame;

            log.info("Redacting interval: {} to {} seconds (samples {} to {}, bytes {} to {})", 
                    interval.getStartTime(), interval.getEndTime(), startSample, endSample, startByte, endByte);

            // Ensure we don't go beyond the audio data
            if (endByte > result.length) {
                endByte = result.length;
                log.warn("Adjusted end byte to {} to stay within audio data bounds", endByte);
            }

            // Replace the interval with beep sounds
            int intervalLength = endByte - startByte;
            int beepLength = beepData.length;

            log.info("Replacing {} bytes with beep pattern of {} bytes", intervalLength, beepLength);

            int beepIndex = 0;
            for (int i = startByte; i < endByte; i++) {
                result[i] = beepData[beepIndex % beepLength];
                beepIndex++;
            }
        }

        return result;
    }

    private byte[] applyRedactionsWithSilence(byte[] audioData, AudioFormat format,
                                              List<TimeInterval> redactionIntervals) {
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        int bytesPerFrame = channels * bytesPerSample;

        byte[] result = audioData.clone();

        for (TimeInterval interval : redactionIntervals) {
            int startSample = (int) (interval.getStartTime() * sampleRate);
            int endSample = (int) (interval.getEndTime() * sampleRate);

            int startByte = startSample * bytesPerFrame;
            int endByte = endSample * bytesPerFrame;

            // Ensure we don't go beyond the audio data
            if (endByte > result.length) {
                endByte = result.length;
            }

            // Replace the interval with silence (zeros)
            for (int i = startByte; i < endByte; i++) {
                result[i] = 0;
            }
        }

        return result;
    }

    private void writeAudioFile(byte[] audioData, AudioFormat format, String outputFile) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
        AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());

        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(outputFile));
    }

    /**
     * Generates replacement sound based on the specified type
     */
    private byte[] generateReplacementSound(AudioFormat format, float frequency, float duration, float volume, String soundType) {
        log.info("Generating {} sound with frequency: {} Hz, duration: {} seconds, volume: {}", soundType, frequency, duration, volume);
        
        switch (soundType.toLowerCase()) {
            case "chime":
                return generateChimeSound(format, duration, volume);
            case "soft":
                return generateSoftTone(format, frequency, duration, volume);
            case "gentle":
                return generateGentleTone(format, frequency, duration, volume);
            case "beep":
            default:
                return generateBeep(format, frequency, duration, volume);
        }
    }

    /**
     * Generates a soothing chime-like sound
     */
    private byte[] generateChimeSound(AudioFormat format, float duration, float volume) {
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int bitsPerSample = format.getSampleSizeInBits();
        int bytesPerSample = bitsPerSample / 8;

        int numSamples = (int) (sampleRate * duration);
        byte[] chimeData = new byte[numSamples * channels * bytesPerSample];

        // Create a chime with multiple harmonics for a more musical sound
        double[] frequencies = {800.0, 1200.0, 1600.0}; // Harmonic series
        double[] amplitudes = {1.0, 0.6, 0.3}; // Decreasing amplitudes for harmonics

        for (int i = 0; i < numSamples; i++) {
            double time = (double) i / sampleRate;
            double amplitude = 0.0;

            // Combine multiple frequencies for rich tone
            for (int j = 0; j < frequencies.length; j++) {
                amplitude += amplitudes[j] * Math.sin(2 * Math.PI * frequencies[j] * time);
            }

            // Apply exponential decay for natural chime sound
            double decay = Math.exp(-time * 3.0); // Faster decay for chime effect
            amplitude *= decay;

            // Apply fade-in and fade-out
            double fadeIn = Math.min(1.0, time / 0.05); // 50ms fade-in
            double fadeOut = Math.min(1.0, (duration - time) / 0.05); // 50ms fade-out
            amplitude *= fadeIn * fadeOut;

            // Convert to 16-bit PCM with configurable volume
            short sample = (short) (amplitude * 32767 * volume * 0.3); // Reduced overall volume for chime

            for (int ch = 0; ch < channels; ch++) {
                int index = (i * channels + ch) * bytesPerSample;
                chimeData[index] = (byte) (sample & 0xFF);
                chimeData[index + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        log.info("Generated chime sound with {} samples", numSamples);
        return chimeData;
    }

    /**
     * Generates a soft tone with gentle fade effects
     */
    private byte[] generateSoftTone(AudioFormat format, float frequency, float duration, float volume) {
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int bitsPerSample = format.getSampleSizeInBits();
        int bytesPerSample = bitsPerSample / 8;

        int numSamples = (int) (sampleRate * duration);
        byte[] softData = new byte[numSamples * channels * bytesPerSample];

        for (int i = 0; i < numSamples; i++) {
            double time = (double) i / sampleRate;
            
            // Use a lower frequency for softer sound
            double baseFreq = frequency * 0.7; // 30% lower frequency
            double amplitude = Math.sin(2 * Math.PI * baseFreq * time);

            // Add a subtle second harmonic for warmth
            amplitude += 0.2 * Math.sin(2 * Math.PI * baseFreq * 2 * time);

            // Apply gentle fade-in and fade-out
            double fadeIn = Math.min(1.0, time / 0.1); // 100ms fade-in
            double fadeOut = Math.min(1.0, (duration - time) / 0.1); // 100ms fade-out
            amplitude *= fadeIn * fadeOut;

            // Apply soft envelope
            double envelope = Math.sin(Math.PI * time / duration);
            amplitude *= envelope;

            // Convert to 16-bit PCM with reduced volume
            short sample = (short) (amplitude * 32767 * volume * 0.4); // 40% of original volume

            for (int ch = 0; ch < channels; ch++) {
                int index = (i * channels + ch) * bytesPerSample;
                softData[index] = (byte) (sample & 0xFF);
                softData[index + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        log.info("Generated soft tone with {} samples", numSamples);
        return softData;
    }

    /**
     * Generates a very gentle, barely audible tone
     */
    private byte[] generateGentleTone(AudioFormat format, float frequency, float duration, float volume) {
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int bitsPerSample = format.getSampleSizeInBits();
        int bytesPerSample = bitsPerSample / 8;

        int numSamples = (int) (sampleRate * duration);
        byte[] gentleData = new byte[numSamples * channels * bytesPerSample];

        for (int i = 0; i < numSamples; i++) {
            double time = (double) i / sampleRate;
            
            // Use very low frequency for gentle sound
            double baseFreq = frequency * 0.5; // 50% lower frequency
            double amplitude = Math.sin(2 * Math.PI * baseFreq * time);

            // Very long fade-in and fade-out for maximum gentleness
            double fadeIn = Math.min(1.0, time / 0.2); // 200ms fade-in
            double fadeOut = Math.min(1.0, (duration - time) / 0.2); // 200ms fade-out
            amplitude *= fadeIn * fadeOut;

            // Apply very gentle envelope
            double envelope = Math.sin(Math.PI * time / duration);
            amplitude *= envelope * envelope; // Square for extra smoothness

            // Convert to 16-bit PCM with very low volume
            short sample = (short) (amplitude * 32767 * volume * 0.15); // Only 15% of original volume

            for (int ch = 0; ch < channels; ch++) {
                int index = (i * channels + ch) * bytesPerSample;
                gentleData[index] = (byte) (sample & 0xFF);
                gentleData[index + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        log.info("Generated gentle tone with {} samples", numSamples);
        return gentleData;
    }
} 