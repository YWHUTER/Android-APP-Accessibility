package com.example.homework.ui;

import static org.junit.Assert.assertEquals;

import com.example.homework.data.OrderRepository;
import com.example.homework.model.OrderRecord;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class OrderListUiMapperTest {
    @Test
    public void filterAndSort_matchesSearchAcrossOrderFields() {
        OrderRecord first = order(1L, "高数教材", 30.0, OrderRepository.STATUS_PENDING);
        first.setItemLocation("图书馆门口");
        first.setSellerName("陈同学");

        OrderRecord second = order(2L, "机械键盘", 120.0, OrderRepository.STATUS_PENDING);
        second.setItemLocation("东院宿舍");
        second.setSellerName("李同学");

        List<OrderRecord> results = OrderListUiMapper.filterAndSort(
                Arrays.asList(first, second),
                "图书馆",
                "",
                true,
                OrderListUiMapper.SortOption.RECENT_ACTIVITY
        );

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
    }

    @Test
    public void filterAndSort_ordersByRecentActivityFirst() {
        OrderRecord createdOnly = order(1L, "教材A", 20.0, OrderRepository.STATUS_PENDING);
        createdOnly.setCreatedAt(100L);

        OrderRecord shipped = order(2L, "教材B", 20.0, OrderRepository.STATUS_SHIPPED);
        shipped.setCreatedAt(90L);
        shipped.setShippedAt(200L);

        OrderRecord completed = order(3L, "教材C", 20.0, OrderRepository.STATUS_COMPLETED);
        completed.setCreatedAt(80L);
        completed.setShippedAt(150L);
        completed.setCompletedAt(300L);

        List<OrderRecord> results = OrderListUiMapper.filterAndSort(
                Arrays.asList(createdOnly, shipped, completed),
                "",
                "",
                true,
                OrderListUiMapper.SortOption.RECENT_ACTIVITY
        );

        assertEquals(Arrays.asList(3L, 2L, 1L), Arrays.asList(
                results.get(0).getId(),
                results.get(1).getId(),
                results.get(2).getId()
        ));
    }

    @Test
    public void filterAndSort_appliesStatusFilterAndPriceSorting() {
        OrderRecord low = order(1L, "低价商品", 15.0, OrderRepository.STATUS_PENDING);
        OrderRecord high = order(2L, "高价商品", 88.0, OrderRepository.STATUS_PENDING);
        OrderRecord shipped = order(3L, "已发货商品", 30.0, OrderRepository.STATUS_SHIPPED);

        List<OrderRecord> results = OrderListUiMapper.filterAndSort(
                Arrays.asList(high, shipped, low),
                "",
                OrderRepository.STATUS_PENDING,
                false,
                OrderListUiMapper.SortOption.PRICE_LOW_TO_HIGH
        );

        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals(2L, results.get(1).getId());
    }

    private static OrderRecord order(long id, String title, double price, String status) {
        OrderRecord order = new OrderRecord();
        order.setId(id);
        order.setItemTitle(title);
        order.setItemPrice(price);
        order.setStatus(status);
        order.setItemCategory("教材");
        order.setItemCondition("九成新");
        order.setItemLocation("校园");
        order.setBuyerName("买家");
        order.setSellerName("卖家");
        return order;
    }
}
