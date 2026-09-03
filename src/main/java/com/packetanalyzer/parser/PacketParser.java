package com.packetanalyzer.parser;

import com.packetanalyzer.model.AppType;
import com.packetanalyzer.model.EthernetHeader;
import com.packetanalyzer.model.IPv4Header;
import com.packetanalyzer.model.ParsedPacket;
import com.packetanalyzer.model.RawPacket;
import com.packetanalyzer.model.TcpHeader;
import com.packetanalyzer.model.UdpHeader;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PacketParser {

    private static final int ETH_HEADER_LENGTH = 14;
    private static final int ETH_TYPE_IPV4 = 0x0800;

    public ParsedPacket parse(RawPacket rawPacket) {

        byte[] data = rawPacket.getData();

        if (data.length < ETH_HEADER_LENGTH) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

        byte[] destinationMac = new byte[6];
        byte[] sourceMac = new byte[6];

        buffer.get(destinationMac);
        buffer.get(sourceMac);

        int etherType = Short.toUnsignedInt(buffer.getShort());

        EthernetHeader ethernetHeader = new EthernetHeader(
                formatMac(sourceMac),
                formatMac(destinationMac),
                etherType
        );

        if (etherType != ETH_TYPE_IPV4) {
            return new ParsedPacket(
                    rawPacket,
                    ethernetHeader,
                    null,
                    null,
                    null,
                    AppType.UNKNOWN,
                    null
            );
        }

        if (data.length < ETH_HEADER_LENGTH + 20) {
            return null;
        }

        int ipStart = ETH_HEADER_LENGTH;

        int versionAndIhl = Byte.toUnsignedInt(data[ipStart]);
        int version = (versionAndIhl >> 4) & 0x0F;
        int ihl = versionAndIhl & 0x0F;

        if (version != 4 || ihl < 5) {
            return null;
        }

        int ipHeaderLength = ihl * 4;

        if (data.length < ipStart + ipHeaderLength) {
            return null;
        }

        int ttl = Byte.toUnsignedInt(data[ipStart + 8]);
        int protocol = Byte.toUnsignedInt(data[ipStart + 9]);

        String sourceIp = formatIp(data, ipStart + 12);
        String destinationIp = formatIp(data, ipStart + 16);

        IPv4Header ipv4Header = new IPv4Header(
                sourceIp,
                destinationIp,
                protocol,
                ttl
        );

        int transportStart = ipStart + ipHeaderLength;

        TcpHeader tcpHeader = null;
        UdpHeader udpHeader = null;
        AppType appType = AppType.UNKNOWN;
        String sni = null;

        if (protocol == 6) {

            if (data.length >= transportStart + 20) {

                int sourcePort = unsignedShort(data, transportStart);
                int destinationPort = unsignedShort(data, transportStart + 2);

                long sequenceNumber = unsignedInt(data, transportStart + 4);
                long acknowledgementNumber = unsignedInt(data, transportStart + 8);

                int flags = unsignedShort(data, transportStart + 12) & 0x01FF;

                int windowSize = unsignedShort(data, transportStart + 14);

                tcpHeader = new TcpHeader(
                        sourcePort,
                        destinationPort,
                        sequenceNumber,
                        acknowledgementNumber,
                        flags,
                        windowSize
                );

                appType = classifyTcp(sourcePort, destinationPort);
            }

        } else if (protocol == 17) {

            if (data.length >= transportStart + 8) {

                int sourcePort = unsignedShort(data, transportStart);
                int destinationPort = unsignedShort(data, transportStart + 2);
                int length = unsignedShort(data, transportStart + 4);

                udpHeader = new UdpHeader(
                        sourcePort,
                        destinationPort,
                        length
                );

                appType = classifyUdp(sourcePort, destinationPort);
            }
        }

        return new ParsedPacket(
                rawPacket,
                ethernetHeader,
                ipv4Header,
                tcpHeader,
                udpHeader,
                appType,
                sni
        );
    }

    private AppType classifyTcp(int sourcePort, int destinationPort) {

        if (sourcePort == 80 || destinationPort == 80) {
            return AppType.HTTP;
        }

        if (sourcePort == 443 || destinationPort == 443) {
            return AppType.HTTPS;
        }

        if (sourcePort == 22 || destinationPort == 22) {
            return AppType.SSH;
        }

        if (sourcePort == 21 || destinationPort == 21) {
            return AppType.FTP;
        }

        if (sourcePort == 25 || destinationPort == 25 ||
                sourcePort == 587 || destinationPort == 587) {
            return AppType.SMTP;
        }

        return AppType.OTHER;
    }

    private AppType classifyUdp(int sourcePort, int destinationPort) {

        if (sourcePort == 53 || destinationPort == 53) {
            return AppType.DNS;
        }

        if (sourcePort == 67 || destinationPort == 67 ||
                sourcePort == 68 || destinationPort == 68) {
            return AppType.DHCP;
        }

        return AppType.OTHER;
    }

    private int unsignedShort(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8)
                | (data[offset + 1] & 0xFF);
    }

    private long unsignedInt(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24)
                | ((long) (data[offset + 1] & 0xFF) << 16)
                | ((long) (data[offset + 2] & 0xFF) << 8)
                | (long) (data[offset + 3] & 0xFF);
    }

    private String formatMac(byte[] mac) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < mac.length; i++) {

            if (i > 0) {
                result.append(":");
            }

            result.append(String.format("%02X", mac[i] & 0xFF));
        }

        return result.toString();
    }

    private String formatIp(byte[] data, int offset) {

        try {

            byte[] address = new byte[4];

            System.arraycopy(
                    data,
                    offset,
                    address,
                    0,
                    4
            );

            return InetAddress.getByAddress(address).getHostAddress();

        } catch (Exception e) {

            return "0.0.0.0";
        }
    }
}
