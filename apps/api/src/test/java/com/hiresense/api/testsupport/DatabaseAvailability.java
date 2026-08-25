package com.hiresense.api.testsupport;

import java.sql.DriverManager;

public final class DatabaseAvailability {

    private static volatile Boolean reachable;

    private DatabaseAvailability() {}

    public static boolean isReachable() {
        Boolean cached = reachable;
        if (cached != null) {
            return cached;
        }
        boolean result;
        try (var connection =
                DriverManager.getConnection("jdbc:postgresql://localhost:5433/hiresense", "hiresense", "hiresense")) {
            result = connection.isValid(2);
        } catch (Exception e) {
            result = false;
        }
        reachable = result;
        return result;
    }
}
