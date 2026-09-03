package com.packetanalyzer.engine;

import com.packetanalyzer.model.ParsedPacket;

import java.util.HashMap;
import java.util.Map;

public class FlowTracker {

    private final Map<String, Flow> flows = new HashMap<>();

    public void track(ParsedPacket packet) {

        if (packet == null || packet.getIpv4Header() == null) {
            return;
        }

        String sourceIp = packet.getIpv4Header().getSourceIp();
        String destinationIp = packet.getIpv4Header().getDestinationIp();

        int sourcePort = 0;
        int destinationPort = 0;
        String protocol = "OTHER";

        if (packet.getTcpHeader() != null) {

            sourcePort = packet.getTcpHeader().getSourcePort();
            destinationPort = packet.getTcpHeader().getDestinationPort();
            protocol = "TCP";

        } else if (packet.getUdpHeader() != null) {

            sourcePort = packet.getUdpHeader().getSourcePort();
            destinationPort = packet.getUdpHeader().getDestinationPort();
            protocol = "UDP";
        }

        Endpoint first = new Endpoint(sourceIp, sourcePort);
        Endpoint second = new Endpoint(destinationIp, destinationPort);

        if (first.compareTo(second) > 0) {
            Endpoint temp = first;
            first = second;
            second = temp;
        }

        String flowKey = createFlowKey(
                first.ip,
                first.port,
                second.ip,
                second.port,
                protocol
        );

        Flow flow = flows.get(flowKey);

        if (flow == null) {

            flow = new Flow(
                    first.ip,
                    first.port,
                    second.ip,
                    second.port,
                    protocol
            );

            flows.put(flowKey, flow);
        }

        flow.addPacket(packet.getRawPacket().getLength());
    }

    private String createFlowKey(
            String sourceIp,
            int sourcePort,
            String destinationIp,
            int destinationPort,
            String protocol) {

        return sourceIp + ":" + sourcePort
                + " <-> "
                + destinationIp + ":" + destinationPort
                + " (" + protocol + ")";
    }

    public Map<String, Flow> getFlows() {
        return Map.copyOf(flows);
    }

    public int getFlowCount() {
        return flows.size();
    }

    public void reset() {
        flows.clear();
    }

    private static class Endpoint implements Comparable<Endpoint> {

        private final String ip;
        private final int port;

        Endpoint(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public int compareTo(Endpoint other) {

            int ipComparison = this.ip.compareTo(other.ip);

            if (ipComparison != 0) {
                return ipComparison;
            }

            return Integer.compare(this.port, other.port);
        }
    }

    public static class Flow {

        private final String sourceIp;
        private final int sourcePort;
        private final String destinationIp;
        private final int destinationPort;
        private final String protocol;

        private long packetCount;
        private long byteCount;

        public Flow(
                String sourceIp,
                int sourcePort,
                String destinationIp,
                int destinationPort,
                String protocol) {

            this.sourceIp = sourceIp;
            this.sourcePort = sourcePort;
            this.destinationIp = destinationIp;
            this.destinationPort = destinationPort;
            this.protocol = protocol;
        }

        public void addPacket(int packetLength) {
            packetCount++;
            byteCount += packetLength;
        }

        public String getSourceIp() {
            return sourceIp;
        }

        public int getSourcePort() {
            return sourcePort;
        }

        public String getDestinationIp() {
            return destinationIp;
        }

        public int getDestinationPort() {
            return destinationPort;
        }

        public String getProtocol() {
            return protocol;
        }

        public long getPacketCount() {
            return packetCount;
        }

        public long getByteCount() {
            return byteCount;
        }
    }
}
