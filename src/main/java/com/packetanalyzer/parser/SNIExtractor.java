package com.packetanalyzer.parser;

public class SNIExtractor {

    public String extract(byte[] data, int offset, int length) {

        if (data == null || length <= 0) {
            return null;
        }

        int end = Math.min(data.length, offset + length);

        if (offset < 0 || offset >= end) {
            return null;
        }

        for (int i = offset; i < end - 5; i++) {

            if ((data[i] & 0xFF) == 0x16 &&
                    (data[i + 1] & 0xFF) == 0x03) {

                String sni = findHostname(data, i + 5, end);

                if (sni != null) {
                    return sni;
                }
            }
        }

        return null;
    }

    private String findHostname(byte[] data, int start, int end) {

        for (int i = start; i < end - 5; i++) {

            if ((data[i] & 0xFF) == 0x00 &&
                    (data[i + 1] & 0xFF) == 0x00) {

                int extensionLength =
                        ((data[i + 2] & 0xFF) << 8)
                                | (data[i + 3] & 0xFF);

                int extensionEnd = i + 4 + extensionLength;

                if (extensionEnd > end) {
                    continue;
                }

                for (int j = i + 4; j < extensionEnd - 2; j++) {

                    int nameType = data[j] & 0xFF;

                    if (nameType != 0) {
                        continue;
                    }

                    int nameLength =
                            ((data[j + 1] & 0xFF) << 8)
                                    | (data[j + 2] & 0xFF);

                    int nameStart = j + 3;
                    int nameEnd = nameStart + nameLength;

                    if (nameEnd > extensionEnd || nameLength <= 0) {
                        continue;
                    }

                    String hostname =
                            new String(
                                    data,
                                    nameStart,
                                    nameLength,
                                    java.nio.charset.StandardCharsets.US_ASCII
                            );

                    if (isValidHostname(hostname)) {
                        return hostname;
                    }
                }
            }
        }

        return null;
    }

    private boolean isValidHostname(String hostname) {

        if (hostname == null || hostname.isEmpty()) {
            return false;
        }

        if (hostname.length() > 253) {
            return false;
        }

        for (int i = 0; i < hostname.length(); i++) {

            char c = hostname.charAt(i);

            if (Character.isLetterOrDigit(c) ||
                    c == '.' ||
                    c == '-') {
                continue;
            }

            return false;
        }

        return hostname.contains(".");
    }
}
