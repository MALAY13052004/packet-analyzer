# PacketLab — Network Packet Analyzer & Live DPI Monitor

PacketLab is a Java 21 + Spring Boot network intelligence platform built around a Deep Packet Inspection (DPI) engine. It supports **two analysis modes**:

1. **Automatic Live Capture (default):** PacketLab detects the host OS and network interface, captures short complete PCAP windows, analyzes them automatically, and updates the web dashboard.
2. **Manual PCAP Analysis:** Drop any saved `.pcap` into the dashboard when you want to inspect an existing capture.

The project keeps the DPI pipeline from the original engine: PCAP reader → packet parser → SNI extraction → application classification → rule/alert inspection → statistics → flows.

## What the project does

- Automatic live network capture in small complete PCAP windows
- Manual `.pcap` upload and analysis
- Ethernet / IPv4 / TCP / UDP packet parsing
- TLS SNI/domain extraction where available
- Application classification
- Network flow tracking
- Rule-based security alerts
- TCP / UDP / other / dropped packet statistics
- 3D/glassmorphism dashboard with live polling
- Cross-platform Java application structure for Windows, macOS and Linux
- Automated Maven test/build workflow through GitHub Actions

## Architecture

```text
                 ┌──────────────────────┐
                 │      PacketLab       │
                 │   Spring Boot API   │
                 └──────────┬───────────┘
                            │
              ┌─────────────┴─────────────┐
              │                           │
      Automatic Live Capture        Manual PCAP Upload
              │                           │
      tcpdump / dumpcap              Multipart upload
              │                           │
        temporary PCAP                  PCAP
              │                           │
              └─────────────┬─────────────┘
                            ↓
                    PacketAnalysisService
                            ↓
                    Java DPI Engine
                            ↓
          ┌─────────────────┼─────────────────┐
          ↓                 ↓                 ↓
      Statistics          Flows             Alerts
          └─────────────────┼─────────────────┘
                            ↓
                     REST API / Dashboard
```

## Requirements

### All platforms

- Java 21+
- Maven 3.9+
- A modern browser

### Automatic live capture

- **macOS / Linux:** `tcpdump` with libpcap and sufficient packet-capture permissions.
- **Windows:** Npcap plus `dumpcap` (normally installed with Wireshark).

If live capture is unavailable, the application reports the reason through `/api/live`. **Manual PCAP analysis remains available.**

> A cross-platform Java application does not remove the operating-system requirement for raw packet capture. Capture permissions/drivers are controlled by the OS.

## Run PacketLab

### macOS

Double-click:

```text
scripts/run-macos.command
```

The script checks Java and Maven, then starts Spring Boot. Live capture starts automatically when capture permissions are available.

### Linux

```bash
chmod +x scripts/run-linux.sh
./scripts/run-linux.sh
```

### Windows

Double-click:

```text
scripts/run-windows.bat
```

or run the PowerShell launcher:

```powershell
./scripts/run-windows.ps1
```

Open `http://localhost:8080` in your browser.

## Automatic live mode

Live mode is enabled by default. The service:

1. Detects the operating system.
2. Detects the default network interface unless one is configured.
3. Starts `tcpdump` on macOS/Linux or `dumpcap` on Windows.
4. Captures a small complete PCAP window (40 packets by default).
5. Moves the completed capture to the runtime PCAP area.
6. Sends it to the Java DPI engine.
7. Publishes the latest result through `/api/live`.
8. Repeats continuously.

The generated PCAP files are runtime artifacts and are ignored by Git.

## Manual PCAP mode

The dashboard still provides an optional PCAP drop area. It sends the selected file to:

```text
POST /api/analyze-pcap
```

This is useful for previously recorded traffic, demonstrations, test datasets, and captures from other tools.

## Configuration

`src/main/resources/application.properties`:

```properties
packetlab.capture.enabled=true
packetlab.capture.packets-per-window=40
packetlab.capture.interface=auto
packetlab.capture.tool=auto
```

Examples:

```properties
packetlab.capture.interface=en0
```

or on Windows, a `dumpcap` interface number:

```properties
packetlab.capture.interface=1
```

You can also set a custom capture executable with:

```properties
packetlab.capture.tool=/path/to/tcpdump
```

## API

| Endpoint | Purpose |
|---|---|
| `GET /api/health` | Backend health check |
| `GET /api/live` | Live capture status + latest analysis |
| `GET /api/analyze` | DPI engine readiness |
| `POST /api/analyze-pcap` | Analyze a manually uploaded PCAP |

## Project structure

```text
PacketLab/
├── README.md
├── START_HERE.md
├── pom.xml
├── .gitignore
├── .github/workflows/build.yml
├── scripts/
│   ├── run-macos.command
│   ├── run-linux.sh
│   ├── run-windows.bat
│   └── run-windows.ps1
├── src/
│   ├── main/java/com/packetanalyzer/
│   │   ├── engine/
│   │   ├── model/
│   │   ├── parser/
│   │   ├── pcap/
│   │   ├── service/
│   │   └── PacketController.java
│   ├── main/resources/
│   │   ├── application.properties
│   │   └── static/
│   └── test/java/
└── frontend/                 # optional Vite source
```

## Relation to the original DPI engine

The original project describes a Java DPI implementation with packet parsing, SNI extraction, application identification, blocking/rules, flow tracking, statistics and automated tests. PacketLab keeps that core pipeline while adding the Spring Boot API, live capture service and dashboard. The original quick-start documentation uses Java 11+, Maven and libpcap; this version standardizes the application build on Java 21 and makes live capture OS-aware. fileciteturn1file0L13-L21 fileciteturn1file0L142-L176

## Test

```bash
mvn test
```

The repository also includes a GitHub Actions workflow that builds and tests the project with Java 21.

## Privacy

Network captures may contain sensitive traffic metadata. Do not commit `.pcap`, `.pcapng`, or the `pcaps/` runtime directory to GitHub.

## Resume description

**PacketLab — Network Packet Analyzer & Live DPI Monitor**  
*Java 21, Spring Boot, PCAP, REST API, HTML/CSS/JavaScript*

- Developed a Spring Boot network packet analyzer with automatic live packet capture and offline PCAP analysis.
- Implemented packet parsing, application classification, network flow tracking, traffic statistics and security alerts.
- Built a real-time 3D dashboard that consumes live DPI results through REST APIs.
- Designed OS-aware capture support for macOS/Linux (`tcpdump`) and Windows (`dumpcap`/Npcap).
- Added automated unit/build verification with Maven and GitHub Actions.
