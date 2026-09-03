package com.packetanalyzer.engine;

import com.packetanalyzer.model.ParsedPacket;

import java.util.HashSet;
import java.util.Set;

public class RuleManager {

    private final Set<String> blockedIps = new HashSet<>();
    private final Set<Integer> blockedPorts = new HashSet<>();

    public void blockIp(String ip) {
        if (ip != null && !ip.isBlank()) {
            blockedIps.add(ip);
        }
    }

    public void unblockIp(String ip) {
        if (ip != null) {
            blockedIps.remove(ip);
        }
    }

    public void blockPort(int port) {
        if (port >= 0 && port <= 65535) {
            blockedPorts.add(port);
        }
    }

    public void unblockPort(int port) {
        blockedPorts.remove(port);
    }

    public boolean isIpBlocked(String ip) {
        return ip != null && blockedIps.contains(ip);
    }

    public boolean isPortBlocked(int port) {
        return blockedPorts.contains(port);
    }

    public boolean shouldDrop(ParsedPacket packet) {

        if (packet == null) {
            return false;
        }

        if (packet.getIpv4Header() != null) {

            String sourceIp = packet.getIpv4Header().getSourceIp();
            String destinationIp = packet.getIpv4Header().getDestinationIp();

            if (isIpBlocked(sourceIp) || isIpBlocked(destinationIp)) {
                return true;
            }
        }

        if (packet.getTcpHeader() != null) {

            int sourcePort = packet.getTcpHeader().getSourcePort();
            int destinationPort = packet.getTcpHeader().getDestinationPort();

            if (isPortBlocked(sourcePort) ||
                    isPortBlocked(destinationPort)) {
                return true;
            }
        }

        if (packet.getUdpHeader() != null) {

            int sourcePort = packet.getUdpHeader().getSourcePort();
            int destinationPort = packet.getUdpHeader().getDestinationPort();

            if (isPortBlocked(sourcePort) ||
                    isPortBlocked(destinationPort)) {
                return true;
            }
        }

        return false;
    }

    public Set<String> getBlockedIps() {
        return Set.copyOf(blockedIps);
    }

    public Set<Integer> getBlockedPorts() {
        return Set.copyOf(blockedPorts);
    }
}
