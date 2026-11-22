package com.example.homework.ui;

import com.example.homework.model.Item;
import com.example.homework.model.User;

import java.util.Locale;

public final class ProfileUiMapper {
    private ProfileUiMapper() {
    }

    public static String buildInterestSummary(User user) {
        return String.format(
                Locale.getDefault(),
                "系统会结合你的专业“%s”和兴趣“%s”，为你推荐更适合的闲置物品。",
                safe(user != null ? user.getMajor() : null),
                safe(user != null ? user.getInterests() : null)
        );
    }

    public static String formatListingMeta(Item item) {
        return String.format(
                Locale.getDefault(),
                "¥%.2f · %s",
                item.getPrice(),
                safe(item.getCategory())
        );
    }

    public static String buildMajorLabel(User user) {
        String major = safe(user != null ? user.getMajor() : null);
        if (user != null && user.getVerifiedAt() > 0L) {
            return major + " · 已认证";
        }
        return major;
    }

    public static String buildAvatarText(User user) {
        String name = user != null ? user.getName() : null;
        if (name == null || name.trim().isEmpty()) {
            return "我";
        }
        return name.substring(0, 1);
    }

    public static String buildListingsEmptyText() {
        return "你当前没有在售闲置，发布后会显示在这里。";
    }

    public static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "未填写" : value;
    }
}
