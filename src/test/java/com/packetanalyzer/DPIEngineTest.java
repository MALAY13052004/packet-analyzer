package com.packetanalyzer;

import com.packetanalyzer.engine.AlertManager;
import com.packetanalyzer.engine.DPIEngine;
import com.packetanalyzer.engine.FlowTracker;
import com.packetanalyzer.model.AppType;
import com.packetanalyzer.model.EthernetHeader;
import com.packetanalyzer.model.IPv4Header;
import com.packetanalyzer.model.ParsedPacket;
import com.packetanalyzer.model.RawPacket;
import com.packetanalyzer.model.TcpHeader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DPIEngineTest {

    @Test
    void engineShouldInitialize() {

        DPIEngine engine = new DPIEngine();

        assertNotNull(engine);
        assertNotNull(engine.getPacketParser());
        assertNotNull(engine.getRuleManager());
        assertNotNull(engine.getFlowTracker());
        assertNotNull(engine.getStatistics());
        assertNotNull(engine.getAlertManager());
    }

    @Test
    void engineShouldIgnoreNullPacket() {

        DPIEngine engine = new DPIEngine();

        engine.process(null);

        assertEquals(
                0,
                engine.getStatistics().getTotalPackets()
        );
    }

    @Test
    void engineShouldResetStatistics() {

        DPIEngine engine = new DPIEngine();

        engine.reset();

        assertEquals(
                0,
                engine.getStatistics().getTotalPackets()
        );

        assertEquals(
                0,
                engine.getStatistics().getTotalBytes()
        );

        assertEquals(
                0,
                engine.getStatistics().getDroppedPackets()
        );

        assertEquals(
                0,
                engine.getAlertManager().getAlertCount()
        );

        assertEquals(
                0,
                engine.getFlowTracker().getFlowCount()
        );
    }

    @Test
    void rawPacketShouldStoreData() {

        byte[] data = {
                0x01,
                0x02,
                0x03,
                0x04
        };

        RawPacket packet =
                new RawPacket(
                        100,
                        500,
                        data
                );

        assertEquals(100, packet.getTimestampSeconds());
        assertEquals(500, packet.getTimestampMicros());
        assertEquals(4, packet.getLength());
    }

    @Test
    void privateToPublicTrafficShouldNotCreateExternalTrafficAlert() {

        AlertManager alertManager = new AlertManager();

        ParsedPacket packet = createTcpPacket(
                "192.168.1.100",
                "8.8.8.8",
                50000,
                443,
                AppType.HTTPS
        );

        alertManager.inspect(packet);

        assertEquals(
                0,
                alertManager.getAlertCount()
        );
    }

    @Test
    void telnetShouldBeHighSeverity() {

        AlertManager alertManager = new AlertManager();

        ParsedPacket packet = createTcpPacket(
                "192.168.1.100",
                "10.0.0.20",
                50000,
                23,
                AppType.UNKNOWN
        );

        alertManager.inspect(packet);

        assertEquals(
                1,
                alertManager.getAlertCount()
        );

        AlertManager.Alert alert =
                alertManager.getAlerts().get(0);

        assertEquals(
                "TELNET",
                alert.getType()
        );

        assertEquals(
                AlertManager.Severity.HIGH,
                alert.getSeverity()
        );
    }

    @Test
    void unknownProtocolShouldBeLowSeverity() {

        AlertManager alertManager = new AlertManager();

        ParsedPacket packet = createUnknownPacket(
                "192.168.1.100",
                "10.0.0.20"
        );

        alertManager.inspect(packet);

        assertEquals(
                1,
                alertManager.getAlertCount()
        );

        AlertManager.Alert alert =
                alertManager.getAlerts().get(0);

        assertEquals(
                "UNKNOWN_PROTOCOL",
                alert.getType()
        );

        assertEquals(
                AlertManager.Severity.LOW,
                alert.getSeverity()
        );
    }

    @Test
    void reverseDirectionsShouldBeOneFlow() {

        FlowTracker flowTracker = new FlowTracker();

        ParsedPacket outgoing = createTcpPacket(
                "192.168.1.100",
                "8.8.8.8",
                50000,
                443,
                AppType.HTTPS
        );

        ParsedPacket incoming = createTcpPacket(
                "8.8.8.8",
                "192.168.1.100",
                443,
                50000,
                AppType.HTTPS
        );

        flowTracker.track(outgoing);
        flowTracker.track(incoming);

        assertEquals(
                1,
                flowTracker.getFlowCount()
        );

        FlowTracker.Flow flow =
                flowTracker.getFlows().values().iterator().next();

        assertEquals(
                2,
                flow.getPacketCount()
        );

        assertEquals(
                20,
                flow.getByteCount()
        );

        assertTrue(
                flowTracker.getFlows().keySet().iterator().next().contains("<->")
        );
    }

    @Test
    void differentPortsShouldRemainDifferentFlows() {

        FlowTracker flowTracker = new FlowTracker();

        ParsedPacket first = createTcpPacket(
                "192.168.1.100",
                "8.8.8.8",
                50000,
                443,
                AppType.HTTPS
        );

        ParsedPacket second = createTcpPacket(
                "192.168.1.100",
                "8.8.8.8",
                50001,
                443,
                AppType.HTTPS
        );

        flowTracker.track(first);
        flowTracker.track(second);

        assertEquals(
                2,
                flowTracker.getFlowCount()
        );
    }

    private ParsedPacket createTcpPacket(
            String sourceIp,
            String destinationIp,
            int sourcePort,
            int destinationPort,
            AppType appType) {

        RawPacket rawPacket =
                new RawPacket(
                        100,
                        500,
                        new byte[10]
                );

        EthernetHeader ethernetHeader =
                new EthernetHeader(
                        "00:11:22:33:44:55",
                        "66:77:88:99:AA:BB",
                        0x0800
                );

        IPv4Header ipv4Header =
                new IPv4Header(
                        sourceIp,
                        destinationIp,
                        6,
                        64
                );

        TcpHeader tcpHeader =
                new TcpHeader(
                        sourcePort,
                        destinationPort,
                        0L,
                        0L,
                        0,
                        65535
                );

        return new ParsedPacket(
                rawPacket,
                ethernetHeader,
                ipv4Header,
                tcpHeader,
                null,
                appType,
                null
        );
    }

    private ParsedPacket createUnknownPacket(
            String sourceIp,
            String destinationIp) {

        RawPacket rawPacket =
                new RawPacket(
                        100,
                        500,
                        new byte[10]
                );

        EthernetHeader ethernetHeader =
                new EthernetHeader(
                        "00:11:22:33:44:55",
                        "66:77:88:99:AA:BB",
                        0x0800
                );

        IPv4Header ipv4Header =
                new IPv4Header(
                        sourceIp,
                        destinationIp,
                        99,
                        64
                );

        return new ParsedPacket(
                rawPacket,
                ethernetHeader,
                ipv4Header,
                null,
                null,
                AppType.UNKNOWN,
                null
        );
    }
}