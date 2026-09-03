package com.packetanalyzer.model;

public class EthernetHeader {

    private final String sourceMac;
    private final String destinationMac;
    private final int etherType;

    public EthernetHeader(String sourceMac, String destinationMac, int etherType) {
        this.sourceMac = sourceMac;
        this.destinationMac = destinationMac;
        this.etherType = etherType;
    }

    public String getSourceMac() {
        return sourceMac;
    }

    public String getDestinationMac() {
        return destinationMac;
    }

    public int getEtherType() {
        return etherType;
    }

    @Override
    public String toString() {
        return "EthernetHeader{" +
                "sourceMac='" + sourceMac + '\'' +
                ", destinationMac='" + destinationMac + '\'' +
                ", etherType=" + etherType +
                '}';
    }
}
