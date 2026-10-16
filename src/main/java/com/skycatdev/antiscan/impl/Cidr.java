package com.skycatdev.antiscan.impl;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;

public record Cidr(byte[] network, int prefix) {
    public static Cidr parse(String text) {
        String[] parts = text.split("/", -1);
        if (parts.length != 2 || !parts[1].matches("[0-9]{1,3}")) throw new IllegalArgumentException("Invalid CIDR");
        byte[] bytes = InetAddresses.forString(parts[0]).getAddress();
        int bits = Integer.parseInt(parts[1]);
        if (bits > bytes.length * 8) throw new IllegalArgumentException("Invalid prefix");
        for (int i = 0; i < bytes.length; i++) bytes[i] &= (byte) mask(bits - i * 8);
        return new Cidr(bytes, bits);
    }

    public boolean contains(InetAddress address) {
        return contains(address.getAddress());
    }

    public boolean contains(byte[] bytes) {
        if (bytes.length != network.length) return false;
        for (int i = 0; i < bytes.length; i++) {
            if ((bytes[i] & mask(prefix - i * 8)) != (network[i] & 255)) return false;
        }
        return true;
    }

    private static int mask(int bits) { return bits <= 0 ? 0 : bits >= 8 ? 255 : (255 << (8 - bits)) & 255; }
}
