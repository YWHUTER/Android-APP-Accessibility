package com.example.homework.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.homework.data.CartRepository;
import com.example.homework.model.Item;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CartUiMapperTest {
    @Test
    public void buildSummary_countsSelectedItemsAndTotal() {
        Item textbook = item(1L, 11L, "教材", 18.5);
        Item keyboard = item(2L, 11L, "数码", 99.0);
        Item lamp = item(3L, 22L, "宿舍", 36.0);
        Set<Long> selectedIds = new HashSet<>(Arrays.asList(1L, 3L));

        CartUiMapper.CartSummary summary = CartUiMapper.buildSummary(
                Arrays.asList(textbook, keyboard, lamp),
                selectedIds
        );

        assertEquals("当前有 3 件商品待结算，勾选后才会计入总价。", summary.getSubtitle());
        assertEquals("合计 · 已选 2 件", summary.getTotalLabel());
        assertEquals("¥54.50", summary.getTotalPrice());
        assertEquals("结算(2)", summary.getCheckoutLabel());
        assertTrue(summary.isCheckoutEnabled());
        assertFalse(summary.isSelectAllChecked());
    }

    @Test
    public void buildSummary_emptyCartDisablesActions() {
        CartUiMapper.CartSummary summary = CartUiMapper.buildSummary(
                Collections.emptyList(),
                Collections.emptySet()
        );

        assertEquals("还没有加入待结算的商品，去发现页逛一逛吧。", summary.getSubtitle());
        assertEquals("合计 · 已选 0 件", summary.getTotalLabel());
        assertEquals("¥0.00", summary.getTotalPrice());
        assertEquals("结算(0)", summary.getCheckoutLabel());
        assertFalse(summary.isCheckoutEnabled());
        assertFalse(summary.isSelectAllEnabled());
    }

    @Test
    public void buildRows_insertsSellerHeadersAndSelectionState() {
        Item first = item(1L, 10L, "教材", 20.0);
        first.setSellerName("卖家甲");
        Item second = item(2L, 10L, "数码", 30.0);
        second.setSellerName("卖家甲");
        Item third = item(3L, 20L, "宿舍", 40.0);
        third.setSellerName("卖家乙");

        List<CartUiMapper.RowModel> rows = CartUiMapper.buildRows(
                Arrays.asList(first, second, third),
                new HashSet<>(Arrays.asList(2L, 3L))
        );

        assertEquals(5, rows.size());
        assertTrue(rows.get(0) instanceof CartUiMapper.SellerHeaderRow);
        assertTrue(rows.get(1) instanceof CartUiMapper.ItemRow);
        assertTrue(rows.get(3) instanceof CartUiMapper.SellerHeaderRow);
        assertFalse(((CartUiMapper.ItemRow) rows.get(1)).isSelected());
        assertTrue(((CartUiMapper.ItemRow) rows.get(2)).isSelected());
    }

    @Test
    public void buildCheckoutMessage_combinesAllResultStates() {
        CartRepository.CheckoutResult result = new CartRepository.CheckoutResult(2, 1, 3);

        String message = CartUiMapper.buildCheckoutMessage(result);

        assertTrue(message.contains("2 笔订单"));
        assertTrue(message.contains("1 件商品你已经下过单了"));
        assertTrue(message.contains("3 件商品当前无法下单"));
    }

    private static Item item(long id, long sellerId, String category, double price) {
        Item item = new Item();
        item.setId(id);
        item.setSellerId(sellerId);
        item.setCategory(category);
        item.setPrice(price);
        item.setTitle(category + "商品");
        item.setLocation("教学楼A");
        item.setCondition("九成新");
        return item;
    }
}
