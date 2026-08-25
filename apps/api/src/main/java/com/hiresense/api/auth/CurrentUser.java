package com.hiresense.api.auth;

public final class CurrentUser {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private CurrentUser() {}

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long id() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
