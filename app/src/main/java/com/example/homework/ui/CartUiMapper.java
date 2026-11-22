package com.example.homework.ui;

import com.example.homework.data.CartRepository;
import com.example.homework.model.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CartUiMapper {
    public static final int VIEW_TYPE_SELLER_HEADER = 1;
    public static final int VIEW_TYPE_ITEM = 2;

    private CartUiMapper() {
    }

    public static CartSummary buildSummary(List<Item> items, Set<Long> selectedItemIds) {
        int selectedCount = 0;
        double totalPrice = 0d;
        for (Item item : items) {
            if (selectedItemIds.contains(item.getId())) {
                selectedCount++;
                totalPrice += item.getPrice();
            }
        }

        return new CartSummary(
                buildSubtitle(items.size()),
                String.format(Locale.getDefault(), "合计 · 已选 %d 件", selectedCount),
                formatPrice(totalPrice),
                String.format(Locale.getDefault(), "结算(%d)", selectedCount),
                selectedCount > 0,
                !items.isEmpty() && selectedCount == items.size(),
                !items.isEmpty(),
                selectedCount,
                totalPrice
        );
    }

    public static List<RowModel> buildRows(List<Item> items, Set<Long> selectedItemIds) {
        Map<Long, List<Item>> groupedItems = new LinkedHashMap<>();
        for (Item item : items) {
            List<Item> sellerItems = groupedItems.get(item.getSellerId());
            if (sellerItems == null) {
                sellerItems = new ArrayList<>();
                groupedItems.put(item.getSellerId(), sellerItems);
            }
            sellerItems.add(item);
        }

        List<RowModel> rows = new ArrayList<>();
        for (Map.Entry<Long, List<Item>> entry : groupedItems.entrySet()) {
            List<Item> sellerItems = entry.getValue();
            if (sellerItems.isEmpty()) {
                continue;
            }

            Item anchor = sellerItems.get(0);
            rows.add(new SellerHeaderRow(
                    entry.getKey(),
                    safeText(anchor.getSellerName(), "校园卖家"),
                    buildSectionMeta(anchor, sellerItems.size())
            ));

            for (Item item : sellerItems) {
                rows.add(new ItemRow(item, selectedItemIds.contains(item.getId())));
            }
        }
        return rows;
    }

    public static String buildCheckoutMessage(CartRepository.CheckoutResult result) {
        StringBuilder builder = new StringBuilder();
        if (result.getCreatedCount() > 0) {
            builder.append(String.format(Locale.getDefault(), "已为你生成 %d 笔订单。", result.getCreatedCount()));
        }
        if (result.getAlreadyPurchasedCount() > 0) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(String.format(
                    Locale.getDefault(),
                    "%d 件商品你已经下过单了，系统已自动从购物车移除。",
                    result.getAlreadyPurchasedCount()
            ));
        }
        if (result.getUnavailableCount() > 0) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(String.format(
                    Locale.getDefault(),
                    "%d 件商品当前无法下单，系统已自动清理。",
                    result.getUnavailableCount()
            ));
        }
        return builder.toString();
    }

    public static String buildSectionMeta(Item item, int count) {
        String major = safeText(item.getSellerMajor(), "校内卖家");
        return String.format(Locale.getDefault(), "%s · %d 件商品", major, count);
    }

    public static String formatPrice(double price) {
        return String.format(Locale.getDefault(), "¥%.2f", price);
    }

    public static String formatItemMeta(Item item) {
        return safeText(item.getCategory(), "校园闲置") + " · " + shortLocation(item.getLocation());
    }

    public static String shortLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return "校内交易";
        }
        return location.length() <= 8 ? location : location.substring(0, 8);
    }

    public static String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    public static String buildSubtitle(int itemCount) {
        if (itemCount == 0) {
            return "还没有加入待结算的商品，去发现页逛一逛吧。";
        }
        return String.format(
                Locale.getDefault(),
                "当前有 %d 件商品待结算，勾选后才会计入总价。",
                itemCount
        );
    }

    public abstract static class RowModel {
        private final long stableId;
        private final int viewType;

        RowModel(long stableId, int viewType) {
            this.stableId = stableId;
            this.viewType = viewType;
        }

        public long getStableId() {
            return stableId;
        }

        public int getViewType() {
            return viewType;
        }
    }

    public static final class SellerHeaderRow extends RowModel {
        private final long sellerId;
        private final String title;
        private final String meta;

        public SellerHeaderRow(long sellerId, String title, String meta) {
            super(-(sellerId + 1L), VIEW_TYPE_SELLER_HEADER);
            this.sellerId = sellerId;
            this.title = title;
            this.meta = meta;
        }

        public long getSellerId() {
            return sellerId;
        }

        public String getTitle() {
            return title;
        }

        public String getMeta() {
            return meta;
        }
    }

    public static final class ItemRow extends RowModel {
        private final Item item;
        private final boolean selected;

        public ItemRow(Item item, boolean selected) {
            super(item.getId(), VIEW_TYPE_ITEM);
            this.item = item;
            this.selected = selected;
        }

        public Item getItem() {
            return item;
        }

        public boolean isSelected() {
            return selected;
        }
    }

    public static final class CartSummary {
        private final String subtitle;
        private final String totalLabel;
        private final String totalPrice;
        private final String checkoutLabel;
        private final boolean checkoutEnabled;
        private final boolean selectAllChecked;
        private final boolean selectAllEnabled;
        private final int selectedCount;
        private final double selectedTotal;

        public CartSummary(String subtitle, String totalLabel, String totalPrice, String checkoutLabel,
                           boolean checkoutEnabled, boolean selectAllChecked, boolean selectAllEnabled,
                           int selectedCount, double selectedTotal) {
            this.subtitle = subtitle;
            this.totalLabel = totalLabel;
            this.totalPrice = totalPrice;
            this.checkoutLabel = checkoutLabel;
            this.checkoutEnabled = checkoutEnabled;
            this.selectAllChecked = selectAllChecked;
            this.selectAllEnabled = selectAllEnabled;
            this.selectedCount = selectedCount;
            this.selectedTotal = selectedTotal;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public String getTotalLabel() {
            return totalLabel;
        }

        public String getTotalPrice() {
            return totalPrice;
        }

        public String getCheckoutLabel() {
            return checkoutLabel;
        }

        public boolean isCheckoutEnabled() {
            return checkoutEnabled;
        }

        public boolean isSelectAllChecked() {
            return selectAllChecked;
        }

        public boolean isSelectAllEnabled() {
            return selectAllEnabled;
        }

        public int getSelectedCount() {
            return selectedCount;
        }

        public double getSelectedTotal() {
            return selectedTotal;
        }
    }
}
