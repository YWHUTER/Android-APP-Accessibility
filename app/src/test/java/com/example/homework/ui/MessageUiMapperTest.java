package com.example.homework.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.homework.model.ConversationSummary;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class MessageUiMapperTest {
    @Test
    public void buildSummary_countsUnreadAndBuySellBuckets() {
        ConversationSummary buyingUnread = conversation(1L, true, 3, "相机");
        ConversationSummary sellingRead = conversation(2L, false, 0, "教材");
        ConversationSummary sellingUnread = conversation(3L, false, 1, "耳机");

        MessageUiMapper.Summary summary = MessageUiMapper.buildSummary(
                Arrays.asList(buyingUnread, sellingRead, sellingUnread)
        );

        assertEquals(4, summary.getUnreadMessages());
        assertEquals(1, summary.getBuyingCount());
        assertEquals(2, summary.getSellingCount());
        assertEquals(2, summary.getUnreadConversations());
    }

    @Test
    public void filterConversations_appliesTabAndKeywordTogether() {
        ConversationSummary buying = conversation(1L, true, 0, "数学教材");
        buying.setLastMessage("周末可以面交");
        buying.setOtherUserMajor("数学");

        ConversationSummary selling = conversation(2L, false, 0, "相机");
        selling.setLastMessage("今晚还能看机");
        selling.setOtherUserMajor("数字媒体");

        List<ConversationSummary> filtered = MessageUiMapper.filterConversations(
                Arrays.asList(buying, selling),
                MessageUiMapper.TAB_SELLING,
                "看机"
        );

        assertEquals(1, filtered.size());
        assertEquals(2L, filtered.get(0).getId());
    }

    @Test
    public void filterConversations_canLimitToUnreadOnly() {
        ConversationSummary readConversation = conversation(1L, true, 0, "教材");
        ConversationSummary unreadConversation = conversation(2L, true, 2, "键盘");

        List<ConversationSummary> filtered = MessageUiMapper.filterConversations(
                Arrays.asList(readConversation, unreadConversation),
                MessageUiMapper.TAB_ALL,
                "",
                true
        );

        assertEquals(1, filtered.size());
        assertEquals(2L, filtered.get(0).getId());
    }

    @Test
    public void resolveEmptyStateText_matchesTabAndSearchContext() {
        assertEquals("没有找到匹配的聊天，换个关键词试试。",
                MessageUiMapper.resolveEmptyStateText(MessageUiMapper.TAB_ALL, "相机"));
        assertEquals("暂时没有买入相关的会话。",
                MessageUiMapper.resolveEmptyStateText(MessageUiMapper.TAB_BUYING, ""));
        assertEquals("暂时没有卖出相关的会话。",
                MessageUiMapper.resolveEmptyStateText(MessageUiMapper.TAB_SELLING, ""));
        assertEquals("当前没有未读消息，先看看全部会话吧。",
                MessageUiMapper.resolveEmptyStateText(MessageUiMapper.TAB_ALL, "", true));
    }

    @Test
    public void helpers_formatUnreadAndAvatarText() {
        ConversationSummary conversation = conversation(8L, false, 120, "图书");
        conversation.setOtherUserName("李同学");

        assertEquals("99+", MessageUiMapper.formatUnreadCount(120));
        assertEquals("李", MessageUiMapper.buildAvatarText(conversation));
        assertTrue(MessageUiMapper.buildAvatarText(new ConversationSummary()).length() > 0);
    }

    private static ConversationSummary conversation(long id, boolean isBuyer, int unreadCount, String title) {
        ConversationSummary conversation = new ConversationSummary();
        conversation.setId(id);
        conversation.setBuyer(isBuyer);
        conversation.setUnreadCount(unreadCount);
        conversation.setItemTitle(title);
        conversation.setOtherUserName("同学" + id);
        conversation.setOtherUserMajor("专业" + id);
        conversation.setLastMessage("消息" + id);
        return conversation;
    }
}
