package com.packetanalyzer.engine;

import com.packetanalyzer.model.AppType;
import com.packetanalyzer.model.ParsedPacket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlertManager {

    private final List<Alert> alerts = new ArrayList<>();
    private final Set<String> alertKeys = new HashSet<>();

    public void inspect(ParsedPacket packet) {

        if (packet == null) {
            return;
        }

        if (packet.getTcpHeader() != null) {

            int destinationPort =
                    packet.getTcpHeader().getDestinationPort();

            if (destinationPort == 23) {

                addAlert(
                        "TELNET",
                        "Telnet traffic detected",
                        getSourceIp(packet),
                        getDestinationIp(packet),
                        Severity.HIGH
                );
            }
        }

        if (packet.getAppType() == AppType.UNKNOWN
                && !hasAlertForPacket("TELNET", packet)) {

            addAlert(
                    "UNKNOWN_PROTOCOL",
                    "Unknown application protocol detected",
                    getSourceIp(packet),
                    getDestinationIp(packet),
                    Severity.LOW
            );
        }
    }

    private boolean hasAlertForPacket(String type, ParsedPacket packet) {

        String sourceIp = getSourceIp(packet);
        String destinationIp = getDestinationIp(packet);

        String key = type + "|" + sourceIp + "|" + destinationIp;

        return alertKeys.contains(key);
    }

    private void addAlert(
            String type,
            String message,
            String sourceIp,
            String destinationIp,
            Severity severity) {

        String key = type + "|" + sourceIp + "|" + destinationIp;

        if (alertKeys.contains(key)) {
            return;
        }

        alertKeys.add(key);

        alerts.add(
                new Alert(
                        type,
                        message,
                        sourceIp,
                        destinationIp,
                        severity
                )
        );
    }

    private String getSourceIp(ParsedPacket packet) {

        if (packet.getIpv4Header() == null) {
            return null;
        }

        return packet.getIpv4Header().getSourceIp();
    }

    private String getDestinationIp(ParsedPacket packet) {

        if (packet.getIpv4Header() == null) {
            return null;
        }

        return packet.getIpv4Header().getDestinationIp();
    }

    public List<Alert> getAlerts() {
        return List.copyOf(alerts);
    }

    public int getAlertCount() {
        return alerts.size();
    }

    public void clearAlerts() {
        alerts.clear();
        alertKeys.clear();
    }

    public enum Severity {
        INFO,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public static class Alert {

        private final String type;
        private final String message;
        private final String sourceIp;
        private final String destinationIp;
        private final Severity severity;

        public Alert(
                String type,
                String message,
                String sourceIp,
                String destinationIp,
                Severity severity) {

            this.type = type;
            this.message = message;
            this.sourceIp = sourceIp;
            this.destinationIp = destinationIp;
            this.severity = severity;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public String getSourceIp() {
            return sourceIp;
        }

        public String getDestinationIp() {
            return destinationIp;
        }

        public Severity getSeverity() {
            return severity;
        }

        @Override
        public String toString() {

            return "Alert{" +
                    "type='" + type + '\'' +
                    ", message='" + message + '\'' +
                    ", sourceIp='" + sourceIp + '\'' +
                    ", destinationIp='" + destinationIp + '\'' +
                    ", severity=" + severity +
                    '}';
        }
    }
}
