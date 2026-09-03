package com.packetanalyzer.engine;

import com.packetanalyzer.model.ParsedPacket;
import com.packetanalyzer.model.RawPacket;
import com.packetanalyzer.parser.PacketParser;

import java.util.List;

public class DPIEngine {

    private final PacketParser packetParser;
    private final RuleManager ruleManager;
    private final FlowTracker flowTracker;
    private final Statistics statistics;
    private final AlertManager alertManager;

    public DPIEngine() {
        this.packetParser = new PacketParser();
        this.ruleManager = new RuleManager();
        this.flowTracker = new FlowTracker();
        this.statistics = new Statistics();
        this.alertManager = new AlertManager();
    }

    public void process(RawPacket rawPacket) {

        if (rawPacket == null) {
            return;
        }

        ParsedPacket packet = packetParser.parse(rawPacket);

        if (packet == null) {
            return;
        }

        statistics.record(packet);

        flowTracker.track(packet);

        alertManager.inspect(packet);

        if (ruleManager.shouldDrop(packet)) {
            statistics.recordDroppedPacket();
        }
    }

    public void processAll(List<RawPacket> packets) {

        if (packets == null) {
            return;
        }

        for (RawPacket packet : packets) {
            process(packet);
        }
    }

    public PacketParser getPacketParser() {
        return packetParser;
    }

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    public FlowTracker getFlowTracker() {
        return flowTracker;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public void reset() {
        statistics.reset();
        alertManager.clearAlerts();
        flowTracker.reset();
    }
}
