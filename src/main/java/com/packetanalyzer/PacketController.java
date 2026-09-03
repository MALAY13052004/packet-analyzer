package com.packetanalyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.packetanalyzer.service.PacketAnalysisService;
import com.packetanalyzer.service.LiveCaptureService;

@RestController
public class PacketController {

    private final PacketAnalysisService packetAnalysisService;
    private final LiveCaptureService liveCaptureService;

    public PacketController(PacketAnalysisService packetAnalysisService, LiveCaptureService liveCaptureService) {
        this.packetAnalysisService = packetAnalysisService;
        this.liveCaptureService = liveCaptureService;
    }

    @GetMapping("/api/health")
    public String health() {
        return "Packet Analyzer API is running!";
    }

    @GetMapping("/api/live")
    public ResponseEntity<?> live() {
        return ResponseEntity.ok(liveCaptureService.getStatusResponse());
    }

    @GetMapping("/api/analyze")
    public String analyze() {
        return packetAnalysisService.analyze();
    }

    @PostMapping("/api/analyze-pcap")
    public ResponseEntity<?> analyzePcap(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please upload a PCAP file.");
        }

        Path tempFile = null;

        try {
            String originalFilename = file.getOriginalFilename();

            if (originalFilename == null
                    || !originalFilename.toLowerCase().endsWith(".pcap")) {

                return ResponseEntity.badRequest()
                        .body("Only .pcap files are supported.");
            }

            tempFile = Files.createTempFile(
                    "packet-analyzer-",
                    ".pcap"
            );

            file.transferTo(tempFile);

            Map<String, Object> result =
                    packetAnalysisService.analyzePcap(
                            tempFile.toString()
                    );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("PCAP analysis failed: " + e.getMessage());

        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
        }
    }
}