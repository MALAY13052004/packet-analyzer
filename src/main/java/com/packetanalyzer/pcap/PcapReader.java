package com.packetanalyzer.pcap;

import com.packetanalyzer.model.RawPacket;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PcapReader {

    private static final int GLOBAL_HEADER_LENGTH = 24;
    private static final int PACKET_HEADER_LENGTH = 16;

    private boolean littleEndian;
    private boolean nanosecondResolution;

    public List<RawPacket> read(Path file) throws IOException {

        List<RawPacket> packets = new ArrayList<>();

        try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {

            byte[] globalHeader = readExactly(input, GLOBAL_HEADER_LENGTH);

            determineFormat(globalHeader);

            while (true) {

                byte[] packetHeader;

                try {
                    packetHeader = readExactly(input, PACKET_HEADER_LENGTH);
                } catch (EOFException e) {
                    break;
                }

                long timestampSeconds = readUnsignedInt(packetHeader, 0);
                long timestampFraction = readUnsignedInt(packetHeader, 4);
                long capturedLength = readUnsignedInt(packetHeader, 8);
                long originalLength = readUnsignedInt(packetHeader, 12);

                if (capturedLength < 0 || capturedLength > Integer.MAX_VALUE) {
                    throw new IOException("Invalid captured packet length: " + capturedLength);
                }

                if (originalLength < capturedLength) {
                    throw new IOException(
                            "Invalid packet lengths: captured="
                                    + capturedLength
                                    + ", original="
                                    + originalLength
                    );
                }

                byte[] packetData = readExactly(
                        input,
                        (int) capturedLength
                );

                long timestampMicros;

                if (nanosecondResolution) {
                    timestampMicros = timestampFraction / 1000;
                } else {
                    timestampMicros = timestampFraction;
                }

                packets.add(
                        new RawPacket(
                                timestampSeconds,
                                timestampMicros,
                                packetData
                        )
                );
            }
        }

        return packets;
    }

    private void determineFormat(byte[] header) throws IOException {

        int magicBigEndian = ByteBuffer.wrap(header, 0, 4)
                .order(ByteOrder.BIG_ENDIAN)
                .getInt();

        int magicLittleEndian = ByteBuffer.wrap(header, 0, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();

        if (magicBigEndian == 0xA1B2C3D4) {
            littleEndian = false;
            nanosecondResolution = false;
            return;
        }

        if (magicLittleEndian == 0xA1B2C3D4) {
            littleEndian = true;
            nanosecondResolution = false;
            return;
        }

        if (magicBigEndian == 0xA1B23C4D) {
            littleEndian = false;
            nanosecondResolution = true;
            return;
        }

        if (magicLittleEndian == 0xA1B23C4D) {
            littleEndian = true;
            nanosecondResolution = true;
            return;
        }

        throw new IOException(
                String.format(
                        "Unsupported PCAP magic number: 0x%08X",
                        magicBigEndian
                )
        );
    }

    private long readUnsignedInt(byte[] data, int offset) {

        if (littleEndian) {

            return ((long) data[offset] & 0xFF)
                    | (((long) data[offset + 1] & 0xFF) << 8)
                    | (((long) data[offset + 2] & 0xFF) << 16)
                    | (((long) data[offset + 3] & 0xFF) << 24);

        } else {

            return (((long) data[offset] & 0xFF) << 24)
                    | (((long) data[offset + 1] & 0xFF) << 16)
                    | (((long) data[offset + 2] & 0xFF) << 8)
                    | ((long) data[offset + 3] & 0xFF);
        }
    }

    private byte[] readExactly(InputStream input, int length)
            throws IOException {

        byte[] data = new byte[length];

        int totalRead = 0;

        while (totalRead < length) {

            int read = input.read(
                    data,
                    totalRead,
                    length - totalRead
            );

            if (read == -1) {

                if (totalRead == 0) {
                    throw new EOFException();
                }

                throw new EOFException(
                        "Unexpected end of PCAP file"
                );
            }

            totalRead += read;
        }

        return data;
    }
}
