package com.example.homework.ai;

import android.text.TextUtils;

import com.example.homework.BuildConfig;
import com.example.homework.model.Item;
import com.example.homework.model.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeepSeekClient {
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String FALLBACK_MODEL = "deepseek-v4-flash";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 18000;
    private static final int MAX_RECOMMENDATION_ITEMS = 8;
    private static final int MAX_SEARCH_ITEMS = 18;

    public boolean isConfigured() {
        return !TextUtils.isEmpty(BuildConfig.DEEPSEEK_API_KEY);
    }

    public PostDraft generatePostDraft(String title, String description, String category,
                                       String price, String condition, String location)
            throws DeepSeekException {
        String systemPrompt =
                "You write concise, trustworthy listing copy for a campus second-hand marketplace. " +
                        "Return only valid JSON. Use natural Simplified Chinese.";
        String userPrompt =
                "Polish or complete this listing draft. Keep it honest and suitable for students. " +
                        "Return this exact JSON shape: " +
                        "{\"title\":\"short title\",\"description\":\"3 to 5 useful sentences\",\"tags\":\"comma separated tags\"}.\n\n" +
                        "Title: " + safe(title) + "\n" +
                        "Description: " + safe(description) + "\n" +
                        "Category: " + safe(category) + "\n" +
                        "Price: " + safe(price) + "\n" +
                        "Condition: " + safe(condition) + "\n" +
                        "Trade location: " + safe(location);

        JSONObject json = requestJson(systemPrompt, userPrompt, 0.55d, 520);
        return new PostDraft(
                json.optString("title", "").trim(),
                json.optString("description", "").trim(),
                json.optString("tags", "").trim()
        );
    }

    public RecommendationResult generateRecommendations(User user, List<Item> items)
            throws DeepSeekException {
        if (user == null || items == null || items.isEmpty()) {
            return RecommendationResult.empty();
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("profile", buildProfileJson(user));
            payload.put("items", buildItemsJson(items));

            String systemPrompt =
                    "You are a ranking assistant for a campus second-hand marketplace. " +
                            "Rank candidate items for the current student. Return only valid JSON in Simplified Chinese.";
            String userPrompt =
                    "Use the profile and candidate items below. Choose and rank the most relevant items first. " +
                            "Give short reasons that can fit on a small product card. " +
                            "Return this exact JSON shape: " +
                            "{\"summary\":\"one short recommendation summary\",\"items\":[{\"id\":123,\"reason\":\"short reason\"}]}.\n\n" +
                            payload.toString();

            JSONObject json = requestJson(systemPrompt, userPrompt, 0.35d, 620);
            return parseRecommendation(json);
        } catch (JSONException exception) {
            throw new DeepSeekException("Failed to build recommendation request.", exception);
        }
    }

    public SearchResult generateSearchResults(String query, User user, List<Item> items)
            throws DeepSeekException {
        if (TextUtils.isEmpty(query) || items == null || items.isEmpty()) {
            return SearchResult.empty();
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("query", safe(query));
            if (user != null) {
                payload.put("profile", buildProfileJson(user));
            }
            payload.put("items", buildSearchItemsJson(items));

            String systemPrompt =
                    "You are an online search assistant for a campus second-hand marketplace. " +
                            "Understand the student's intent, handle synonyms, and rank candidate items. " +
                            "Return only valid JSON in Simplified Chinese.";
            String userPrompt =
                    "Use the query and candidate items below. Pick only relevant items and rank best matches first. " +
                            "Prefer exact campus marketplace intent over broad guesses. " +
                            "Return this exact JSON shape: " +
                            "{\"rewrittenQuery\":\"short intent\",\"summary\":\"one short search summary\"," +
                            "\"items\":[{\"id\":123,\"reason\":\"short reason\"}]}.\n\n" +
                            payload.toString();

            JSONObject json = requestJson(systemPrompt, userPrompt, 0.25d, 760);
            return parseSearchResult(json);
        } catch (JSONException exception) {
            throw new DeepSeekException("Failed to build search request.", exception);
        }
    }

    public PriceSuggestion generatePriceSuggestion(String title, String description, String category,
                                                   String condition, String location, String currentPrice)
            throws DeepSeekException {
        String systemPrompt =
                "You are a practical pricing assistant for a campus second-hand marketplace. " +
                        "Suggest a fair student-to-student price. Return only valid JSON in Simplified Chinese.";
        String userPrompt =
                "Estimate a reasonable listing price from the draft below. " +
                        "Be conservative: avoid inflated prices, and explain the main factor briefly. " +
                        "Return this exact JSON shape: " +
                        "{\"suggestedPrice\":45,\"minPrice\":40,\"maxPrice\":55,\"reason\":\"short reason\"}.\n\n" +
                        "Title: " + safe(title) + "\n" +
                        "Description: " + safe(description) + "\n" +
                        "Category: " + safe(category) + "\n" +
                        "Condition: " + safe(condition) + "\n" +
                        "Trade location: " + safe(location) + "\n" +
                        "Current price input: " + safe(currentPrice);

        JSONObject json = requestJson(systemPrompt, userPrompt, 0.25d, 360);
        return parsePriceSuggestion(json);
    }

    private JSONObject requestJson(String systemPrompt, String userPrompt, double temperature,
                                   int maxTokens) throws DeepSeekException {
        if (!isConfigured()) {
            throw new DeepSeekException("DeepSeek API key is missing.");
        }

        HttpURLConnection connection = null;
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", modelName());
            requestBody.put("stream", false);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("response_format", new JSONObject().put("type", "json_object"));
            requestBody.put("thinking", new JSONObject().put("type", "disabled"));
            requestBody.put("messages", new JSONArray()
                    .put(message("system", systemPrompt))
                    .put(message("user", userPrompt)));

            byte[] bodyBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + BuildConfig.DEEPSEEK_API_KEY);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bodyBytes);
            }

            int responseCode = connection.getResponseCode();
            String responseText = readStream(responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            if (responseCode < 200 || responseCode >= 300) {
                throw new DeepSeekException("DeepSeek API request failed: " +
                        responseCode + " " + compact(responseText));
            }

            JSONObject responseJson = new JSONObject(responseText);
            JSONArray choices = responseJson.getJSONArray("choices");
            if (choices.length() == 0) {
                throw new DeepSeekException("DeepSeek returned no choices.");
            }

            String content = choices
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content", "");
            if (TextUtils.isEmpty(content)) {
                throw new DeepSeekException("DeepSeek returned an empty message.");
            }
            return parseJsonContent(content);
        } catch (IOException | JSONException exception) {
            throw new DeepSeekException("DeepSeek request failed.", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private RecommendationResult parseRecommendation(JSONObject json) {
        String summary = json.optString("summary", "").trim();
        List<Long> orderedItemIds = new ArrayList<>();
        Map<Long, String> itemReasons = new HashMap<>();

        JSONArray items = json.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                long id = item.optLong("id", -1L);
                if (id <= 0L) {
                    continue;
                }
                orderedItemIds.add(id);
                String reason = item.optString("reason", "").trim();
                if (!TextUtils.isEmpty(reason)) {
                    itemReasons.put(id, reason);
                }
            }
        }

        JSONArray orderedIds = json.optJSONArray("orderedIds");
        if (orderedItemIds.isEmpty() && orderedIds != null) {
            for (int i = 0; i < orderedIds.length(); i++) {
                long id = orderedIds.optLong(i, -1L);
                if (id > 0L) {
                    orderedItemIds.add(id);
                }
            }
        }
        return new RecommendationResult(summary, orderedItemIds, itemReasons);
    }

    private SearchResult parseSearchResult(JSONObject json) {
        String rewrittenQuery = json.optString("rewrittenQuery", "").trim();
        String summary = json.optString("summary", "").trim();
        List<Long> orderedItemIds = new ArrayList<>();
        Map<Long, String> itemReasons = new HashMap<>();

        JSONArray items = json.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                long id = item.optLong("id", -1L);
                if (id <= 0L) {
                    continue;
                }
                orderedItemIds.add(id);
                String reason = item.optString("reason", "").trim();
                if (!TextUtils.isEmpty(reason)) {
                    itemReasons.put(id, reason);
                }
            }
        }
        return new SearchResult(rewrittenQuery, summary, orderedItemIds, itemReasons);
    }

    private PriceSuggestion parsePriceSuggestion(JSONObject json) {
        double suggestedPrice = Math.max(0d, json.optDouble("suggestedPrice", 0d));
        double minPrice = Math.max(0d, json.optDouble("minPrice", suggestedPrice));
        double maxPrice = Math.max(0d, json.optDouble("maxPrice", suggestedPrice));
        if (minPrice > maxPrice) {
            double swap = minPrice;
            minPrice = maxPrice;
            maxPrice = swap;
        }
        if (suggestedPrice <= 0d && maxPrice > 0d) {
            suggestedPrice = (minPrice + maxPrice) / 2d;
        }
        String reason = json.optString("reason", "").trim();
        return new PriceSuggestion(suggestedPrice, minPrice, maxPrice, reason);
    }

    private JSONObject buildProfileJson(User user) throws JSONException {
        JSONObject profile = new JSONObject();
        profile.put("major", safe(user.getMajor()));
        profile.put("interests", safe(user.getInterests()));
        profile.put("bio", safe(user.getBio()));
        return profile;
    }

    private JSONArray buildItemsJson(List<Item> items) throws JSONException {
        JSONArray array = new JSONArray();
        int count = Math.min(items.size(), MAX_RECOMMENDATION_ITEMS);
        for (int i = 0; i < count; i++) {
            Item item = items.get(i);
            JSONObject json = new JSONObject();
            json.put("id", item.getId());
            json.put("title", safe(item.getTitle()));
            json.put("description", compact(safe(item.getDescription())));
            json.put("category", safe(item.getCategory()));
            json.put("price", item.getPrice());
            json.put("condition", safe(item.getCondition()));
            json.put("location", safe(item.getLocation()));
            json.put("tags", safe(item.getTags()));
            json.put("preferredMajor", safe(item.getPreferredMajor()));
            json.put("sellerMajor", safe(item.getSellerMajor()));
            json.put("campusPickupOnly", item.getCampusPickupOnly() == 1);
            array.put(json);
        }
        return array;
    }

    private JSONArray buildSearchItemsJson(List<Item> items) throws JSONException {
        JSONArray array = new JSONArray();
        int count = Math.min(items.size(), MAX_SEARCH_ITEMS);
        for (int i = 0; i < count; i++) {
            Item item = items.get(i);
            JSONObject json = new JSONObject();
            json.put("id", item.getId());
            json.put("title", safe(item.getTitle()));
            json.put("description", compact(safe(item.getDescription())));
            json.put("category", safe(item.getCategory()));
            json.put("price", item.getPrice());
            json.put("condition", safe(item.getCondition()));
            json.put("location", safe(item.getLocation()));
            json.put("tags", safe(item.getTags()));
            json.put("preferredMajor", safe(item.getPreferredMajor()));
            json.put("sellerMajor", safe(item.getSellerMajor()));
            json.put("campusPickupOnly", item.getCampusPickupOnly() == 1);
            array.put(json);
        }
        return array;
    }

    private JSONObject message(String role, String content) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private JSONObject parseJsonContent(String content) throws JSONException {
        String trimmed = content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        return new JSONObject(trimmed);
    }

    private String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String modelName() {
        String configuredModel = BuildConfig.DEEPSEEK_MODEL;
        return TextUtils.isEmpty(configuredModel) ? FALLBACK_MODEL : configuredModel;
    }

    private String compact(String value) {
        if (value == null) {
            return "";
        }
        String compacted = value.replace('\n', ' ').replace('\r', ' ').trim();
        return compacted.length() > 140 ? compacted.substring(0, 140) : compacted;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class PostDraft {
        private final String title;
        private final String description;
        private final String tags;

        public PostDraft(String title, String description, String tags) {
            this.title = title;
            this.description = description;
            this.tags = tags;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getTags() {
            return tags;
        }
    }

    public static final class RecommendationResult {
        private final String summary;
        private final List<Long> orderedItemIds;
        private final Map<Long, String> itemReasons;

        public RecommendationResult(String summary, List<Long> orderedItemIds,
                                    Map<Long, String> itemReasons) {
            this.summary = summary;
            this.orderedItemIds = orderedItemIds == null
                    ? Collections.emptyList()
                    : new ArrayList<>(orderedItemIds);
            this.itemReasons = itemReasons == null
                    ? Collections.emptyMap()
                    : new HashMap<>(itemReasons);
        }

        public static RecommendationResult empty() {
            return new RecommendationResult("", Collections.emptyList(), Collections.emptyMap());
        }

        public String getSummary() {
            return summary;
        }

        public List<Long> getOrderedItemIds() {
            return orderedItemIds;
        }

        public Map<Long, String> getItemReasons() {
            return itemReasons;
        }
    }

    public static final class SearchResult {
        private final String rewrittenQuery;
        private final String summary;
        private final List<Long> orderedItemIds;
        private final Map<Long, String> itemReasons;

        public SearchResult(String rewrittenQuery, String summary, List<Long> orderedItemIds,
                            Map<Long, String> itemReasons) {
            this.rewrittenQuery = rewrittenQuery == null ? "" : rewrittenQuery;
            this.summary = summary == null ? "" : summary;
            this.orderedItemIds = orderedItemIds == null
                    ? Collections.emptyList()
                    : new ArrayList<>(orderedItemIds);
            this.itemReasons = itemReasons == null
                    ? Collections.emptyMap()
                    : new HashMap<>(itemReasons);
        }

        public static SearchResult empty() {
            return new SearchResult("", "", Collections.emptyList(), Collections.emptyMap());
        }

        public String getRewrittenQuery() {
            return rewrittenQuery;
        }

        public String getSummary() {
            return summary;
        }

        public List<Long> getOrderedItemIds() {
            return orderedItemIds;
        }

        public Map<Long, String> getItemReasons() {
            return itemReasons;
        }
    }

    public static final class PriceSuggestion {
        private final double suggestedPrice;
        private final double minPrice;
        private final double maxPrice;
        private final String reason;

        public PriceSuggestion(double suggestedPrice, double minPrice, double maxPrice, String reason) {
            this.suggestedPrice = suggestedPrice;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.reason = reason == null ? "" : reason;
        }

        public double getSuggestedPrice() {
            return suggestedPrice;
        }

        public double getMinPrice() {
            return minPrice;
        }

        public double getMaxPrice() {
            return maxPrice;
        }

        public String getReason() {
            return reason;
        }
    }

    public static class DeepSeekException extends Exception {
        public DeepSeekException(String message) {
            super(message);
        }

        public DeepSeekException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
