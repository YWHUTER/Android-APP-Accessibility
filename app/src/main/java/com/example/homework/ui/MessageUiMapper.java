package com.example.homework.ui;

import com.example.homework.model.ConversationSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MessageUiMapper {
    public static final int TAB_ALL = 0;
    public static final int TAB_BUYING = 1;
    public static final int TAB_SELLING = 2;

    private MessageUiMapper() {
    }

    public static Summary buildSummary(List<ConversationSummary> conversations) {
        int unreadMessages = 0;
        int buyingCount = 0;
        int sellingCount = 0;
        int unreadConversations = 0;

        for (ConversationSummary conversation : conversations) {
            unreadMessages += conversation.getUnreadCount();
            if (conversation.isBuyer()) {
                buyingCount++;
            } else {
                sellingCount++;
            }
            if (conversation.getUnreadCount() > 0) {
                unreadConversations++;
            }
        }

        return new Summary(unreadMessages, buyingCount, sellingCount, unreadConversations);
    }

    public static List<ConversationSummary> filterConversations(List<ConversationSummary> allConversations,
                                                                int currentTab, String query) {
        return filterConversations(allConversations, currentTab, query, false);
    }

    public static List<ConversationSummary> filterConversations(List<ConversationSummary> allConversations,
                                                                int currentTab, String query,
                                                                boolean unreadOnly) {
        List<ConversationSummary> filteredList = new ArrayList<>();
        String normalizedQuery = safe(query).trim().toLowerCase(Locale.ROOT);

        for (ConversationSummary conversation : allConversations) {
            boolean tabMatch = currentTab == TAB_ALL
                    || (currentTab == TAB_BUYING && conversation.isBuyer())
                    || (currentTab == TAB_SELLING && !conversation.isBuyer());
            if (!tabMatch) {
                continue;
            }
            if (unreadOnly && conversation.getUnreadCount() <= 0) {
                continue;
            }

            if (normalizedQuery.isEmpty()) {
                filteredList.add(conversation);
                continue;
            }

            String searchableText = (safe(conversation.getOtherUserName()) + " "
                    + safe(conversation.getItemTitle()) + " "
                    + safe(conversation.getLastMessage()) + " "
                    + safe(conversation.getOtherUserMajor())).toLowerCase(Locale.ROOT);
            if (searchableText.contains(normalizedQuery)) {
                filteredList.add(conversation);
            }
        }
        return filteredList;
    }

    public static String resolveEmptyStateText(int currentTab, String query) {
        return resolveEmptyStateText(currentTab, query, false);
    }

    public static String resolveEmptyStateText(int currentTab, String query, boolean unreadOnly) {
        if (unreadOnly) {
            if (!safe(query).trim().isEmpty()) {
                return "没有找到符合条件的未读消息，换个关键词试试。";
            }
            return "当前没有未读消息，先看看全部会话吧。";
        }
        if (!safe(query).trim().isEmpty()) {
            return "没有找到匹配的聊天，换个关键词试试。";
        }
        if (currentTab == TAB_BUYING) {
            return "暂时没有买入相关的会话。";
        }
        if (currentTab == TAB_SELLING) {
            return "暂时没有卖出相关的会话。";
        }
        return "暂时还没有会话。";
    }

    public static String formatUnreadCount(int unreadCount) {
        return unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
    }

    public static String buildAvatarText(ConversationSummary conversation) {
        if (conversation == null || safe(conversation.getOtherUserName()).trim().isEmpty()) {
            return "聊";
        }
        return conversation.getOtherUserName().substring(0, 1).toUpperCase(Locale.getDefault());
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }

    public static final class Summary {
        private final int unreadMessages;
        private final int buyingCount;
        private final int sellingCount;
        private final int unreadConversations;

        public Summary(int unreadMessages, int buyingCount, int sellingCount, int unreadConversations) {
            this.unreadMessages = unreadMessages;
            this.buyingCount = buyingCount;
            this.sellingCount = sellingCount;
            this.unreadConversations = unreadConversations;
        }

        public int getUnreadMessages() {
            return unreadMessages;
        }

        public int getBuyingCount() {
            return buyingCount;
        }

        public int getSellingCount() {
            return sellingCount;
        }

        public int getUnreadConversations() {
            return unreadConversations;
        }
    }
}
