package com.example.homework.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.homework.model.Post;

import java.util.ArrayList;
import java.util.List;

public class CommunityRepository {
    private final AppDatabaseHelper databaseHelper;

    public CommunityRepository(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
    }

    public List<Post> getAllPosts() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT p.*, u.name AS user_name, u.major AS user_major " +
                        "FROM " + AppDatabaseHelper.TABLE_POSTS + " p " +
                        "JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON p.user_id = u.id " +
                        "ORDER BY p.created_at DESC",
                null
        );
        List<Post> posts = new ArrayList<>();
        while (cursor.moveToNext()) {
            posts.add(fromCursor(cursor));
        }
        cursor.close();
        return posts;
    }

    public long addPost(long userId, String content, String imageUrl) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("content", content);
        values.put("image_url", imageUrl);
        values.put("created_at", System.currentTimeMillis());
        return db.insert(AppDatabaseHelper.TABLE_POSTS, null, values);
    }

    public void deletePost(long postId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(AppDatabaseHelper.TABLE_POSTS, "id=?", new String[]{String.valueOf(postId)});
    }

    public List<Post> getPostsByUser(long userId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT p.*, u.name AS user_name, u.major AS user_major " +
                        "FROM " + AppDatabaseHelper.TABLE_POSTS + " p " +
                        "JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON p.user_id = u.id " +
                        "WHERE p.user_id=? ORDER BY p.created_at DESC",
                new String[]{String.valueOf(userId)}
        );
        List<Post> posts = new ArrayList<>();
        while (cursor.moveToNext()) {
            posts.add(fromCursor(cursor));
        }
        cursor.close();
        return posts;
    }

    private Post fromCursor(Cursor cursor) {
        Post post = new Post();
        post.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        post.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow("user_id")));
        post.setUserName(cursor.getString(cursor.getColumnIndexOrThrow("user_name")));
        post.setUserMajor(cursor.getString(cursor.getColumnIndexOrThrow("user_major")));
        post.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
        post.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("image_url")));
        post.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        return post;
    }
}
