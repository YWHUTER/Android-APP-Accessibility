package com.example.homework.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.example.homework.model.Item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CartRepository {
    private final AppDatabaseHelper databaseHelper;
    private final OrderRepository orderRepository;

    public CartRepository(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
        orderRepository = new OrderRepository(context);
    }

    public AddToCartResult addItem(long userId, Item item) {
        if (userId <= 0L || item == null || item.getId() <= 0L) {
            return new AddToCartResult(AddToCartResult.STATE_MISSING, -1L);
        }

        Availability availability = getAvailability(item.getId());
        if (!availability.exists) {
            return new AddToCartResult(AddToCartResult.STATE_MISSING, item.getId());
        }
        if (availability.sellerId == userId) {
            return new AddToCartResult(AddToCartResult.STATE_OWN_ITEM, item.getId());
        }
        if (!availability.available) {
            return new AddToCartResult(AddToCartResult.STATE_UNAVAILABLE, item.getId());
        }
        if (isInCart(userId, item.getId())) {
            return new AddToCartResult(AddToCartResult.STATE_ALREADY_IN_CART, item.getId());
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("item_id", item.getId());
        values.put("added_at", System.currentTimeMillis());
        try {
            db.insertOrThrow(AppDatabaseHelper.TABLE_CART_ITEMS, null, values);
            return new AddToCartResult(AddToCartResult.STATE_ADDED, item.getId());
        } catch (SQLiteConstraintException exception) {
            return new AddToCartResult(AddToCartResult.STATE_ALREADY_IN_CART, item.getId());
        }
    }

    public boolean isInCart(long userId, long itemId) {
        if (userId <= 0L || itemId <= 0L) {
            return false;
        }
        pruneInvalidItems(userId);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + AppDatabaseHelper.TABLE_CART_ITEMS + " WHERE user_id=? AND item_id=?",
                new String[]{String.valueOf(userId), String.valueOf(itemId)}
        );
        boolean inCart = false;
        if (cursor.moveToFirst()) {
            inCart = cursor.getInt(0) > 0;
        }
        cursor.close();
        return inCart;
    }

    public List<Item> getCartItems(long userId) {
        if (userId <= 0L) {
            return new ArrayList<>();
        }
        pruneInvalidItems(userId);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT i.*, u.name AS seller_name, u.major AS seller_major " +
                        "FROM " + AppDatabaseHelper.TABLE_CART_ITEMS + " c " +
                        "JOIN " + AppDatabaseHelper.TABLE_ITEMS + " i ON c.item_id = i.id " +
                        "JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON i.seller_id = u.id " +
                        "WHERE c.user_id=? " +
                        "ORDER BY c.added_at DESC",
                new String[]{String.valueOf(userId)}
        );
        List<Item> items = new ArrayList<>();
        while (cursor.moveToNext()) {
            items.add(fromCursor(cursor));
        }
        cursor.close();
        return items;
    }

    public void removeItem(long userId, long itemId) {
        if (userId <= 0L || itemId <= 0L) {
            return;
        }
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(
                AppDatabaseHelper.TABLE_CART_ITEMS,
                "user_id=? AND item_id=?",
                new String[]{String.valueOf(userId), String.valueOf(itemId)}
        );
    }

    public CheckoutResult checkout(long userId) {
        if (userId <= 0L) {
            return new CheckoutResult(0, 0, 0);
        }
        List<Long> itemIds = new ArrayList<>();
        for (Item item : getCartItems(userId)) {
            itemIds.add(item.getId());
        }
        return checkout(userId, itemIds);
    }

    public CheckoutResult checkout(long userId, List<Long> selectedItemIds) {
        if (userId <= 0L || selectedItemIds == null || selectedItemIds.isEmpty()) {
            return new CheckoutResult(0, 0, 0);
        }

        List<Item> items = getCartItems(userId);
        Set<Long> targetIds = new HashSet<>();
        for (Long itemId : selectedItemIds) {
            if (itemId != null && itemId > 0L) {
                targetIds.add(itemId);
            }
        }
        if (targetIds.isEmpty()) {
            return new CheckoutResult(0, 0, 0);
        }

        int createdCount = 0;
        int alreadyPurchasedCount = 0;
        int unavailableCount = 0;

        for (Item item : items) {
            if (!targetIds.remove(item.getId())) {
                continue;
            }

            Availability availability = getAvailability(item.getId());
            if (!availability.available || availability.sellerId == userId) {
                unavailableCount++;
                removeItem(userId, item.getId());
                continue;
            }

            OrderRepository.CreateOrderResult result = orderRepository.createOrder(item, userId);
            if (result.getState() == OrderRepository.CreateOrderResult.STATE_CREATED) {
                createdCount++;
                removeItem(userId, item.getId());
            } else if (result.getState() == OrderRepository.CreateOrderResult.STATE_ALREADY_PURCHASED) {
                alreadyPurchasedCount++;
                removeItem(userId, item.getId());
            } else {
                unavailableCount++;
                removeItem(userId, item.getId());
            }
        }

        unavailableCount += targetIds.size();
        return new CheckoutResult(createdCount, alreadyPurchasedCount, unavailableCount);
    }

    public int getCartItemCount(long userId) {
        if (userId <= 0L) {
            return 0;
        }

        pruneInvalidItems(userId);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + AppDatabaseHelper.TABLE_CART_ITEMS + " WHERE user_id=?",
                new String[]{String.valueOf(userId)}
        );
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    private int pruneInvalidItems(long userId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT c.item_id " +
                        "FROM " + AppDatabaseHelper.TABLE_CART_ITEMS + " c " +
                        "LEFT JOIN " + AppDatabaseHelper.TABLE_ITEMS + " i ON c.item_id = i.id " +
                        "LEFT JOIN " + AppDatabaseHelper.TABLE_ORDERS + " o ON c.item_id = o.item_id " +
                        "WHERE c.user_id=? AND (i.id IS NULL OR i.is_active<>1 OR o.id IS NOT NULL)",
                new String[]{String.valueOf(userId)}
        );

        List<Long> invalidItemIds = new ArrayList<>();
        while (cursor.moveToNext()) {
            invalidItemIds.add(cursor.getLong(0));
        }
        cursor.close();

        for (Long itemId : invalidItemIds) {
            removeItem(userId, itemId);
        }
        return invalidItemIds.size();
    }

    private Availability getAvailability(long itemId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT i.seller_id, i.is_active, " +
                        "CASE WHEN EXISTS(" +
                        "SELECT 1 FROM " + AppDatabaseHelper.TABLE_ORDERS + " o WHERE o.item_id = i.id" +
                        ") THEN 1 ELSE 0 END AS has_order " +
                        "FROM " + AppDatabaseHelper.TABLE_ITEMS + " i WHERE i.id=?",
                new String[]{String.valueOf(itemId)}
        );

        Availability availability = new Availability();
        if (cursor.moveToFirst()) {
            availability.exists = true;
            availability.sellerId = cursor.getLong(cursor.getColumnIndexOrThrow("seller_id"));
            availability.available = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1
                    && cursor.getInt(cursor.getColumnIndexOrThrow("has_order")) == 0;
        }
        cursor.close();
        return availability;
    }

    private Item fromCursor(Cursor cursor) {
        Item item = new Item();
        item.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        item.setSellerId(cursor.getLong(cursor.getColumnIndexOrThrow("seller_id")));
        item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        item.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
        item.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("price")));
        item.setLocation(cursor.getString(cursor.getColumnIndexOrThrow("location")));
        item.setCondition(cursor.getString(cursor.getColumnIndexOrThrow("item_condition")));
        item.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("image_url")));
        item.setTags(cursor.getString(cursor.getColumnIndexOrThrow("tags")));
        item.setPreferredMajor(cursor.getString(cursor.getColumnIndexOrThrow("preferred_major")));
        item.setCampusPickupOnly(cursor.getInt(cursor.getColumnIndexOrThrow("campus_pickup_only")));
        item.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        item.setSellerName(cursor.getString(cursor.getColumnIndexOrThrow("seller_name")));
        item.setSellerMajor(cursor.getString(cursor.getColumnIndexOrThrow("seller_major")));
        return item;
    }

    private static class Availability {
        boolean exists;
        boolean available;
        long sellerId;
    }

    public static class AddToCartResult {
        public static final int STATE_ADDED = 1;
        public static final int STATE_ALREADY_IN_CART = 2;
        public static final int STATE_UNAVAILABLE = 3;
        public static final int STATE_OWN_ITEM = 4;
        public static final int STATE_MISSING = 5;

        private final int state;
        private final long itemId;

        public AddToCartResult(int state, long itemId) {
            this.state = state;
            this.itemId = itemId;
        }

        public int getState() {
            return state;
        }

        public long getItemId() {
            return itemId;
        }
    }

    public static class CheckoutResult {
        private final int createdCount;
        private final int alreadyPurchasedCount;
        private final int unavailableCount;

        public CheckoutResult(int createdCount, int alreadyPurchasedCount, int unavailableCount) {
            this.createdCount = createdCount;
            this.alreadyPurchasedCount = alreadyPurchasedCount;
            this.unavailableCount = unavailableCount;
        }

        public int getCreatedCount() {
            return createdCount;
        }

        public int getAlreadyPurchasedCount() {
            return alreadyPurchasedCount;
        }

        public int getUnavailableCount() {
            return unavailableCount;
        }
    }
}
