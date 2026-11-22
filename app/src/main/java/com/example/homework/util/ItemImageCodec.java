package com.example.homework.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ItemImageCodec {
    private static final String DELIMITER = "\n";

    private ItemImageCodec() {
    }

    public static List<String> decode(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] parts = rawValue.split(DELIMITER);
        List<String> results = new ArrayList<>();
        for (String part : parts) {
            if (part != null) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    results.add(trimmed);
                }
            }
        }
        return results;
    }

    public static String encode(List<String> imageSources) {
        if (imageSources == null || imageSources.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (String imageSource : imageSources) {
            if (imageSource == null) {
                continue;
            }
            String trimmed = imageSource.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(DELIMITER);
            }
            builder.append(trimmed);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    public static String getCoverImage(String rawValue) {
        List<String> images = decode(rawValue);
        return images.isEmpty() ? null : images.get(0);
    }
}
