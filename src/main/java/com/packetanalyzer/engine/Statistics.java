package com.packetanalyzer.engine;

import com.packetanalyzer.model.AppType;
import com.packetanalyzer.model.ParsedPacket;

import java.util.EnumMap;
import java.util.Map;

public class Statistics {

    private long totalPackets;
    private long totalBytes;
    private long tcpPackets;
    private long udpPackets;
    private long otherPackets;
    private long droppedPackets;

    private final Map<AppType, Long> applicationCounts =
            new EnumMap<>(AppType.class);

    public void record(ParsedPacket packet) {

        if (packet == null || packet.getRawPacket() == null) {
            return;
        }

        totalPackets++;
        totalBytes += packet.getRawPacket().getLength();

        if (packet.isTcp()) {
            tcpPackets++;
        } else if (packet.isUdp()) {
            udpPackets++;
        } else {
            otherPackets++;
        }

        AppType appType = packet.getAppType();

        if (appType != null) {
            applicationCounts.merge(appType, 1L, Long::sum);
        }
    }

    public void recordDroppedPacket() {
        droppedPackets++;
    }

    public long getTotalPackets() {
        return totalPackets;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getTcpPackets() {
        return tcpPackets;
    }

    public long getUdpPackets() {
        return udpPackets;
    }

    public long getOtherPackets() {
        return otherPackets;
    }

    public long getDroppedPackets() {
        return droppedPackets;
    }

    public Map<AppType, Long> getApplicationCounts() {
        return Map.copyOf(applicationCounts);
    }

    public void reset() {

        totalPackets = 0;
        totalBytes = 0;
        tcpPackets = 0;
        udpPackets = 0;
        otherPackets = 0;
        droppedPackets = 0;

        applicationCounts.clear();
    }

    @Override
    public String toString() {

        return "Statistics{" +
                "totalPackets=" + totalPackets +
                ", totalBytes=" + totalBytes +
                ", tcpPackets=" + tcpPackets +
                ", udpPackets=" + udpPackets +
                ", otherPackets=" + otherPackets +
                ", droppedPackets=" + droppedPackets +
                ", applicationCounts=" + applicationCounts +
                '}';
    }
}
