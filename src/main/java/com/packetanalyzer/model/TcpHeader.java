package com.packetanalyzer.model;

public class TcpHeader {

    private final int sourcePort;
    private final int destinationPort;
    private final long sequenceNumber;
    private final long acknowledgementNumber;
    private final int flags;
    private final int windowSize;

    public TcpHeader(
            int sourcePort,
            int destinationPort,
            long sequenceNumber,
            long acknowledgementNumber,
            int flags,
            int windowSize) {

        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.sequenceNumber = sequenceNumber;
        this.acknowledgementNumber = acknowledgementNumber;
        this.flags = flags;
        this.windowSize = windowSize;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public long getAcknowledgementNumber() {
        return acknowledgementNumber;
    }

    public int getFlags() {
        return flags;
    }

    public int getWindowSize() {
        return windowSize;
    }

    @Override
    public String toString() {
        return "TcpHeader{" +
                "sourcePort=" + sourcePort +
                ", destinationPort=" + destinationPort +
                ", sequenceNumber=" + sequenceNumber +
                ", acknowledgementNumber=" + acknowledgementNumber +
                ", flags=" + flags +
                ", windowSize=" + windowSize +
                '}';
    }
}
