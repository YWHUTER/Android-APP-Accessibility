package com.example.homework.ui;

import com.example.homework.model.OrderRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class OrderListUiMapper {
    public enum SortOption {
        RECENT_ACTIVITY,
        PRICE_HIGH_TO_LOW,
        PRICE_LOW_TO_HIGH
    }

    private OrderListUiMapper() {
    }

    public static List<OrderRecord> filterAndSort(List<OrderRecord> orders, String query, String targetStatus,
                                                  boolean buyerMode, SortOption sortOption) {
        List<OrderRecord> results = new ArrayList<>();
        if (orders == null || orders.isEmpty()) {
            return results;
        }

        for (OrderRecord order : orders) {
            if (!matchesStatus(order, targetStatus)) {
                continue;
            }
            if (!matchesQuery(order, query, buyerMode)) {
                continue;
            }
            results.add(order);
        }

        results.sort(buildComparator(sortOption));
        return results;
    }

    public static boolean hasQuery(String query) {
        return query != null && !query.trim().isEmpty();
    }

    public static long resolveActivityTimestamp(OrderRecord order) {
        if (order == null) {
            return 0L;
        }
        if (order.getCompletedAt() > 0L) {
            return order.getCompletedAt();
        }
        if (order.getShippedAt() > 0L) {
            return order.getShippedAt();
        }
        return order.getCreatedAt();
    }

    private static boolean matchesStatus(OrderRecord order, String targetStatus) {
        return targetStatus == null || targetStatus.isEmpty() || targetStatus.equals(order.getStatus());
    }

    private static boolean matchesQuery(OrderRecord order, String query, boolean buyerMode) {
        if (!hasQuery(query)) {
            return true;
        }

        String counterpart = buyerMode ? safe(order.getSellerName()) : safe(order.getBuyerName());
        String searchableText = (
                safe(order.getItemTitle()) + " " +
                        safe(order.getItemLocation()) + " " +
                        safe(order.getItemCategory()) + " " +
                        safe(order.getItemCondition()) + " " +
                        safe(order.getStatus()) + " " +
                        counterpart
        ).toLowerCase(Locale.ROOT);
        return searchableText.contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private static Comparator<OrderRecord> buildComparator(SortOption sortOption) {
        SortOption resolvedSort = sortOption == null ? SortOption.RECENT_ACTIVITY : sortOption;
        Comparator<OrderRecord> recentComparator = (left, right) ->
                Long.compare(resolveActivityTimestamp(right), resolveActivityTimestamp(left));

        if (resolvedSort == SortOption.PRICE_HIGH_TO_LOW) {
            return (left, right) -> {
                int compare = Double.compare(right.getItemPrice(), left.getItemPrice());
                return compare != 0 ? compare : recentComparator.compare(left, right);
            };
        }
        if (resolvedSort == SortOption.PRICE_LOW_TO_HIGH) {
            return (left, right) -> {
                int compare = Double.compare(left.getItemPrice(), right.getItemPrice());
                return compare != 0 ? compare : recentComparator.compare(left, right);
            };
        }
        return recentComparator;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
