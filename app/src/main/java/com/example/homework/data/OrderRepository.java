package com.example.homework.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.example.homework.model.Item;
import com.example.homework.model.OrderRecord;

import java.util.ArrayList;
import java.util.List;

public class OrderRepository {
    public static final String STATUS_PENDING = "待发货";
    public static final String STATUS_SHIPPED = "已发货";
    public static final String STATUS_COMPLETED = "已完成";

    private final AppDatabaseHelper databaseHelper;

    public OrderRepository(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
    }

    public CreateOrderResult createOrder(Item item, long buyerId) {
        if (item == null || item.getId() <= 0L || buyerId <= 0L) {
            return new CreateOrderResult(CreateOrderResult.STATE_UNAVAILABLE, -1L);
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            CreateOrderResult existingOrder = findExistingOrderResult(db, item.getId(), buyerId);
            if (existingOrder != null) {
                return existingOrder;
            }

            ItemOrderSnapshot snapshot = getItemOrderSnapshot(db, item.getId());
            if (snapshot == null || !snapshot.active) {
                return new CreateOrderResult(CreateOrderResult.STATE_UNAVAILABLE, -1L);
            }
            if (snapshot.sellerId == buyerId) {
                return new CreateOrderResult(CreateOrderResult.STATE_OWN_ITEM, -1L);
            }

            long now = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put("item_id", item.getId());
            values.put("buyer_id", buyerId);
            values.put("seller_id", snapshot.sellerId);
            values.put("status", STATUS_PENDING);
            values.put("created_at", now);

            long orderId = db.insertOrThrow(AppDatabaseHelper.TABLE_ORDERS, null, values);
            db.setTransactionSuccessful();
            return new CreateOrderResult(CreateOrderResult.STATE_CREATED, orderId);
        } catch (SQLiteConstraintException exception) {
            CreateOrderResult existingOrder = findExistingOrderResult(db, item.getId(), buyerId);
            if (existingOrder != null) {
                return existingOrder;
            }
            throw exception;
        } finally {
            db.endTransaction();
        }
    }

    private ItemOrderSnapshot getItemOrderSnapshot(SQLiteDatabase db, long itemId) {
        Cursor cursor = db.rawQuery(
                "SELECT seller_id, is_active FROM " + AppDatabaseHelper.TABLE_ITEMS + " WHERE id=?",
                new String[]{String.valueOf(itemId)}
        );
        ItemOrderSnapshot snapshot = null;
        if (cursor.moveToFirst()) {
            snapshot = new ItemOrderSnapshot(
                    cursor.getLong(cursor.getColumnIndexOrThrow("seller_id")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1
            );
        }
        cursor.close();
        return snapshot;
    }

    public OrderRecord getOrderByItemId(long itemId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(baseOrderSql() + " WHERE o.item_id=?",
                new String[]{String.valueOf(itemId)});
        OrderRecord order = null;
        if (cursor.moveToFirst()) {
            order = fromCursor(cursor);
        }
        cursor.close();
        return order;
    }

    public OrderRecord getBuyerOrder(long itemId, long buyerId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(baseOrderSql() + " WHERE o.item_id=? AND o.buyer_id=?",
                new String[]{String.valueOf(itemId), String.valueOf(buyerId)});
        OrderRecord order = null;
        if (cursor.moveToFirst()) {
            order = fromCursor(cursor);
        }
        cursor.close();
        return order;
    }

    public boolean markShipped(long orderId) {
        return updateStatus(orderId, STATUS_PENDING, STATUS_SHIPPED, "shipped_at");
    }

    public boolean markCompleted(long orderId) {
        return updateStatus(orderId, STATUS_SHIPPED, STATUS_COMPLETED, "completed_at");
    }

    public List<OrderRecord> getOrdersByBuyer(long buyerId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                baseOrderSql() + " WHERE o.buyer_id=? ORDER BY o.created_at DESC",
                new String[]{String.valueOf(buyerId)}
        );
        List<OrderRecord> orders = readOrders(cursor);
        cursor.close();
        return orders;
    }

    public List<OrderRecord> getOrdersBySeller(long sellerId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                baseOrderSql() + " WHERE o.seller_id=? ORDER BY o.created_at DESC",
                new String[]{String.valueOf(sellerId)}
        );
        List<OrderRecord> orders = readOrders(cursor);
        cursor.close();
        return orders;
    }

    public int countOrdersByBuyer(long buyerId) {
        return count("buyer_id", buyerId);
    }

    public int countOrdersBySeller(long sellerId) {
        return count("seller_id", sellerId);
    }

    private CreateOrderResult findExistingOrderResult(SQLiteDatabase db, long itemId, long buyerId) {
        Cursor cursor = db.rawQuery(
                "SELECT id, buyer_id FROM " + AppDatabaseHelper.TABLE_ORDERS + " WHERE item_id=?",
                new String[]{String.valueOf(itemId)}
        );
        CreateOrderResult result = null;
        if (cursor.moveToFirst()) {
            long orderId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            long existingBuyerId = cursor.getLong(cursor.getColumnIndexOrThrow("buyer_id"));
            result = new CreateOrderResult(
                    existingBuyerId == buyerId
                            ? CreateOrderResult.STATE_ALREADY_PURCHASED
                            : CreateOrderResult.STATE_SOLD_TO_OTHER,
                    orderId
            );
        }
        cursor.close();
        return result;
    }

    private boolean updateStatus(long orderId, String expectedStatus, String nextStatus, String timeColumn) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", nextStatus);
        values.put(timeColumn, System.currentTimeMillis());
        return db.update(
                AppDatabaseHelper.TABLE_ORDERS,
                values,
                "id=? AND status=?",
                new String[]{String.valueOf(orderId), expectedStatus}
        ) > 0;
    }

    private int count(String column, long userId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + AppDatabaseHelper.TABLE_ORDERS + " WHERE " + column + "=?",
                new String[]{String.valueOf(userId)}
        );
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    private List<OrderRecord> readOrders(Cursor cursor) {
        List<OrderRecord> orders = new ArrayList<>();
        while (cursor.moveToNext()) {
            orders.add(fromCursor(cursor));
        }
        return orders;
    }

    private OrderRecord fromCursor(Cursor cursor) {
        OrderRecord order = new OrderRecord();
        order.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        order.setItemId(cursor.getLong(cursor.getColumnIndexOrThrow("item_id")));
        order.setBuyerId(cursor.getLong(cursor.getColumnIndexOrThrow("buyer_id")));
        order.setSellerId(cursor.getLong(cursor.getColumnIndexOrThrow("seller_id")));
        order.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
        order.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        order.setShippedAt(cursor.getLong(cursor.getColumnIndexOrThrow("shipped_at")));
        order.setCompletedAt(cursor.getLong(cursor.getColumnIndexOrThrow("completed_at")));
        order.setItemTitle(cursor.getString(cursor.getColumnIndexOrThrow("item_title")));
        order.setItemPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("item_price")));
        order.setItemLocation(cursor.getString(cursor.getColumnIndexOrThrow("item_location")));
        order.setItemCondition(cursor.getString(cursor.getColumnIndexOrThrow("item_condition")));
        order.setItemCategory(cursor.getString(cursor.getColumnIndexOrThrow("item_category")));
        order.setItemImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("item_image_url")));
        order.setBuyerName(cursor.getString(cursor.getColumnIndexOrThrow("buyer_name")));
        order.setSellerName(cursor.getString(cursor.getColumnIndexOrThrow("seller_name")));
        return order;
    }

    private String baseOrderSql() {
        return "SELECT o.*, " +
                "i.title AS item_title, i.price AS item_price, i.location AS item_location, " +
                "i.item_condition AS item_condition, i.category AS item_category, i.image_url AS item_image_url, " +
                "buyer.name AS buyer_name, seller.name AS seller_name " +
                "FROM " + AppDatabaseHelper.TABLE_ORDERS + " o " +
                "JOIN " + AppDatabaseHelper.TABLE_ITEMS + " i ON o.item_id = i.id " +
                "JOIN " + AppDatabaseHelper.TABLE_USERS + " buyer ON o.buyer_id = buyer.id " +
                "JOIN " + AppDatabaseHelper.TABLE_USERS + " seller ON o.seller_id = seller.id";
    }

    public static class CreateOrderResult {
        public static final int STATE_CREATED = 1;
        public static final int STATE_ALREADY_PURCHASED = 2;
        public static final int STATE_SOLD_TO_OTHER = 3;
        public static final int STATE_UNAVAILABLE = 4;
        public static final int STATE_OWN_ITEM = 5;

        private final int state;
        private final long orderId;

        public CreateOrderResult(int state, long orderId) {
            this.state = state;
            this.orderId = orderId;
        }

        public int getState() {
            return state;
        }

        public long getOrderId() {
            return orderId;
        }
    }

    private static class ItemOrderSnapshot {
        private final long sellerId;
        private final boolean active;

        private ItemOrderSnapshot(long sellerId, boolean active) {
            this.sellerId = sellerId;
            this.active = active;
        }
    }
}
