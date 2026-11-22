package com.example.homework.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.homework.model.User;
import com.example.homework.util.PasswordSecurity;

public class UserRepository {
    private final AppDatabaseHelper databaseHelper;

    public UserRepository(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
    }

    public long register(User user) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", user.getName());
        values.put("username", user.getUsername());
        values.put("password", PasswordSecurity.hash(user.getPassword()));
        values.put("major", user.getMajor());
        values.put("interests", user.getInterests());
        values.put("bio", user.getBio());
        values.put("student_id_image_url", user.getStudentIdImageUrl());
        values.put("avatar_image_url", user.getAvatarImageUrl());
        values.put("verified_at", user.getVerifiedAt());
        return db.insert(AppDatabaseHelper.TABLE_USERS, null, values);
    }

    public User login(String username, String password) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + AppDatabaseHelper.TABLE_USERS + " WHERE username=?",
                new String[]{username}
        );
        User user = null;
        if (cursor.moveToFirst()) {
            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            if (PasswordSecurity.matches(password, storedPassword)) {
                user = fromCursor(cursor);
                if (PasswordSecurity.isLegacyPlaintext(storedPassword)) {
                    ContentValues values = new ContentValues();
                    values.put("password", PasswordSecurity.hash(password));
                    db.update(AppDatabaseHelper.TABLE_USERS, values, "id=?",
                            new String[]{String.valueOf(user.getId())});
                    user.setPassword(values.getAsString("password"));
                }
            }
        }
        cursor.close();
        return user;
    }

    public boolean usernameExists(String username) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM " + AppDatabaseHelper.TABLE_USERS + " WHERE username=?",
                new String[]{username}
        );
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public User getByUsername(String username) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + AppDatabaseHelper.TABLE_USERS + " WHERE username=?",
                new String[]{username}
        );
        User user = null;
        if (cursor.moveToFirst()) {
            user = fromCursor(cursor);
        }
        cursor.close();
        return user;
    }

    public User getById(long id) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + AppDatabaseHelper.TABLE_USERS + " WHERE id=?",
                new String[]{String.valueOf(id)}
        );
        User user = null;
        if (cursor.moveToFirst()) {
            user = fromCursor(cursor);
        }
        cursor.close();
        return user;
    }

    public boolean updateProfile(User user) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", user.getName());
        values.put("major", user.getMajor());
        values.put("interests", user.getInterests());
        values.put("bio", user.getBio());
        int rows = db.update(AppDatabaseHelper.TABLE_USERS, values, "id=?",
                new String[]{String.valueOf(user.getId())});
        return rows > 0;
    }

    public boolean updateAvatarImage(long userId, String avatarImageUrl) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("avatar_image_url", avatarImageUrl);
        int rows = db.update(
                AppDatabaseHelper.TABLE_USERS,
                values,
                "id=?",
                new String[]{String.valueOf(userId)}
        );
        return rows > 0;
    }

    public boolean updatePassword(long userId, String oldPassword, String newPassword) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT password FROM " + AppDatabaseHelper.TABLE_USERS + " WHERE id=?",
                new String[]{String.valueOf(userId)}
        );
        boolean valid = false;
        if (cursor.moveToFirst()) {
            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            valid = PasswordSecurity.matches(oldPassword, storedPassword);
        }
        cursor.close();
        if (!valid) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("password", PasswordSecurity.hash(newPassword));
        return db.update(AppDatabaseHelper.TABLE_USERS, values, "id=?",
                new String[]{String.valueOf(userId)}) > 0;
    }

    public boolean resetPassword(String username, String newPassword) {
        if (username == null || username.trim().isEmpty()
                || newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", PasswordSecurity.hash(newPassword));
        int rows = db.update(
                AppDatabaseHelper.TABLE_USERS,
                values,
                "username=?",
                new String[]{username.trim()}
        );
        return rows > 0;
    }

    private User fromCursor(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        user.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow("username")));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow("password")));
        user.setMajor(cursor.getString(cursor.getColumnIndexOrThrow("major")));
        user.setInterests(cursor.getString(cursor.getColumnIndexOrThrow("interests")));
        user.setBio(cursor.getString(cursor.getColumnIndexOrThrow("bio")));
        user.setStudentIdImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("student_id_image_url")));
        user.setAvatarImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("avatar_image_url")));
        user.setVerifiedAt(cursor.getLong(cursor.getColumnIndexOrThrow("verified_at")));
        return user;
    }
}
