# Audio Redaction Tool

A web-based tool for redacting sensitive information from audio files by replacing specified time intervals with replacement sounds or silence.

## Features

- **Multiple Sound Types**: Choose from different replacement sounds:
  - **Standard Beep**: Traditional beep sound
  - **Chime**: Musical chime-like sound with harmonics and natural decay
  - **Soft Tone**: Gentle tone with fade effects and reduced volume
  - **Gentle Tone**: Very quiet, barely audible tone for minimal disruption

- **Customizable Parameters**:
  - Frequency (100-10000 Hz)
  - Duration (0.1-5.0 seconds)
  - Volume (0.0-1.0)
  - Sound type selection

- **Flexible Redaction**: Support for multiple time intervals
- **Multiple Formats**: WAV and MP3 input support, WAV output
- **Web Interface**: User-friendly web UI for easy audio processing

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Running the Application

1. **Clone and navigate to the project directory:**
   ```bash
   cd hackathon-tezguard
   ```

2. **Build the application:**
   ```bash
   mvn clean install
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the web interface:**
   Open your browser and go to `http://localhost:8080`

## Usage

### Web Interface

1. **Upload Audio File**: Select a WAV or MP3 file to redact
2. **Set Redaction Intervals**: Enter time intervals in format `start1,end1;start2,end2` (e.g., `0,5;10,15`)
   - Leave empty for default (first 5 seconds)
3. **Choose Redaction Method**:
   - **Beep Sound**: Replace with customizable audio
   - **Silence**: Replace with complete silence
4. **Configure Beep Settings** (if using beep method):
   - **Frequency**: 100-10000 Hz (default: 1000 Hz)
   - **Duration**: 0.1-5.0 seconds (default: 0.5 seconds)
   - **Volume**: 0.0-1.0 (default: 0.3)
   - **Sound Type**: Standard Beep, Chime, Soft Tone, or Gentle Tone
5. **Process**: Click "Remove PII" to start redaction
6. **Download**: Download the redacted audio file

### API Usage

#### Redact Audio
```http
POST /api/audio/redact
Content-Type: multipart/form-data

Parameters:
- file: Audio file (WAV/MP3)
- intervals: Time intervals (optional, format: "start1,end1;start2,end2")
- replacementMethod: "beep" or "silence" (default: "beep")
- beepFrequency: Frequency in Hz (optional, default: 1000)
- beepDuration: Duration in seconds (optional, default: 0.5)
- beepVolume: Volume 0.0-1.0 (optional, default: 0.3)
- soundType: "beep", "chime", "soft", or "gentle" (default: "beep")
```

#### Download Redacted Audio
```http
GET /api/audio/download/{fileId}
```

#### Cleanup File
```http
DELETE /api/audio/cleanup/{fileId}
```

### Example API Request

```bash
curl -X POST http://localhost:8080/api/audio/redact \
  -F "file=@audio.wav" \
  -F "intervals=0,5;10,15" \
  -F "replacementMethod=beep" \
  -F "beepFrequency=1000" \
  -F "beepDuration=1.0" \
  -F "beepVolume=0.3" \
  -F "soundType=chime"
```

## Sound Types

### 🎵 Chime Sound
- **Description**: Musical chime-like sound with multiple harmonics
- **Characteristics**: 
  - Rich, musical tone using harmonic series (800Hz, 1200Hz, 1600Hz)
  - Natural exponential decay for authentic chime effect
  - 50ms fade-in/fade-out for smooth transitions
  - 30% of original volume for pleasant listening
- **Best for**: Professional environments, presentations, public recordings

### 🎧 Soft Tone
- **Description**: Gentle tone with warm harmonics and smooth envelopes
- **Characteristics**:
  - 30% lower frequency than standard beep for softer sound
  - Subtle second harmonic for warmth
  - 100ms fade-in/fade-out for gentle transitions
  - Sine wave envelope for natural sound curve
  - 40% of original volume
- **Best for**: Interviews, podcasts, educational content

### 🤫 Gentle Tone
- **Description**: Very quiet, barely audible replacement sound
- **Characteristics**:
  - 50% lower frequency for maximum gentleness
  - 200ms fade-in/fade-out for ultra-smooth transitions
  - Squared envelope for extra smoothness
  - Only 15% of original volume
- **Best for**: Sensitive content, quiet environments, minimal disruption

## Technical Details

- **Framework**: Spring Boot 3.5.3
- **Java Version**: 17
- **Audio Processing**: Java Sound API
- **File Upload Limit**: 512MB
- **Output Format**: WAV (PCM_SIGNED, 48kHz, 16-bit, stereo)

## Project Structure

```
src/
├── main/
│   ├── java/com/salesforce/
│   │   ├── App.java                    # Main application class
│   │   ├── redact/
│   │   │   ├── controller/
│   │   │   │   └── AudioRedactionController.java
│   │   │   └── service/
│   │   │       └── AudioRedactionService.java
│   │   └── transcribe/                 # Transcription utilities
│   └── resources/
│       ├── config/
│       │   └── application.properties
│       └── static/
│           └── index.html              # Web interface
```

## Building with Docker

```bash
# Build Docker image
docker build -t audio-redaction-tool .

# Run container
docker run -p 8080:8080 audio-redaction-tool
```

