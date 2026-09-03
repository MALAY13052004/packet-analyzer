package com.packetanalyzer;

import com.packetanalyzer.engine.DPIEngine;
import com.packetanalyzer.model.RawPacket;
import com.packetanalyzer.pcap.PcapReader;

import java.nio.file.Path;
import java.util.List;

public class DPIApp {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage: java -jar packet-analyzer.jar <pcap-file>");
            return;
        }

        Path pcapFile = Path.of(args[0]);

        try {

            System.out.println("======================================");
            System.out.println("       Java DPI Packet Analyzer");
            System.out.println("======================================");

            System.out.println("Reading PCAP: " + pcapFile);

            PcapReader pcapReader = new PcapReader();

            List<RawPacket> packets = pcapReader.read(pcapFile);

            System.out.println("Packets loaded: " + packets.size());

            DPIEngine engine = new DPIEngine();

            engine.processAll(packets);

            System.out.println();
            System.out.println("========== ANALYSIS RESULTS ==========");

            System.out.println(
                    "Total packets: "
                            + engine.getStatistics().getTotalPackets()
            );

            System.out.println(
                    "Total bytes: "
                            + engine.getStatistics().getTotalBytes()
            );

            System.out.println(
                    "TCP packets: "
                            + engine.getStatistics().getTcpPackets()
            );

            System.out.println(
                    "UDP packets: "
                            + engine.getStatistics().getUdpPackets()
            );

            System.out.println(
                    "Other packets: "
                            + engine.getStatistics().getOtherPackets()
            );

            System.out.println(
                    "Dropped packets: "
                            + engine.getStatistics().getDroppedPackets()
            );

            System.out.println(
                    "Flows detected: "
                            + engine.getFlowTracker().getFlowCount()
            );

            System.out.println(
                    "Alerts: "
                            + engine.getAlertManager().getAlertCount()
            );

            System.out.println();
            System.out.println("Application statistics:");

            engine.getStatistics()
                    .getApplicationCounts()
                    .forEach((type, count) ->
                            System.out.println(
                                    "  " + type + ": " + count
                            )
                    );

            System.out.println();
            System.out.println("======================================");

        } catch (Exception e) {

            System.err.println(
                    "Error while analyzing PCAP: "
                            + e.getMessage()
            );
        }
    }
}

