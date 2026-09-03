package com.packetanalyzer.model;

public class ParsedPacket {

    private final RawPacket rawPacket;
    private final EthernetHeader ethernetHeader;
    private final IPv4Header ipv4Header;
    private final TcpHeader tcpHeader;
    private final UdpHeader udpHeader;
    private final AppType appType;
    private final String sni;

    public ParsedPacket(
            RawPacket rawPacket,
            EthernetHeader ethernetHeader,
            IPv4Header ipv4Header,
            TcpHeader tcpHeader,
            UdpHeader udpHeader,
            AppType appType,
            String sni) {

        this.rawPacket = rawPacket;
        this.ethernetHeader = ethernetHeader;
        this.ipv4Header = ipv4Header;
        this.tcpHeader = tcpHeader;
        this.udpHeader = udpHeader;
        this.appType = appType;
        this.sni = sni;
    }

    public RawPacket getRawPacket() {
        return rawPacket;
    }

    public EthernetHeader getEthernetHeader() {
        return ethernetHeader;
    }

    public IPv4Header getIpv4Header() {
        return ipv4Header;
    }

    public TcpHeader getTcpHeader() {
        return tcpHeader;
    }

    public UdpHeader getUdpHeader() {
        return udpHeader;
    }

    public AppType getAppType() {
        return appType;
    }

    public String getSni() {
        return sni;
    }

    public boolean isTcp() {
        return tcpHeader != null;
    }

    public boolean isUdp() {
        return udpHeader != null;
    }
}
