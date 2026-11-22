package com.example.homework.ui;

public enum DiscoveryFeedMode {
    FOLLOW,
    RECOMMEND,
    LATEST;

    public static DiscoveryFeedMode fromName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FOLLOW;
        }
        for (DiscoveryFeedMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return FOLLOW;
    }
}
