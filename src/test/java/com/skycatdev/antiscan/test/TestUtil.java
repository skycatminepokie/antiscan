package com.skycatdev.antiscan.test;

import net.minecraft.network.Connection;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtil {
    private AtomicInteger lastOfIp = new AtomicInteger(0);

    public Connection mockConnection(String ip) {
        InetSocketAddress address = mock();
        Inet4Address fourAddress = mock();
        Connection connection = mock();
        when(connection.getRemoteAddress()).thenReturn(address);
        when(address.getAddress()).thenReturn(fourAddress);
        when(fourAddress.getHostAddress()).thenReturn(ip);
        return connection;
    }

    public Connection mockConnection() {
        return mockConnection(newIp());
    }

    public String newIp() {
        // Wikipedia says 192.0.2.0/24 (192.0.2.0 - 192.0.2.255) is for documentation and examples. Seems safe to use here.
        int last = lastOfIp.getAndIncrement();
        if (last > 255) throw new RuntimeException("Ran out of fake ips");
        return "192.0.2." + last;
    }
}
