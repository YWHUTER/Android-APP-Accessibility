package com.example.homework;

import com.example.homework.util.A11y;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AmapPlacePickerActivity extends AppCompatActivity {
    public static final String EXTRA_INITIAL_QUERY = "com.example.homework.extra.INITIAL_PLACE_QUERY";
    public static final String EXTRA_SELECTED_ADDRESS = "com.example.homework.extra.SELECTED_ADDRESS";
    public static final String EXTRA_SELECTED_NAME = "com.example.homework.extra.SELECTED_NAME";

    private static final int PAGE_SIZE = 20;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private final List<AmapPlace> places = new ArrayList<>();

    private EditText etKeyword;
    private TextView btnSearch;
    private TextView tvStatus;
    private ArrayAdapter<AmapPlace> adapter;
    private int searchVersion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amap_place_picker);

        etKeyword = findViewById(R.id.et_place_keyword);
        btnSearch = findViewById(R.id.btn_place_search);
        tvStatus = findViewById(R.id.tv_place_status);
        ListView resultList = findViewById(R.id.list_place_results);

        adapter = new ArrayAdapter<AmapPlace>(this, android.R.layout.simple_list_item_2, android.R.id.text1, places) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                AmapPlace place = getItem(position);
                TextView title = view.findViewById(android.R.id.text1);
                TextView subtitle = view.findViewById(android.R.id.text2);
                title.setText(place == null ? "" : place.name);
                subtitle.setText(place == null ? "" : place.getDisplayAddress());
                title.setTextColor(getColorCompat(R.color.text_primary));
                subtitle.setTextColor(getColorCompat(R.color.text_tertiary));
                title.setTextSize(15f);
                subtitle.setTextSize(12f);
                view.setPadding(dp(16), dp(10), dp(16), dp(10));
                return view;
            }
        };
        resultList.setAdapter(adapter);
        resultList.setOnItemClickListener((parent, view, position, id) -> selectPlace(adapter.getItem(position)));

        findViewById(R.id.btn_place_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_place_manual).setOnClickListener(v -> finish());
        btnSearch.setOnClickListener(v -> searchPlaces());

        String initialQuery = getIntent().getStringExtra(EXTRA_INITIAL_QUERY);
        if (!TextUtils.isEmpty(initialQuery)) {
            etKeyword.setText(initialQuery);
            etKeyword.setSelection(etKeyword.getText().length());
        }

        if (TextUtils.isEmpty(BuildConfig.AMAP_WEB_API_KEY)) {
            setStatus("\u8bf7\u5148\u5728 local.properties \u914d\u7f6e amap.webApiKey\uff0c\u6216\u8fd4\u56de\u624b\u52a8\u586b\u5199\u5730\u70b9\u3002");
        } else if (!TextUtils.isEmpty(initialQuery)) {
            searchPlaces();
        }
    }

    private void searchPlaces() {
        if (TextUtils.isEmpty(BuildConfig.AMAP_WEB_API_KEY)) {
            setStatus("\u8bf7\u5148\u5728 local.properties \u914d\u7f6e amap.webApiKey\uff0c\u6216\u8fd4\u56de\u624b\u52a8\u586b\u5199\u5730\u70b9\u3002");
            return;
        }

        String keyword = etKeyword.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            setStatus("\u8bf7\u8f93\u5165\u6821\u56ed\u3001\u697c\u680b\u6216\u5546\u5708\u540d\u79f0\u3002");
            return;
        }

        int requestId = ++searchVersion;
        setLoading(true);
        searchExecutor.execute(() -> {
            SearchResult result;
            try {
                result = requestPlaces(keyword);
            } catch (Exception exception) {
                result = SearchResult.error("\u9ad8\u5fb7\u641c\u7d22\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u7f51\u7edc\u6216 Key \u914d\u7f6e\u3002");
            }

            SearchResult finalResult = result;
            mainHandler.post(() -> {
                if (requestId != searchVersion) {
                    return;
                }
                setLoading(false);
                places.clear();
                places.addAll(finalResult.places);
                adapter.notifyDataSetChanged();
                setStatus(finalResult.message);
            });
        });
    }

    private SearchResult requestPlaces(String keyword) throws Exception {
        String urlText = "https://restapi.amap.com/v3/place/text"
                + "?key=" + encode(BuildConfig.AMAP_WEB_API_KEY)
                + "&keywords=" + encode(keyword)
                + "&offset=" + PAGE_SIZE
                + "&page=1"
                + "&extensions=base"
                + "&output=JSON";

        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);

        try {
            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String body = readAll(stream);
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("HTTP " + statusCode);
            }
            JSONObject json = new JSONObject(body);
            if (!"1".equals(json.optString("status"))) {
                return SearchResult.error(readableInfo(json));
            }

            List<AmapPlace> results = new ArrayList<>();
            JSONArray pois = json.optJSONArray("pois");
            if (pois != null) {
                for (int index = 0; index < pois.length(); index++) {
                    JSONObject poi = pois.optJSONObject(index);
                    if (poi == null) {
                        continue;
                    }
                    AmapPlace place = AmapPlace.fromJson(poi);
                    if (!TextUtils.isEmpty(place.name)) {
                        results.add(place);
                    }
                }
            }
            if (results.isEmpty()) {
                return SearchResult.success(results, "\u6ca1\u6709\u627e\u5230\u76f8\u5173\u5730\u70b9\uff0c\u6362\u4e2a\u5173\u952e\u8bcd\u8bd5\u8bd5\u3002");
            }
            return SearchResult.success(results, "\u5df2\u627e\u5230 " + results.size() + " \u4e2a\u5730\u70b9\uff0c\u70b9\u51fb\u5373\u53ef\u56de\u586b\u3002");
        } finally {
            connection.disconnect();
        }
    }

    private void selectPlace(@Nullable AmapPlace place) {
        if (place == null) {
            return;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_SELECTED_NAME, place.name);
        data.putExtra(EXTRA_SELECTED_ADDRESS, place.toSelectedAddress());
        setResult(Activity.RESULT_OK, data);
        A11y.toast(this, "\u5df2\u9009\u62e9\u5730\u70b9");
        finish();
    }

    private void setLoading(boolean loading) {
        btnSearch.setEnabled(!loading);
        btnSearch.setAlpha(loading ? 0.62f : 1f);
        btnSearch.setText(loading ? "\u641c\u7d22\u4e2d" : "\u641c\u7d22");
    }

    private void setStatus(String message) {
        tvStatus.setText(message);
    }

    private int getColorCompat(int resId) {
        return getResources().getColor(resId, getTheme());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String readableInfo(JSONObject json) {
        String info = json.optString("info");
        if (TextUtils.isEmpty(info)) {
            return "\u9ad8\u5fb7\u8fd4\u56de\u5f02\u5e38\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\u3002";
        }
        return "\u9ad8\u5fb7\u8fd4\u56de\uff1a" + info;
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    private static String readAll(@Nullable InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    @Override
    protected void onDestroy() {
        searchVersion++;
        searchExecutor.shutdownNow();
        super.onDestroy();
    }

    private static class SearchResult {
        private final List<AmapPlace> places;
        private final String message;

        private SearchResult(List<AmapPlace> places, String message) {
            this.places = places;
            this.message = message;
        }

        private static SearchResult success(List<AmapPlace> places, String message) {
            return new SearchResult(places, message);
        }

        private static SearchResult error(String message) {
            return new SearchResult(new ArrayList<>(), message);
        }
    }

    private static class AmapPlace {
        private final String name;
        private final String address;
        private final String district;

        private AmapPlace(String name, String address, String district) {
            this.name = name;
            this.address = address;
            this.district = district;
        }

        private static AmapPlace fromJson(JSONObject poi) {
            String district = joinDistinct(
                    readJsonText(poi, "pname"),
                    readJsonText(poi, "cityname"),
                    readJsonText(poi, "adname")
            );
            return new AmapPlace(
                    readJsonText(poi, "name"),
                    readJsonText(poi, "address"),
                    district
            );
        }

        private String getDisplayAddress() {
            if (TextUtils.isEmpty(address)) {
                return district;
            }
            if (TextUtils.isEmpty(district) || address.contains(district)) {
                return address;
            }
            return district + " \u00b7 " + address;
        }

        private String toSelectedAddress() {
            String displayAddress = getDisplayAddress();
            if (TextUtils.isEmpty(displayAddress)) {
                return name;
            }
            return name + " \u00b7 " + displayAddress;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    private static String readJsonText(JSONObject object, String key) {
        Object value = object.opt(key);
        if (value == null || value == JSONObject.NULL || value instanceof JSONArray) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private static String joinDistinct(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (TextUtils.isEmpty(value) || builder.toString().contains(value)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
