package com.elibrary.interfaces.rest;

/**
 * Minimal stand-in for an authenticated principal.
 * <p>
 * This service does not ship a user registry; identity arrives as an
 * {@code X-User-Id} header and is resolved by {@link UserContextInterceptor}.
 * Swapping this for a JWT-based resolver changes one class, not the API.
 */
public final class UserContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<String>();

    private UserContext() {
    }

    public static void set(String userId) {
        CURRENT.set(userId);
    }

    public static String require() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}