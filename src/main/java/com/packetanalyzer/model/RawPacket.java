package com.packetanalyzer.model;

public class RawPacket {

    private final long timestampSeconds;
    private final long timestampMicros;
    private final byte[] data;

    public RawPacket(long timestampSeconds, long timestampMicros, byte[] data) {
        this.timestampSeconds = timestampSeconds;
        this.timestampMicros = timestampMicros;
        this.data = data.clone();
    }

    public long getTimestampSeconds() {
        return timestampSeconds;
    }

    public long getTimestampMicros() {
        return timestampMicros;
    }

    public byte[] getData() {
        return data.clone();
    }

    public int getLength() {
        return data.length;
    }
}
