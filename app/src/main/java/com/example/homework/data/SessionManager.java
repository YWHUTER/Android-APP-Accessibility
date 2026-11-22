package com.example.homework.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "marketplace_session";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void login(long userId) {
        preferences.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public void logout() {
        preferences.edit().remove(KEY_USER_ID).apply();
    }

    public long getLoggedInUserId() {
        return preferences.getLong(KEY_USER_ID, -1L);
    }

    public boolean isLoggedIn() {
        return getLoggedInUserId() > 0;
    }
}
