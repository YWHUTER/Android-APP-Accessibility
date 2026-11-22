package com.example.homework.ui;

import com.example.homework.model.Item;
import com.example.homework.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DiscoveryUiMapper {
    public static final String CATEGORY_ALL = "全部";
    public static final String SEARCH_HINT = "搜索教材、自行车、相机...";

    private DiscoveryUiMapper() {
    }

    public static List<Item> filterItems(List<Item> sourceItems, String query, String category) {
        List<Item> filteredItems = new ArrayList<>();
        String normalizedQuery = normalize(query);
        for (Item item : sourceItems) {
            if (matchesQuery(item, normalizedQuery) && matchesCategory(item, category)) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    public static RecommendationPanel buildRecommendationPanel(User user, int itemCount) {
        if (user == null) {
            return RecommendationPanel.hidden();
        }

        String major = safeText(user.getMajor(), "你的专业");
        String interests = firstUsefulToken(user.getInterests(), "你的兴趣");
        return new RecommendationPanel(
                true,
                "为你生成今日推荐",
                String.format(
                        Locale.getDefault(),
                        "结合 %s、%s 以及最近的互动记录，筛出了 %d 个更值得优先看的在售商品。",
                        major,
                        interests,
                        itemCount
                ),
                "专业 · " + major,
                "兴趣 · " + interests,
                "推荐 " + itemCount
        );
    }

    public static String resolveEmptyStateText(DiscoveryFeedMode feedMode) {
        if (feedMode == DiscoveryFeedMode.RECOMMEND) {
            return "暂时没有匹配到推荐商品，试试修改兴趣或筛选条件。";
        }
        if (feedMode == DiscoveryFeedMode.LATEST) {
            return "暂时没有最新上架的商品，稍后再来看看。";
        }
        return "暂时没有符合条件的闲置，换个关键词试试。";
    }

    public static String buildCardReason(Item item, int position, DiscoveryFeedMode feedMode, User currentUser) {
        if (feedMode == DiscoveryFeedMode.LATEST) {
            return buildLatestReason(item, System.currentTimeMillis());
        }
        if (feedMode != DiscoveryFeedMode.RECOMMEND || currentUser == null) {
            return "";
        }

        String userMajor = normalize(currentUser.getMajor());
        String preferredMajor = normalize(item.getPreferredMajor());
        String sellerMajor = normalize(item.getSellerMajor());
        String interestMatch = findMatchingInterestToken(item, currentUser);

        if (!userMajor.isEmpty() && preferredMajor.contains(userMajor)) {
            return "专业匹配";
        }
        if (!interestMatch.isEmpty()) {
            return "兴趣命中 · " + interestMatch;
        }
        if (!userMajor.isEmpty() && sellerMajor.contains(userMajor)) {
            return "同专业卖家";
        }
        if (item.getCampusPickupOnly() == 1) {
            return "支持校内面交";
        }
        if (position < 3) {
            return "高匹配推荐";
        }
        return "为你精选";
    }

    public static String buildLatestReason(Item item, long nowMillis) {
        long diff = Math.max(0L, nowMillis - item.getCreatedAt());
        long minutes = diff / (60L * 1000L);
        if (minutes < 60L) {
            return "最新上新";
        }

        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours <= 3L ? "3小时内上新" : "今日新发布";
        }

        long days = hours / 24L;
        if (days <= 3L) {
            return "近3天上新";
        }
        if (days <= 7L) {
            return "本周新发布";
        }
        return "最近更新";
    }

    public static int resolveImageHeight(Item item, int position) {
        String category = safe(item.getCategory());
        if (category.contains("教材")) {
            return 150;
        }
        if (category.contains("电子")) {
            return position % 2 == 0 ? 176 : 158;
        }
        if (category.contains("出行")) {
            return 184;
        }
        if (item.getTitle() != null && item.getTitle().length() > 14) {
            return 164;
        }
        return 156;
    }

    public static String formatPrice(double price) {
        long rounded = Math.round(price);
        if (Math.abs(price - rounded) < 0.01d) {
            return String.format(Locale.getDefault(), "¥%d", rounded);
        }
        return String.format(Locale.getDefault(), "¥%.2f", price);
    }

    public static String formatRelativeTime(long createdAt) {
        return formatRelativeTime(createdAt, System.currentTimeMillis());
    }

    public static String formatRelativeTime(long createdAt, long nowMillis) {
        long diff = Math.max(0L, nowMillis - createdAt);
        long minutes = diff / (60L * 1000L);
        if (minutes <= 0L) {
            return "刚刚";
        }
        if (minutes < 60L) {
            return minutes + "分钟前";
        }

        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + "小时前";
        }

        long days = hours / 24L;
        if (days < 7L) {
            return days + "天前";
        }

        long weeks = days / 7L;
        if (weeks < 5L) {
            return weeks + "周前";
        }

        long months = Math.max(days / 30L, 1L);
        return months + "个月前";
    }

    public static String buildAvatar(String sellerName) {
        if (isBlank(sellerName)) {
            return "校";
        }
        return sellerName.substring(0, 1);
    }

    public static String shortLocation(String location) {
        if (isBlank(location)) {
            return "校内面交";
        }
        if (location.contains("图书馆")) {
            return "图书馆";
        }
        if (location.contains("宿舍")) {
            return "宿舍区";
        }
        if (location.contains("食堂")) {
            return "食堂";
        }
        if (location.contains("教学楼")) {
            return "教学楼";
        }
        return location.length() > 6 ? location.substring(0, 6) : location;
    }

    public static String firstUsefulToken(String source, String fallback) {
        String[] tokens = safe(source).split("[,，、\\s|]+");
        for (String token : tokens) {
            if (token.trim().length() >= 2) {
                return token.trim();
            }
        }
        return fallback;
    }

    private static boolean matchesQuery(Item item, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String searchableText = normalize(
                safe(item.getTitle()) + " " +
                        safe(item.getDescription()) + " " +
                        safe(item.getCategory()) + " " +
                        safe(item.getLocation()) + " " +
                        safe(item.getSellerName()) + " " +
                        safe(item.getTags())
        );
        return searchableText.contains(query);
    }

    private static boolean matchesCategory(Item item, String category) {
        if (isBlank(category) || CATEGORY_ALL.equals(category)) {
            return true;
        }

        switch (category) {
            case "教材":
                return containsAny(item, "教材", "教辅", "高数", "课本");
            case "数码":
                return containsAny(item, "电子产品", "相机", "笔记本", "电脑", "键盘", "耳机");
            case "宿舍":
                return containsAny(item, "宿舍用品", "宿舍", "生活电器");
            case "穿搭":
                return containsAny(item, "穿搭", "服饰", "鞋", "包");
            case "运动":
                return containsAny(item, "运动", "篮球", "球拍", "健身");
            case "骑行":
                return containsAny(item, "校园出行", "自行车", "骑行", "代步");
            case "手作":
                return containsAny(item, "艺术", "手作", "绘画");
            case "美妆":
                return containsAny(item, "美妆", "护肤", "化妆");
            case "美食":
                return containsAny(item, "美食", "零食", "饮料");
            default:
                return true;
        }
    }

    private static boolean containsAny(Item item, String... keywords) {
        String normalized = normalize(
                safe(item.getCategory()) + " " +
                        safe(item.getTitle()) + " " +
                        safe(item.getDescription()) + " " +
                        safe(item.getTags())
        );
        for (String keyword : keywords) {
            if (normalized.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private static String findMatchingInterestToken(Item item, User user) {
        String itemText = normalize(
                safe(item.getCategory()) + " " +
                        safe(item.getTitle()) + " " +
                        safe(item.getDescription()) + " " +
                        safe(item.getTags())
        );
        String[] tokens = safe(user.getInterests()).split("[,，、\\s|]+");
        for (String token : tokens) {
            String normalizedToken = normalize(token);
            if (normalizedToken.length() >= 2 && itemText.contains(normalizedToken)) {
                return token.trim();
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String safeText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    public static final class RecommendationPanel {
        private final boolean visible;
        private final String title;
        private final String summary;
        private final String profileChip;
        private final String interestChip;
        private final String countChip;

        public RecommendationPanel(boolean visible, String title, String summary,
                                   String profileChip, String interestChip, String countChip) {
            this.visible = visible;
            this.title = title;
            this.summary = summary;
            this.profileChip = profileChip;
            this.interestChip = interestChip;
            this.countChip = countChip;
        }

        public static RecommendationPanel hidden() {
            return new RecommendationPanel(false, "", "", "", "", "");
        }

        public boolean isVisible() {
            return visible;
        }

        public String getTitle() {
            return title;
        }

        public String getSummary() {
            return summary;
        }

        public String getProfileChip() {
            return profileChip;
        }

        public String getInterestChip() {
            return interestChip;
        }

        public String getCountChip() {
            return countChip;
        }
    }
}
