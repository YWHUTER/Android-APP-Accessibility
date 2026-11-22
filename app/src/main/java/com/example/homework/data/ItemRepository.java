package com.example.homework.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.homework.model.Item;
import com.example.homework.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ItemRepository {
    private final AppDatabaseHelper databaseHelper;
    private final UserRepository userRepository;

    public ItemRepository(Context context) {
        databaseHelper = new AppDatabaseHelper(context);
        userRepository = new UserRepository(context);
    }

    public List<Item> getAllItems() {
        return getItems(null, null);
    }

    public List<Item> getLatestItems() {
        return getItems(null, null);
    }

    public List<Item> getDiscoveryItems() {
        List<Item> latestItems = getLatestItems();
        List<ItemScore> scored = new ArrayList<>();
        Map<String, Integer> categoryExposure = new HashMap<>();
        Map<Long, Integer> sellerExposure = new HashMap<>();

        for (Item item : latestItems) {
            String categoryKey = normalize(item.getCategory());
            int categoryCount = categoryExposure.getOrDefault(categoryKey, 0);
            int sellerCount = sellerExposure.getOrDefault(item.getSellerId(), 0);

            double score = freshnessScore(item.getCreatedAt());
            score -= categoryCount * 1.1d;
            score -= sellerCount * 1.8d;
            if (item.getCampusPickupOnly() == 1) {
                score += 0.6d;
            }

            scored.add(new ItemScore(item, score));
            categoryExposure.put(categoryKey, categoryCount + 1);
            sellerExposure.put(item.getSellerId(), sellerCount + 1);
        }

        scored.sort((left, right) -> {
            int scoreCompare = Double.compare(right.score, left.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return Long.compare(right.item.getCreatedAt(), left.item.getCreatedAt());
        });

        List<Item> results = new ArrayList<>();
        for (ItemScore itemScore : scored) {
            results.add(itemScore.item);
        }
        return results;
    }

    public List<Item> getItems(String query, String category) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT i.*, u.name AS seller_name, u.major AS seller_major FROM ")
                .append(AppDatabaseHelper.TABLE_ITEMS).append(" i JOIN ")
                .append(AppDatabaseHelper.TABLE_USERS).append(" u ON i.seller_id = u.id LEFT JOIN ")
                .append(AppDatabaseHelper.TABLE_ORDERS).append(" o ON i.id = o.item_id WHERE o.id IS NULL AND i.is_active = 1 ");
        List<String> args = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (i.title LIKE ? OR i.description LIKE ? OR i.tags LIKE ?) ");
            String like = "%" + query.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (category != null && !category.trim().isEmpty()
                && !"All".equalsIgnoreCase(category)
                && !"全部".equals(category)) {
            sql.append("AND i.category = ? ");
            args.add(category);
        }
        sql.append("ORDER BY i.created_at DESC");

        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        List<Item> items = readItems(cursor);
        cursor.close();
        return items;
    }

    public List<Item> getItemsBySeller(long sellerId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT i.*, u.name AS seller_name, u.major AS seller_major FROM " + AppDatabaseHelper.TABLE_ITEMS + " i " +
                        "JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON i.seller_id = u.id " +
                        "LEFT JOIN " + AppDatabaseHelper.TABLE_ORDERS + " o ON i.id = o.item_id " +
                        "WHERE i.seller_id=? AND o.id IS NULL AND i.is_active = 1 ORDER BY i.created_at DESC",
                new String[]{String.valueOf(sellerId)}
        );
        List<Item> items = readItems(cursor);
        cursor.close();
        return items;
    }

    public List<Item> getRecommendedItems(User user) {
        if (user == null) {
            return getAllItems();
        }

        List<Item> allItems = getAllItems();
        if (allItems.isEmpty()) {
            return allItems;
        }

        RecommendationProfile profile = buildRecommendationProfile(user);
        List<ItemScore> scored = new ArrayList<>();

        for (Item item : allItems) {
            if (item.getSellerId() == user.getId()) {
                continue;
            }
            double score = calculateRecommendationScore(item, user, profile);
            scored.add(new ItemScore(item, score));
        }

        scored.sort((left, right) -> {
            int scoreCompare = Double.compare(right.score, left.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return Long.compare(right.item.getCreatedAt(), left.item.getCreatedAt());
        });

        return diversifyRecommendations(scored);
    }

    public Item getItemById(long itemId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT i.*, u.name AS seller_name, u.major AS seller_major FROM " + AppDatabaseHelper.TABLE_ITEMS + " i " +
                        "JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON i.seller_id = u.id WHERE i.id=?",
                new String[]{String.valueOf(itemId)}
        );
        Item item = null;
        if (cursor.moveToFirst()) {
            item = fromCursor(cursor);
        }
        cursor.close();
        return item;
    }

    public long save(Item item) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("seller_id", item.getSellerId());
        values.put("title", item.getTitle());
        values.put("description", item.getDescription());
        values.put("category", item.getCategory());
        values.put("price", item.getPrice());
        values.put("location", item.getLocation());
        values.put("item_condition", item.getCondition());
        values.put("image_url", item.getImageUrl());
        values.put("tags", item.getTags());
        values.put("preferred_major", item.getPreferredMajor());
        values.put("campus_pickup_only", item.getCampusPickupOnly());
        values.put("is_active", 1);
        values.put("created_at", item.getCreatedAt() == 0 ? System.currentTimeMillis() : item.getCreatedAt());

        if (item.getId() > 0) {
            db.update(AppDatabaseHelper.TABLE_ITEMS, values, "id=?", new String[]{String.valueOf(item.getId())});
            return item.getId();
        }
        return db.insert(AppDatabaseHelper.TABLE_ITEMS, null, values);
    }

    public void delete(long itemId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_active", 0);
        db.update(AppDatabaseHelper.TABLE_ITEMS, values, "id=?", new String[]{String.valueOf(itemId)});
    }

    private RecommendationProfile buildRecommendationProfile(User user) {
        RecommendationProfile profile = new RecommendationProfile();
        Set<String> anchorTokens = new HashSet<>();

        addSignals(profile.keywordWeights, anchorTokens, user.getMajor(), 12d, true);
        addSignals(profile.keywordWeights, anchorTokens, user.getInterests(), 8d, true);
        addSignals(profile.keywordWeights, anchorTokens, user.getBio(), 3d, false);

        loadOrderSignals(user.getId(), profile, 1.0d, true);
        loadConversationSignals(user.getId(), profile, 0.75d, true);
        loadListingSignals(user.getId(), profile, 0.35d);
        loadSimilarUserSignals(user, anchorTokens, profile);
        return profile;
    }

    private void loadOrderSignals(long userId, RecommendationProfile profile, double strength, boolean markSeen) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT i.id, i.seller_id, i.category, i.title, i.description, i.tags, i.preferred_major, i.price " +
                        "FROM " + AppDatabaseHelper.TABLE_ORDERS + " o " +
                        "JOIN " + AppDatabaseHelper.TABLE_ITEMS + " i ON o.item_id = i.id " +
                        "WHERE o.buyer_id=?",
                new String[]{String.valueOf(userId)}
        );
        while (cursor.moveToNext()) {
            applyBehaviorSignal(cursor, profile, strength, markSeen);
        }
        cursor.close();
    }

    private void loadConversationSignals(long userId, RecommendationProfile profile, double strength, boolean markSeen) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT i.id, i.seller_id, i.category, i.title, i.description, i.tags, i.preferred_major, i.price " +
                        "FROM " + AppDatabaseHelper.TABLE_CONVERSATIONS + " c " +
                        "JOIN " + AppDatabaseHelper.TABLE_ITEMS + " i ON c.item_id = i.id " +
                        "WHERE c.buyer_id=?",
                new String[]{String.valueOf(userId)}
        );
        while (cursor.moveToNext()) {
            applyBehaviorSignal(cursor, profile, strength, markSeen);
        }
        cursor.close();
    }

    private void loadListingSignals(long userId, RecommendationProfile profile, double strength) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT i.id, i.seller_id, i.category, i.title, i.description, i.tags, i.preferred_major, i.price " +
                        "FROM " + AppDatabaseHelper.TABLE_ITEMS + " i WHERE i.seller_id=?",
                new String[]{String.valueOf(userId)}
        );
        while (cursor.moveToNext()) {
            applyBehaviorSignal(cursor, profile, strength, false);
        }
        cursor.close();
    }

    private void loadSimilarUserSignals(User currentUser, Set<String> anchorTokens, RecommendationProfile profile) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, major, interests, bio FROM " + AppDatabaseHelper.TABLE_USERS + " WHERE id<>?",
                new String[]{String.valueOf(currentUser.getId())}
        );
        while (cursor.moveToNext()) {
            long otherUserId = cursor.getLong(0);
            String otherMajor = cursor.getString(1);
            String otherInterests = cursor.getString(2);
            String otherBio = cursor.getString(3);
            double similarity = computeUserSimilarity(currentUser, anchorTokens, otherMajor, otherInterests, otherBio);
            if (similarity < 1.2d) {
                continue;
            }

            loadOrderSignals(otherUserId, profile, Math.min(similarity * 0.55d, 2.2d), false);
            loadConversationSignals(otherUserId, profile, Math.min(similarity * 0.35d, 1.4d), false);
        }
        cursor.close();
    }

    private double computeUserSimilarity(User currentUser, Set<String> anchorTokens,
                                         String otherMajor, String otherInterests, String otherBio) {
        double similarity = 0d;
        String currentMajor = normalize(currentUser.getMajor());
        String normalizedOtherMajor = normalize(otherMajor);

        if (!currentMajor.isEmpty() && currentMajor.equals(normalizedOtherMajor)) {
            similarity += 1.8d;
        } else if (!currentMajor.isEmpty()
                && !normalizedOtherMajor.isEmpty()
                && (currentMajor.contains(normalizedOtherMajor) || normalizedOtherMajor.contains(currentMajor))) {
            similarity += 0.9d;
        }

        Set<String> otherTokens = new HashSet<>();
        otherTokens.addAll(extractTokens(otherMajor, true));
        otherTokens.addAll(extractTokens(otherInterests, true));
        otherTokens.addAll(extractTokens(otherBio, false));

        int overlap = 0;
        for (String token : anchorTokens) {
            if (otherTokens.contains(token)) {
                overlap++;
            }
        }
        similarity += overlap * 0.45d;
        return similarity;
    }

    private void applyBehaviorSignal(Cursor cursor, RecommendationProfile profile, double strength, boolean markSeen) {
        long itemId = cursor.getLong(0);
        long sellerId = cursor.getLong(1);
        String category = cursor.getString(2);
        String title = cursor.getString(3);
        String description = cursor.getString(4);
        String tags = cursor.getString(5);
        String preferredMajor = cursor.getString(6);
        double price = cursor.getDouble(7);

        if (markSeen) {
            profile.seenItemIds.add(itemId);
        }

        mergeWeight(profile.categoryWeights, normalize(category), 8d * strength);
        addSignals(profile.keywordWeights, null, title, 3d * strength, false);
        addSignals(profile.keywordWeights, null, description, 1.5d * strength, false);
        addSignals(profile.keywordWeights, null, tags, 4d * strength, true);
        addSignals(profile.keywordWeights, null, preferredMajor, 4d * strength, true);
        profile.sellerWeights.put(sellerId, profile.sellerWeights.getOrDefault(sellerId, 0d) + (2d * strength));
        collectPriceSignal(profile, price, strength);
    }

    private void collectPriceSignal(RecommendationProfile profile, double price, double strength) {
        if (price <= 0d || strength <= 0d) {
            return;
        }
        profile.weightedPriceSum += price * strength;
        profile.weightedPriceSquareSum += price * price * strength;
        profile.priceWeightTotal += strength;
    }

    private double calculateRecommendationScore(Item item, User user, RecommendationProfile profile) {
        double score = freshnessScore(item.getCreatedAt());
        String itemText = normalize(
                safe(item.getCategory()) + " " +
                        safe(item.getTitle()) + " " +
                        safe(item.getDescription()) + " " +
                        safe(item.getTags()) + " " +
                        safe(item.getPreferredMajor()) + " " +
                        safe(item.getLocation()) + " " +
                        safe(item.getSellerMajor())
        );
        String itemCategory = normalize(item.getCategory());
        String userMajor = normalize(user.getMajor());
        String preferredMajor = normalize(item.getPreferredMajor());
        String sellerMajor = normalize(item.getSellerMajor());

        score += profile.categoryWeights.getOrDefault(itemCategory, 0d);
        score += matchKeywordScore(itemText, profile.keywordWeights);
        score += profile.sellerWeights.getOrDefault(item.getSellerId(), 0d);
        score += priceFitScore(item.getPrice(), profile);

        if (!userMajor.isEmpty()) {
            if (preferredMajor.equals(userMajor)) {
                score += 18d;
            } else if (!preferredMajor.isEmpty()
                    && (preferredMajor.contains(userMajor) || userMajor.contains(preferredMajor))) {
                score += 10d;
            }

            if (sellerMajor.equals(userMajor)) {
                score += 4d;
            }
        }

        if (profile.seenItemIds.contains(item.getId())) {
            score -= 2.5d;
        }

        if (item.getCampusPickupOnly() == 1) {
            score += 1.5d;
        }

        return score;
    }

    private double freshnessScore(long createdAt) {
        long ageMillis = Math.max(0L, System.currentTimeMillis() - createdAt);
        long ageHours = ageMillis / (60L * 60L * 1000L);
        if (ageHours <= 6L) {
            return 6d;
        }
        if (ageHours <= 24L) {
            return 4d;
        }
        if (ageHours <= 72L) {
            return 2.5d;
        }
        if (ageHours <= 168L) {
            return 1d;
        }
        return 0.2d;
    }

    private double priceFitScore(double price, RecommendationProfile profile) {
        if (price <= 0d || profile.priceWeightTotal <= 0d) {
            return 0d;
        }

        double averagePrice = profile.weightedPriceSum / profile.priceWeightTotal;
        double variance = (profile.weightedPriceSquareSum / profile.priceWeightTotal) - (averagePrice * averagePrice);
        double standardDeviation = Math.sqrt(Math.max(0d, variance));
        double tolerance = Math.max(30d, Math.max(averagePrice * 0.35d, standardDeviation * 1.35d));
        double diff = Math.abs(price - averagePrice);

        if (diff <= tolerance) {
            return 5d;
        }
        if (diff <= tolerance * 2d) {
            return 5d - ((diff - tolerance) / tolerance) * 3d;
        }
        if (diff <= tolerance * 4d) {
            return -Math.min(4d, ((diff / tolerance) - 2d) * 1.5d);
        }
        return -4d;
    }

    private double matchKeywordScore(String itemText, Map<String, Double> keywordWeights) {
        double score = 0d;
        for (Map.Entry<String, Double> entry : keywordWeights.entrySet()) {
            String token = entry.getKey();
            if (token.length() < 2) {
                continue;
            }
            if (itemText.contains(token)) {
                score += entry.getValue();
            }
        }
        return Math.min(score, 45d);
    }

    private List<Item> diversifyRecommendations(List<ItemScore> scored) {
        List<ItemScore> remaining = new ArrayList<>(scored);
        List<Item> results = new ArrayList<>();
        Map<String, Integer> categoryExposure = new HashMap<>();
        int rank = 0;

        // Greedy re-ranking: keep high-score items, but reduce repeated categories at the top.
        while (!remaining.isEmpty()) {
            int bestIndex = 0;
            double bestAdjustedScore = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < remaining.size(); i++) {
                ItemScore candidate = remaining.get(i);
                String category = normalize(candidate.item.getCategory());
                int exposure = categoryExposure.getOrDefault(category, 0);
                double penalty = exposure * (rank < 8 ? 4d : 1.5d);
                double adjustedScore = candidate.score - penalty;
                if (adjustedScore > bestAdjustedScore) {
                    bestAdjustedScore = adjustedScore;
                    bestIndex = i;
                }
            }

            ItemScore selected = remaining.remove(bestIndex);
            results.add(selected.item);
            String selectedCategory = normalize(selected.item.getCategory());
            categoryExposure.put(selectedCategory, categoryExposure.getOrDefault(selectedCategory, 0) + 1);
            rank++;
        }
        return results;
    }

    private void addSignals(Map<String, Double> weights, Set<String> tokens, String text,
                            double baseWeight, boolean includeWholeText) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return;
        }

        if (includeWholeText && normalized.length() >= 2) {
            mergeWeight(weights, normalized, baseWeight);
            if (tokens != null) {
                tokens.add(normalized);
            }
        }

        for (String part : extractTokens(text, includeWholeText)) {
            mergeWeight(weights, part, Math.max(1d, baseWeight * 0.7d));
            if (tokens != null) {
                tokens.add(part);
            }
        }
    }

    private Set<String> extractTokens(String text, boolean includeWholeText) {
        Set<String> tokens = new HashSet<>();
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return tokens;
        }

        if (includeWholeText && normalized.length() >= 2) {
            tokens.add(normalized);
        }

        String[] parts = normalized.split("[,，/|、\\s]+");
        for (String part : parts) {
            if (part.length() >= 2) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private void mergeWeight(Map<String, Double> weights, String key, double delta) {
        if (key == null || key.isEmpty() || delta <= 0d) {
            return;
        }
        weights.put(key, weights.getOrDefault(key, 0d) + delta);
    }

    private List<Item> readItems(Cursor cursor) {
        List<Item> items = new ArrayList<>();
        while (cursor.moveToNext()) {
            items.add(fromCursor(cursor));
        }
        return items;
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
        int sellerNameIndex = cursor.getColumnIndex("seller_name");
        int sellerMajorIndex = cursor.getColumnIndex("seller_major");
        if (sellerNameIndex >= 0) {
            item.setSellerName(cursor.getString(sellerNameIndex));
        } else {
            User seller = userRepository.getById(item.getSellerId());
            item.setSellerName(seller != null ? seller.getName() : "未知用户");
        }
        if (sellerMajorIndex >= 0) {
            item.setSellerMajor(cursor.getString(sellerMajorIndex));
        }
        return item;
    }

    private String normalize(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class RecommendationProfile {
        final Map<String, Double> keywordWeights = new HashMap<>();
        final Map<String, Double> categoryWeights = new HashMap<>();
        final Map<Long, Double> sellerWeights = new HashMap<>();
        final Set<Long> seenItemIds = new HashSet<>();
        double weightedPriceSum;
        double weightedPriceSquareSum;
        double priceWeightTotal;
    }

    private static class ItemScore {
        final Item item;
        final double score;

        ItemScore(Item item, double score) {
            this.item = item;
            this.score = score;
        }
    }
}
