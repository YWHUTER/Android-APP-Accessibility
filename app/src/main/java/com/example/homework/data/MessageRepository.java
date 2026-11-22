package com.example.homework.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.homework.model.ChatMessage;
import com.example.homework.model.ConversationSummary;

import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private final AppDatabaseHelper databaseHelper;

    public MessageRepository(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
    }

    public List<ConversationSummary> getConversationSummaries(long userId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT c.id, c.item_id, c.last_message, c.last_message_at, " +
                        "COALESCE(i.title, '已下架商品') AS item_title, COALESCE(i.price, 0) AS item_price, i.image_url AS item_image_url, " +
                        "CASE WHEN c.buyer_id=? THEN s.id ELSE b.id END AS other_user_id, " +
                        "CASE WHEN c.buyer_id=? THEN s.name ELSE b.name END AS other_user_name, " +
                        "CASE WHEN c.buyer_id=? THEN s.major ELSE b.major END AS other_user_major, " +
                        "CASE WHEN c.buyer_id=? THEN 1 ELSE 0 END AS is_buyer, " +
                        "(SELECT COUNT(*) FROM " + AppDatabaseHelper.TABLE_MESSAGES + " m2 " +
                        "WHERE m2.conversation_id = c.id AND m2.sender_id <> ? AND COALESCE(m2.is_read, 0) = 0) AS unread_count " +
                        "FROM " + AppDatabaseHelper.TABLE_CONVERSATIONS + " c " +
                        "LEFT JOIN " + AppDatabaseHelper.TABLE_ITEMS + " i ON c.item_id = i.id " +
                        "JOIN " + AppDatabaseHelper.TABLE_USERS + " b ON c.buyer_id = b.id " +
                        "JOIN " + AppDatabaseHelper.TABLE_USERS + " s ON c.seller_id = s.id " +
                        "WHERE (c.buyer_id=? OR c.seller_id=?) AND COALESCE(c.last_message, '') <> '' " +
                        "ORDER BY c.last_message_at DESC",
                new String[]{
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(userId)
                }
        );
        List<ConversationSummary> conversations = new ArrayList<>();
        while (cursor.moveToNext()) {
            conversations.add(summaryFromCursor(cursor));
        }
        cursor.close();
        return conversations;
    }

    public int getUnreadMessageCount(long userId) {
        if (userId <= 0L) {
            return 0;
        }

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + AppDatabaseHelper.TABLE_MESSAGES + " m " +
                        "JOIN " + AppDatabaseHelper.TABLE_CONVERSATIONS + " c ON m.conversation_id = c.id " +
                        "WHERE (c.buyer_id=? OR c.seller_id=?) " +
                        "AND m.sender_id<>? AND COALESCE(m.is_read, 0)=0",
                new String[]{
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(userId)
                }
        );
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public ConversationSummary getConversation(long conversationId, long userId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT c.id, c.item_id, c.last_message, c.last_message_at, " +
                        "COALESCE(i.title, '已下架商品') AS item_title, COALESCE(i.price, 0) AS item_price, i.image_url AS item_image_url, " +
                        "CASE WHEN c.buyer_id=? THEN s.id ELSE b.id END AS other_user_id, " +
                        "CASE WHEN c.buyer_id=? THEN s.name ELSE b.name END AS other_user_name, " +
                        "CASE WHEN c.buyer_id=? THEN s.major ELSE b.major END AS other_user_major, " +
                        "CASE WHEN c.buyer_id=? THEN 1 ELSE 0 END AS is_buyer, " +
                        "(SELECT COUNT(*) FROM " + AppDatabaseHelper.TABLE_MESSAGES + " m2 " +
                        "WHERE m2.conversation_id = c.id AND m2.sender_id <> ? AND COALESCE(m2.is_read, 0) = 0) AS unread_count " +
                        "FROM " + AppDatabaseHelper.TABLE_CONVERSATIONS + " c " +
                        "LEFT JOIN " + AppDatabaseHelper.TABLE_ITEMS + " i ON c.item_id = i.id " +
                        "JOIN " + AppDatabaseHelper.TABLE_USERS + " b ON c.buyer_id = b.id " +
                        "JOIN " + AppDatabaseHelper.TABLE_USERS + " s ON c.seller_id = s.id " +
                        "WHERE c.id=? AND (c.buyer_id=? OR c.seller_id=?)",
                new String[]{
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(userId),
                        String.valueOf(conversationId),
                        String.valueOf(userId),
                        String.valueOf(userId)
                }
        );
        ConversationSummary summary = null;
        if (cursor.moveToFirst()) {
            summary = summaryFromCursor(cursor);
        }
        cursor.close();
        return summary;
    }

    public long getOrCreateConversation(long itemId, long buyerId, long sellerId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM " + AppDatabaseHelper.TABLE_CONVERSATIONS + " WHERE item_id=? AND buyer_id=? AND seller_id=?",
                new String[]{String.valueOf(itemId), String.valueOf(buyerId), String.valueOf(sellerId)}
        );
        long conversationId = -1L;
        if (cursor.moveToFirst()) {
            conversationId = cursor.getLong(0);
        }
        cursor.close();
        if (conversationId > 0) {
            return conversationId;
        }

        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("item_id", itemId);
        values.put("buyer_id", buyerId);
        values.put("seller_id", sellerId);
        values.put("last_message", "");
        values.put("last_message_at", now);
        return db.insert(AppDatabaseHelper.TABLE_CONVERSATIONS, null, values);
    }

    public List<ChatMessage> getMessages(long conversationId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT m.*, u.name AS sender_name FROM " + AppDatabaseHelper.TABLE_MESSAGES + " m " +
                        "JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON m.sender_id = u.id " +
                        "WHERE m.conversation_id=? ORDER BY m.created_at ASC",
                new String[]{String.valueOf(conversationId)}
        );
        List<ChatMessage> messages = new ArrayList<>();
        while (cursor.moveToNext()) {
            ChatMessage message = new ChatMessage();
            message.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
            message.setConversationId(cursor.getLong(cursor.getColumnIndexOrThrow("conversation_id")));
            message.setSenderId(cursor.getLong(cursor.getColumnIndexOrThrow("sender_id")));
            message.setSenderName(cursor.getString(cursor.getColumnIndexOrThrow("sender_name")));
            message.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
            message.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
            messages.add(message);
        }
        cursor.close();
        return messages;
    }

    public void sendMessage(long conversationId, long senderId, String content) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long now = System.currentTimeMillis();

        ContentValues messageValues = new ContentValues();
        messageValues.put("conversation_id", conversationId);
        messageValues.put("sender_id", senderId);
        messageValues.put("content", content);
        messageValues.put("is_read", 0);
        messageValues.put("created_at", now);
        db.insert(AppDatabaseHelper.TABLE_MESSAGES, null, messageValues);

        ContentValues conversationValues = new ContentValues();
        conversationValues.put("last_message", content);
        conversationValues.put("last_message_at", now);
        db.update(
                AppDatabaseHelper.TABLE_CONVERSATIONS,
                conversationValues,
                "id=?",
                new String[]{String.valueOf(conversationId)}
        );
    }

    public void markConversationAsRead(long conversationId, long readerId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        db.update(
                AppDatabaseHelper.TABLE_MESSAGES,
                values,
                "conversation_id=? AND sender_id<>? AND COALESCE(is_read, 0)=0",
                new String[]{String.valueOf(conversationId), String.valueOf(readerId)}
        );
    }

    public void deleteMessages(long conversationId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(AppDatabaseHelper.TABLE_MESSAGES, "conversation_id=?",
                new String[]{String.valueOf(conversationId)});
        ContentValues values = new ContentValues();
        values.put("last_message", "");
        values.put("last_message_at", System.currentTimeMillis());
        db.update(AppDatabaseHelper.TABLE_CONVERSATIONS, values, "id=?",
                new String[]{String.valueOf(conversationId)});
    }

    public long getConversationItemId(long conversationId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT item_id FROM " + AppDatabaseHelper.TABLE_CONVERSATIONS + " WHERE id=?",
                new String[]{String.valueOf(conversationId)}
        );
        long itemId = -1L;
        if (cursor.moveToFirst()) {
            itemId = cursor.getLong(0);
        }
        cursor.close();
        return itemId;
    }

    private ConversationSummary summaryFromCursor(Cursor cursor) {
        ConversationSummary summary = new ConversationSummary();
        summary.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        summary.setItemId(cursor.getLong(cursor.getColumnIndexOrThrow("item_id")));
        summary.setItemTitle(cursor.getString(cursor.getColumnIndexOrThrow("item_title")));
        summary.setItemImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("item_image_url")));
        summary.setItemPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("item_price")));
        summary.setOtherUserId(cursor.getLong(cursor.getColumnIndexOrThrow("other_user_id")));
        summary.setOtherUserName(cursor.getString(cursor.getColumnIndexOrThrow("other_user_name")));
        summary.setOtherUserMajor(cursor.getString(cursor.getColumnIndexOrThrow("other_user_major")));
        summary.setLastMessage(cursor.getString(cursor.getColumnIndexOrThrow("last_message")));
        summary.setLastMessageAt(cursor.getLong(cursor.getColumnIndexOrThrow("last_message_at")));
        summary.setBuyer(cursor.getInt(cursor.getColumnIndexOrThrow("is_buyer")) == 1);
        summary.setUnreadCount(cursor.getInt(cursor.getColumnIndexOrThrow("unread_count")));
        return summary;
    }
}
