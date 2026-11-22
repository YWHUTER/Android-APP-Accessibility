package com.example.homework.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.homework.model.Item;
import com.example.homework.model.User;

import org.junit.Test;

public class ProfileUiMapperTest {
    @Test
    public void buildInterestSummary_usesFallbackForMissingFields() {
        User user = new User();

        String summary = ProfileUiMapper.buildInterestSummary(user);

        assertTrue(summary.contains("未填写"));
    }

    @Test
    public void formatListingMeta_formatsPriceAndCategory() {
        Item item = new Item();
        item.setPrice(66.6);
        item.setCategory("数码设备");

        assertEquals("¥66.60 · 数码设备", ProfileUiMapper.formatListingMeta(item));
    }

    @Test
    public void buildAvatarText_usesFirstCharacter() {
        User user = new User();
        user.setName("王同学");

        assertEquals("王", ProfileUiMapper.buildAvatarText(user));
        assertEquals("我", ProfileUiMapper.buildAvatarText(new User()));
    }
}
