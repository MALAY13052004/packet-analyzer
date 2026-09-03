package com.packetanalyzer.service;

import com.packetanalyzer.engine.DPIEngine;
import com.packetanalyzer.model.RawPacket;
import com.packetanalyzer.pcap.PcapReader;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PacketAnalysisService {

    private final PcapReader pcapReader = new PcapReader();

    public String analyze() {
        return "DPI Engine is ready for packet analysis!";
    }

    /**
     * Each analysis gets its own DPIEngine. This keeps live capture and manual
     * uploads isolated when they happen at the same time.
     */
    public Map<String, Object> analyzePcap(String filePath) throws Exception {
        Path path = Path.of(filePath);
        List<RawPacket> packets = pcapReader.read(path);
        DPIEngine dpiEngine = new DPIEngine();
        dpiEngine.processAll(packets);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPackets", dpiEngine.getStatistics().getTotalPackets());
        result.put("totalBytes", dpiEngine.getStatistics().getTotalBytes());
        result.put("tcpPackets", dpiEngine.getStatistics().getTcpPackets());
        result.put("udpPackets", dpiEngine.getStatistics().getUdpPackets());
        result.put("otherPackets", dpiEngine.getStatistics().getOtherPackets());
        result.put("droppedPackets", dpiEngine.getStatistics().getDroppedPackets());
        result.put("applicationCounts", dpiEngine.getStatistics().getApplicationCounts());
        result.put("flowCount", dpiEngine.getFlowTracker().getFlowCount());
        result.put("flows", dpiEngine.getFlowTracker().getFlows());
        result.put("alertCount", dpiEngine.getAlertManager().getAlertCount());
        result.put("alerts", dpiEngine.getAlertManager().getAlerts());
        return result;
    }
}
