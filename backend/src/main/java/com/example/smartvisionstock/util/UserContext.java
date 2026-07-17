package com.example.smartvisionstock.util;

public class UserContext {
    private static final ThreadLocal<Long> userId = new ThreadLocal<>();
    private static final ThreadLocal<String> username = new ThreadLocal<>();
    private static final ThreadLocal<String> role = new ThreadLocal<>();

    public static Long getCurrentUserId() {
        return userId.get();
    }

    public static Long getCurrentUserIdOrDefault(Long defaultValue) {
        Long id = userId.get();
        return id != null ? id : defaultValue;
    }

    public static void setCurrentUserId(Long id) {
        userId.set(id);
    }

    public static String getCurrentUsername() {
        return username.get();
    }

    public static String getCurrentUsernameOrDefault(String defaultValue) {
        String name = username.get();
        return name != null ? name : defaultValue;
    }

    public static String getCurrentRole() {
        return role.get();
    }

    public static void setCurrentRole(String r) {
        role.set(r);
    }

    public static void setUser(Long id, String name, String r) {
        userId.set(id);
        username.set(name);
        role.set(r);
    }

    public static void setCurrentUser(Long id, String name, String r) {
        userId.set(id);
        username.set(name);
        role.set(r);
    }

    public static void clear() {
        userId.remove();
        username.remove();
        role.remove();
    }
}