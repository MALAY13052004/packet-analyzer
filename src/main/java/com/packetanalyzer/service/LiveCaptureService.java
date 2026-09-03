package com.packetanalyzer.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Captures short, complete PCAP chunks with macOS tcpdump and continuously
 * feeds them into the existing DPI engine. The launcher caches sudo
 * credentials once so no command needs to be typed by the user.
 */
@Service
public class LiveCaptureService {

    private final PacketAnalysisService analysisService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Process captureProcess;
    private volatile Map<String, Object> latestResult = new LinkedHashMap<>();
    private volatile String status = "STARTING";
    private volatile String message = "Preparing live capture…";
    private volatile String interfaceName = "unknown";
    private volatile Instant lastCaptureAt;

    private final Path captureDir = Path.of("pcaps");
    private final Path workingFile = captureDir.resolve("live-current.pcap");
    private final Path readyFile = captureDir.resolve("live-latest.pcap");

    public LiveCaptureService(PacketAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostConstruct
    public void start() {
        if (!running.compareAndSet(false, true)) return;

        Thread worker = new Thread(this::captureLoop, "packetlab-live-capture");
        worker.setDaemon(true);
        worker.start();
    }

    private void captureLoop() {
        try {
            Files.createDirectories(captureDir);
            interfaceName = detectDefaultInterface();

            if (interfaceName == null || interfaceName.isBlank()) {
                fail("No active network interface was found.");
                return;
            }

            status = "LIVE";
            message = "Capturing traffic on " + interfaceName;

            while (running.get()) {
                Files.deleteIfExists(workingFile);

                ProcessBuilder builder = new ProcessBuilder(
                        "sudo", "-n", "tcpdump",
                        "-i", interfaceName,
                        "-c", "40",
                        "-w", workingFile.toString(),
                        "-U"
                );
                builder.redirectErrorStream(true);
                captureProcess = builder.start();

                String output = readProcessOutput(captureProcess);
                int exitCode = captureProcess.waitFor();
                captureProcess = null;

                if (!running.get()) break;

                if (exitCode != 0) {
                    if (output.contains("password is required") || output.contains("a password is required")) {
                        fail("macOS permission is required for packet capture. Restart PacketLab.app and allow the administrator prompt.");
                    } else {
                        fail("tcpdump could not start: " + clean(output));
                    }
                    Thread.sleep(3000);
                    continue;
                }

                if (Files.exists(workingFile) && Files.size(workingFile) >= 24) {
                    Files.move(workingFile, readyFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);

                    latestResult = analysisService.analyzePcap(readyFile.toString());
                    lastCaptureAt = Instant.now();
                    status = "LIVE";
                    message = "Live traffic analyzed on " + interfaceName;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail("Live capture error: " + e.getMessage());
        } finally {
            destroyCaptureProcess();
            running.set(false);
            if (!"ERROR".equals(status)) status = "STOPPED";
        }
    }

    private String detectDefaultInterface() throws Exception {
        Process process = new ProcessBuilder("/sbin/route", "get", "default")
                .redirectErrorStream(true)
                .start();

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
        response.put("lastCaptureAt", lastCaptureAt == null ? null : lastCaptureAt.toString());
        response.put("capturing", running.get());
        response.put("data", latestResult);
        return response;
    }
}
