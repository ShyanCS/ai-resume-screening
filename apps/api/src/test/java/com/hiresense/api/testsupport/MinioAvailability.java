package com.hiresense.api.testsupport;

import java.net.InetSocketAddress;
import java.net.Socket;

public final class MinioAvailability {

    private static volatile Boolean reachable;

    private MinioAvailability() {}

    public static boolean isReachable() {
        Boolean cached = reachable;
        if (cached != null) {
            return cached;
        }
        boolean result;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 9000), 1500);
            result = socket.isConnected();
        } catch (Exception e) {
            result = false;
        }
        reachable = result;
        return result;
    }
}
