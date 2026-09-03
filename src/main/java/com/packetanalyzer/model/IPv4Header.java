package com.packetanalyzer.model;

public class IPv4Header {

    private final String sourceIp;
    private final String destinationIp;
    private final int protocol;
    private final int ttl;

    public IPv4Header(String sourceIp, String destinationIp, int protocol, int ttl) {
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.protocol = protocol;
        this.ttl = ttl;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public int getProtocol() {
        return protocol;
    }

    public int getTtl() {
        return ttl;
    }

    @Override
    public String toString() {
        return "IPv4Header{" +
                "sourceIp=" + sourceIp +
                ", destinationIp=" + destinationIp +
                ", protocol=" + protocol +
                ", ttl=" + ttl +
                '}';
    }
}
