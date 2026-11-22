package com.example.homework.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.homework.model.Item;
import com.example.homework.model.User;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class DiscoveryUiMapperTest {
    @Test
    public void filterItems_filtersByQueryAndCategorySignals() {
        Item textbook = item(1L, "高等数学教材", "教材", "高数 课本", "教学楼 A", 0L);
        Item keyboard = item(2L, "机械键盘", "电子产品", "键盘 数码", "图书馆", 0L);

        List<Item> filteredItems = DiscoveryUiMapper.filterItems(
                Arrays.asList(textbook, keyboard),
                "高数",
                "教材"
        );

        assertEquals(1, filteredItems.size());
        assertEquals(1L, filteredItems.get(0).getId());
    }

    @Test
    public void buildRecommendationPanel_usesUserProfileAndCount() {
        User user = user("计算机科学与技术", "键盘、摄影");

        DiscoveryUiMapper.RecommendationPanel panel =
                DiscoveryUiMapper.buildRecommendationPanel(user, 3);

        assertTrue(panel.isVisible());
        assertTrue(panel.getSummary().contains("计算机科学与技术"));
        assertTrue(panel.getSummary().contains("键盘"));
        assertEquals("推荐 3", panel.getCountChip());
    }

    @Test
    public void buildCardReason_prioritizesMajorMatchBeforeOtherSignals() {
        User user = user("计算机科学与技术", "摄影");
        Item item = item(3L, "算法导论", "教材", "算法", "图书馆", 0L);
        item.setPreferredMajor("计算机科学与技术");
        item.setCampusPickupOnly(1);

        String reason = DiscoveryUiMapper.buildCardReason(
                item,
                5,
                DiscoveryFeedMode.RECOMMEND,
                user
        );

        assertEquals("专业匹配", reason);
    }

    @Test
    public void buildLatestReason_usesFreshnessBuckets() {
        long now = 1_000_000L;
        Item freshItem = item(4L, "便携相机", "电子产品", "摄影", "宿舍", now - 30L * 60L * 1000L);
        Item todayItem = item(5L, "台灯", "宿舍用品", "宿舍", "宿舍", now - 5L * 60L * 60L * 1000L);
        Item weeklyItem = item(6L, "自行车", "校园出行", "骑行", "东院", now - 5L * 24L * 60L * 60L * 1000L);

        assertEquals("最新上新", DiscoveryUiMapper.buildLatestReason(freshItem, now));
        assertEquals("今日新发布", DiscoveryUiMapper.buildLatestReason(todayItem, now));
        assertEquals("本周新发布", DiscoveryUiMapper.buildLatestReason(weeklyItem, now));
    }

    @Test
    public void formatRelativeTime_outputsReadableChineseLabels() {
        long now = 10L * 24L * 60L * 60L * 1000L;

        assertEquals("刚刚", DiscoveryUiMapper.formatRelativeTime(now, now));
        assertEquals("5分钟前", DiscoveryUiMapper.formatRelativeTime(now - 5L * 60L * 1000L, now));
        assertEquals("3小时前", DiscoveryUiMapper.formatRelativeTime(now - 3L * 60L * 60L * 1000L, now));
        assertEquals("2天前", DiscoveryUiMapper.formatRelativeTime(now - 2L * 24L * 60L * 60L * 1000L, now));
        assertFalse(DiscoveryUiMapper.formatRelativeTime(now - 40L * 24L * 60L * 60L * 1000L, now).isEmpty());
    }

    private static Item item(long id, String title, String category, String tags,
                             String location, long createdAt) {
        Item item = new Item();
        item.setId(id);
        item.setTitle(title);
        item.setCategory(category);
        item.setTags(tags);
        item.setLocation(location);
        item.setCreatedAt(createdAt);
        item.setDescription(title + " 的描述");
        item.setSellerName("卖家");
        item.setSellerMajor("计算机科学与技术");
        return item;
    }

    private static User user(String major, String interests) {
        User user = new User();
        user.setMajor(major);
        user.setInterests(interests);
        return user;
    }
}
