package com.example;

import lombok.Generated;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class HostAddresses {
    public static final String HOST_ADDRESS;

    @Generated
    private HostAddresses() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static {
        try {
            HOST_ADDRESS = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            throw new IllegalStateException("Local host name could not be resolved.", ex);
        }
    }
}
