package com.packetanalyzer.model;

public class UdpHeader {

    private final int sourcePort;
    private final int destinationPort;
    private final int length;

    public UdpHeader(int sourcePort, int destinationPort, int length) {
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.length = length;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "UdpHeader{" +
                "sourcePort=" + sourcePort +
                ", destinationPort=" + destinationPort +
                ", length=" + length +
                '}';
    }
}
