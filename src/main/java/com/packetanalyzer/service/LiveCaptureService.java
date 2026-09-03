package com.packetanalyzer.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cross-platform live capture service. Short PCAP windows are captured by
 * tcpdump on macOS/Linux and dumpcap on Windows, then fed to the existing DPI engine.
 *
 * On container platforms such as Render, capture is performed inside the container's
 * own network namespace. It does not capture arbitrary traffic from visitors' devices.
 */
@Service
public class LiveCaptureService {

    private final PacketAnalysisService analysisService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${packetlab.capture.enabled:true}")
    private boolean captureEnabled;

    @Value("${packetlab.capture.packets-per-window:40}")
    private int packetsPerWindow;

    @Value("${packetlab.capture.interface:auto}")
    private String configuredInterface;

    @Value("${packetlab.capture.tool:auto}")
    private String configuredTool;

    @Value("${packetlab.capture.environment:auto}")
    private String captureEnvironment;

    private volatile Process captureProcess;
    private volatile Map<String, Object> latestResult = new LinkedHashMap<>();
    private volatile String status = "STARTING";
    private volatile String message = "Preparing live capture…";
    private volatile String interfaceName = "unknown";
    private volatile String captureTool = "unknown";
    private volatile Instant lastCaptureAt;

    private final Path captureDir = Path.of("pcaps");
    private final Path workingFile = captureDir.resolve("live-current.pcap");
    private final Path readyFile = captureDir.resolve("live-latest.pcap");

    public LiveCaptureService(PacketAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostConstruct
    public void start() {
        if (!captureEnabled) {
            status = "DISABLED";
            message = "Automatic live capture is disabled in this deployment. Manual PCAP analysis is available.";
            return;
        }
        if (!running.compareAndSet(false, true)) return;

        Thread worker = new Thread(this::captureLoop, "packetlab-live-capture");
        worker.setDaemon(true);
        worker.start();
    }

    private void captureLoop() {
        try {
            Files.createDirectories(captureDir);
            interfaceName = resolveInterface();
            captureTool = resolveTool();

            if (interfaceName == null || interfaceName.isBlank()) {
                fail("No active network interface was found.");
                return;
            }
            if (captureTool == null || captureTool.isBlank()) {
                fail("No supported packet capture tool was found. Install tcpdump (macOS/Linux) or dumpcap (Windows).");
                return;
            }

            status = "LIVE";
            message = "Capturing traffic on " + interfaceName + " using " + captureTool;

            while (running.get()) {
                Files.deleteIfExists(workingFile);

                ProcessBuilder builder = buildCaptureProcess();
                builder.redirectErrorStream(true);
                captureProcess = builder.start();

                String output = readProcessOutput(captureProcess);
                int exitCode = captureProcess.waitFor();
                captureProcess = null;

                if (!running.get()) break;

                if (exitCode != 0) {
                    if (isPermissionDenied(output)) {
                        markCaptureUnavailable();
                        break;
                    }
                    fail(captureError(output));
                    Thread.sleep(3000);
                    continue;
                }

                if (Files.exists(workingFile) && Files.size(workingFile) >= 24) {
                    moveCaptureToReadyFile();
                    latestResult = analysisService.analyzePcap(readyFile.toString());
                    lastCaptureAt = Instant.now();
                    status = "LIVE";
                    message = "Live traffic analyzed on " + interfaceName + " using " + captureTool;
                } else {
                    status = "LIVE";
                    message = "Capture window completed with no packets on " + interfaceName;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail("Live capture error: " + e.getMessage());
        } finally {
            destroyCaptureProcess();
            running.set(false);
            if (!"ERROR".equals(status) && !"DISABLED".equals(status) && !"CLOUD_UNAVAILABLE".equals(status) && !"UNAVAILABLE".equals(status)) status = "STOPPED";
        }
    }

    private ProcessBuilder buildCaptureProcess() {
        String count = String.valueOf(Math.max(1, packetsPerWindow));
        String os = osFamily();

        if ("dumpcap".equalsIgnoreCase(captureTool)) {
            return new ProcessBuilder("dumpcap", "-i", interfaceName, "-c", count, "-w", workingFile.toString());
        }

        if ("linux".equals(os)) {
            // Containers normally run as root and can use tcpdump directly. Avoid sudo
            // because cloud containers do not provide an interactive sudo credential flow.
            return new ProcessBuilder("tcpdump", "-i", interfaceName, "-c", count, "-w", workingFile.toString(), "-U");
        }

        if ("macos".equals(os)) {
            return new ProcessBuilder("sudo", "-n", "tcpdump", "-i", interfaceName, "-c", count,
                    "-w", workingFile.toString(), "-U");
        }

        return new ProcessBuilder("tcpdump", "-i", interfaceName, "-c", count, "-w", workingFile.toString(), "-U");
    }

    private String resolveTool() throws Exception {
        if (!"auto".equalsIgnoreCase(configuredTool) && !configuredTool.isBlank()) {
            return configuredTool.trim();
        }
        String os = osFamily();
        if ("windows".equals(os)) return commandExists("dumpcap") ? "dumpcap" : null;
        return commandExists("tcpdump") ? "tcpdump" : null;
    }

    private String resolveInterface() throws Exception {
        if (configuredInterface != null && !configuredInterface.isBlank()
                && !"auto".equalsIgnoreCase(configuredInterface.trim())) {
            return configuredInterface.trim();
        }

        String os = osFamily();
        if ("linux".equals(os)) return detectLinuxInterface();
        if ("macos".equals(os)) return detectMacInterface();
        if ("windows".equals(os)) return detectWindowsInterface();
        return null;
    }

    private String detectLinuxInterface() throws Exception {
        String result = runAndFindFirstLine("ip", "route", "show", "default");
        if (result != null) {
            String[] parts = result.trim().split("\\s+");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("dev".equals(parts[i])) return parts[i + 1];
            }
        }

        Path route = Path.of("/proc/net/route");
        if (Files.exists(route)) {
            for (String line : Files.readAllLines(route)) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 1 && "00000000".equals(parts[1]) && !"Iface".equalsIgnoreCase(parts[0])) {
                    return parts[0];
                }
            }
        }
        return null;
    }

    private String detectMacInterface() throws Exception {
        Process process = new ProcessBuilder("/sbin/route", "get", "default")
                .redirectErrorStream(true).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("interface:")) {
                    process.waitFor();
                    return trimmed.substring("interface:".length()).trim();
                }
            }
        }
        process.waitFor();
        return null;
    }

    private String detectWindowsInterface() throws Exception {
        String output = runCommand("dumpcap", "-D");
        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            int dot = trimmed.indexOf('.');
            if (dot > 0) {
                String index = trimmed.substring(0, dot).trim();
                if (index.matches("\\d+")) return index;
            }
        }
        return "1";
    }

    private String runAndFindFirstLine(String... command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    process.waitFor();
                    return line;
                }
            }
        }
        process.waitFor();
        return null;
    }

    private String runCommand(String... command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = readProcessOutput(process);
        process.waitFor();
        return output;
    }

    private boolean commandExists(String command) {
        try {
            Process process = new ProcessBuilder("sh", "-c", "command -v " + command)
                    .redirectErrorStream(true).start();
            int exit = process.waitFor();
            return exit == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String osFamily() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "windows";
        if (os.contains("mac") || os.contains("darwin")) return "macos";
        if (os.contains("nux") || os.contains("nix")) return "linux";
        return "unknown";
    }

    private void moveCaptureToReadyFile() throws Exception {
        try {
            Files.move(workingFile, readyFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception atomicMoveFailure) {
            Files.move(workingFile, readyFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String readProcessOutput(Process process) throws Exception {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() < 4000) output.append(line).append('\n');
            }
        }
        return output.toString();
    }

    private boolean isPermissionDenied(String output) {
        String cleaned = clean(output).toLowerCase(Locale.ROOT);
        return cleaned.contains("permission denied")
                || cleaned.contains("operation not permitted")
                || cleaned.contains("cap_net_raw")
                || cleaned.contains("you don't have permission to perform this capture");
    }

    private boolean isCloudEnvironment() {
        return "cloud".equalsIgnoreCase(captureEnvironment)
                || "true".equalsIgnoreCase(System.getenv("RENDER"));
    }

    private void markCaptureUnavailable() {
        status = isCloudEnvironment() ? "CLOUD_UNAVAILABLE" : "UNAVAILABLE";
        if ("CLOUD_UNAVAILABLE".equals(status)) {
            message = "Live packet capture is unavailable in this cloud container because raw packet capture privileges (CAP_NET_RAW) are not granted. Use the PCAP upload below for analysis.";
        } else {
            message = "Live packet capture is unavailable in this runtime because packet-capture privileges are not available. Use the PCAP upload below for analysis.";
        }
    }

    private String captureError(String output) {
        String cleaned = clean(output);
        if (cleaned.isBlank()) return captureTool + " could not start capturing on " + interfaceName + ".";
        if (isPermissionDenied(cleaned)) {
            return "Packet capture permission was denied for " + interfaceName + ". The host/container must grant packet-capture privileges.";
        }
        if (cleaned.toLowerCase(Locale.ROOT).contains("password is required")) {
            return "macOS packet capture requires administrator authorization. Run the local launcher that grants tcpdump permission.";
        }
        return captureTool + " could not start: " + cleaned;
    }

    private String clean(String value) {
        return value == null ? "unknown error" : value.replaceAll("\\s+", " ").trim();
    }

    private void fail(String text) {
        status = "ERROR";
        message = text;
    }

    private void destroyCaptureProcess() {
        Process process = captureProcess;
        if (process != null) {
            process.destroy();
            captureProcess = null;
        }
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        destroyCaptureProcess();
    }

    public Map<String, Object> getStatusResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        response.put("message", message);
        response.put("interface", interfaceName);
        response.put("tool", captureTool);
        response.put("lastCaptureAt", lastCaptureAt == null ? null : lastCaptureAt.toString());
        response.put("capturing", running.get());
        response.put("environment", isCloudEnvironment() ? "cloud" : captureEnvironment);
        response.put("captureAvailable", "LIVE".equals(status));
        response.put("fallback", "PCAP_UPLOAD");
        response.put("fallbackMessage", "Upload a .pcap file below to run the same Java DPI analysis without live capture.");
        response.put("data", latestResult);
        return response;
    }
}
