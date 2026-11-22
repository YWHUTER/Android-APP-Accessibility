package com.example.homework.ui;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.homework.ai.DeepSeekClient;
import com.example.homework.data.ItemRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.data.UserRepository;
import com.example.homework.model.Item;
import com.example.homework.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscoveryViewModel extends AndroidViewModel {
    private static final long SEARCH_DEBOUNCE_MS = 280L;
    private static final long AI_RECOMMENDATION_CACHE_TTL_MS = 5L * 60L * 1000L;
    private static final long AI_RECOMMENDATION_RETRY_COOLDOWN_MS = 60L * 1000L;
    private static final int AI_RECOMMENDATION_CACHE_LIMIT = 8;
    private static final int AI_RECOMMENDATION_CANDIDATE_LIMIT = 8;
    private static final long AI_SEARCH_CACHE_TTL_MS = 5L * 60L * 1000L;
    private static final long AI_SEARCH_RETRY_COOLDOWN_MS = 45L * 1000L;
    private static final int AI_SEARCH_CACHE_LIMIT = 10;
    private static final int AI_SEARCH_CANDIDATE_LIMIT = 18;

    private final ItemRepository itemRepository;
    private final SessionManager sessionManager;
    private final UserRepository userRepository;
    private final DeepSeekClient deepSeekClient = new DeepSeekClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<DiscoveryState> uiState = new MutableLiveData<>();
    private final Runnable debouncedLoad = this::loadInBackground;

    private String searchQuery = "";
    private String selectedCategory = DiscoveryUiMapper.CATEGORY_ALL;
    private DiscoveryFeedMode feedMode = DiscoveryFeedMode.FOLLOW;
    private boolean initialized;
    private int requestVersion;
    private final Map<String, CachedRecommendation> aiRecommendationCache = new HashMap<>();
    private final Map<String, Long> aiRecommendationFailureTimes = new HashMap<>();
    private final Map<String, CachedSearch> aiSearchCache = new HashMap<>();
    private final Map<String, Long> aiSearchFailureTimes = new HashMap<>();
    private String pendingAiRecommendationKey = "";
    private volatile String activeAiRecommendationKey = "";
    private String pendingAiSearchKey = "";
    private volatile String activeAiSearchKey = "";
    private boolean aiRecommendationEnabled;
    private boolean aiSearchEnabled;
    private String aiSearchSubmittedQuery = "";

    public DiscoveryViewModel(@NonNull Application application) {
        super(application);
        itemRepository = new ItemRepository(application);
        sessionManager = new SessionManager(application);
        userRepository = new UserRepository(application);
        uiState.setValue(new DiscoveryState(
                false,
                searchQuery,
                selectedCategory,
                feedMode,
                Collections.emptyList(),
                null,
                Collections.emptyMap(),
                aiRecommendationEnabled,
                aiSearchEnabled,
                DiscoveryUiMapper.RecommendationPanel.hidden(),
                "",
                DiscoveryUiMapper.resolveEmptyStateText(feedMode)
        ));
    }

    public LiveData<DiscoveryState> getUiState() {
        return uiState;
    }

    public void initialize(String initialQuery, String initialCategory, DiscoveryFeedMode initialFeedMode,
                           boolean initialAiSearchEnabled) {
        String normalizedQuery = initialQuery == null ? "" : initialQuery;
        String normalizedCategory = normalizeCategory(initialCategory);
        DiscoveryFeedMode normalizedFeedMode =
                initialFeedMode == null ? DiscoveryFeedMode.FOLLOW : initialFeedMode;

        boolean changed = !normalizedQuery.equals(searchQuery)
                || !normalizedCategory.equals(selectedCategory)
                || normalizedFeedMode != feedMode
                || initialAiSearchEnabled != aiSearchEnabled;

        searchQuery = normalizedQuery;
        selectedCategory = normalizedCategory;
        feedMode = normalizedFeedMode;
        aiSearchEnabled = initialAiSearchEnabled;
        aiSearchSubmittedQuery = aiSearchEnabled ? normalizedQuery : "";

        if (!initialized || changed) {
            initialized = true;
            loadImmediately();
        }
    }

    public void setSearchQuery(String query) {
        String normalizedQuery = query == null ? "" : query;
        if (normalizedQuery.equals(searchQuery)) {
            return;
        }
        searchQuery = normalizedQuery;
        publishLoading();
        mainHandler.removeCallbacks(debouncedLoad);
        mainHandler.postDelayed(debouncedLoad, SEARCH_DEBOUNCE_MS);
    }

    public void submitSearch() {
        aiSearchSubmittedQuery = aiSearchEnabled ? searchQuery : "";
        loadImmediately();
    }

    public void setAiSearchEnabled(boolean enabled) {
        if (aiSearchEnabled == enabled) {
            if (enabled) {
                submitSearch();
            }
            return;
        }
        aiSearchEnabled = enabled;
        aiSearchSubmittedQuery = enabled ? searchQuery : "";
        loadImmediately();
    }

    public void setSelectedCategory(String category) {
        String normalizedCategory = normalizeCategory(category);
        if (normalizedCategory.equals(selectedCategory)) {
            return;
        }
        selectedCategory = normalizedCategory;
        loadImmediately();
    }

    public void setFeedMode(DiscoveryFeedMode mode) {
        DiscoveryFeedMode normalizedMode = mode == null ? DiscoveryFeedMode.FOLLOW : mode;
        if (normalizedMode == feedMode) {
            return;
        }
        feedMode = normalizedMode;
        loadImmediately();
    }

    public void toggleAiRecommendations() {
        if (feedMode != DiscoveryFeedMode.RECOMMEND) {
            feedMode = DiscoveryFeedMode.RECOMMEND;
        }
        aiRecommendationEnabled = !aiRecommendationEnabled;
        loadImmediately();
    }

    public void setAiRecommendationsEnabled(boolean enabled) {
        if (feedMode != DiscoveryFeedMode.RECOMMEND) {
            feedMode = DiscoveryFeedMode.RECOMMEND;
        }
        if (aiRecommendationEnabled == enabled) {
            return;
        }
        aiRecommendationEnabled = enabled;
        loadImmediately();
    }

    public void resetFilters() {
        searchQuery = "";
        selectedCategory = DiscoveryUiMapper.CATEGORY_ALL;
        feedMode = DiscoveryFeedMode.FOLLOW;
        aiRecommendationEnabled = false;
        aiSearchEnabled = false;
        aiSearchSubmittedQuery = "";
        loadImmediately();
    }

    public void refreshImmediate() {
        if (!initialized) {
            return;
        }
        loadImmediately();
    }

    private void loadImmediately() {
        mainHandler.removeCallbacks(debouncedLoad);
        loadInBackground();
    }

    private void loadInBackground() {
        int requestId = ++requestVersion;
        publishLoading();

        final String query = searchQuery;
        final String category = selectedCategory;
        final DiscoveryFeedMode currentFeedMode = feedMode;

        executor.execute(() -> {
            User currentUser = null;
            List<Item> sourceItems;

            if (currentFeedMode == DiscoveryFeedMode.RECOMMEND && sessionManager.isLoggedIn()) {
                currentUser = userRepository.getById(sessionManager.getLoggedInUserId());
                if (currentUser != null) {
                    sourceItems = itemRepository.getRecommendedItems(currentUser);
                } else {
                    sourceItems = itemRepository.getDiscoveryItems();
                }
            } else if (currentFeedMode == DiscoveryFeedMode.LATEST) {
                sourceItems = itemRepository.getLatestItems();
            } else {
                sourceItems = itemRepository.getDiscoveryItems();
            }

            List<Item> localFilteredItems = DiscoveryUiMapper.filterItems(sourceItems, query, category);
            List<Item> categoryFilteredItems = DiscoveryUiMapper.filterItems(sourceItems, "", category);
            List<Item> filteredItems = localFilteredItems;
            Map<Long, String> aiItemReasons = Collections.emptyMap();
            String aiSearchStatus = buildAiSearchStatus(query);

            boolean aiSearchReady = aiSearchEnabled
                    && !isBlank(query)
                    && query.equals(aiSearchSubmittedQuery);
            List<Item> aiSearchCandidates = aiSearchReady
                    ? buildAiSearchCandidates(categoryFilteredItems, query)
                    : Collections.emptyList();
            boolean shouldRequestAiSearch = aiSearchReady
                    && deepSeekClient.isConfigured()
                    && !aiSearchCandidates.isEmpty();
            String aiSearchCacheKey = shouldRequestAiSearch
                    ? buildAiSearchCacheKey(currentUser, query, category, currentFeedMode, aiSearchCandidates)
                    : "";
            CachedSearch cachedSearch = shouldRequestAiSearch ? getCachedAiSearch(aiSearchCacheKey) : null;
            boolean aiSearchCoolingDown = shouldRequestAiSearch && isAiSearchCoolingDown(aiSearchCacheKey);

            if (aiSearchEnabled && !isBlank(query) && !query.equals(aiSearchSubmittedQuery)) {
                aiSearchStatus = "\u5df2\u5207\u5230 AI \u8054\u673a\u641c\u7d22\uff0c\u6309\u641c\u7d22\u540e\u53d1\u8d77\u8054\u673a\u67e5\u8be2\u3002";
            } else if (aiSearchReady && !deepSeekClient.isConfigured()) {
                aiSearchStatus = "\u672a\u914d\u7f6e DeepSeek Key\uff0c\u5df2\u4f7f\u7528\u672c\u5730\u641c\u7d22\u3002";
            } else if (aiSearchReady && aiSearchCandidates.isEmpty()) {
                aiSearchStatus = "\u5f53\u524d\u5206\u7c7b\u4e0b\u6ca1\u6709\u53ef\u4f9b AI \u641c\u7d22\u7684\u5019\u9009\u5546\u54c1\u3002";
            } else if (cachedSearch != null && cachedSearch.result != null) {
                DeepSeekClient.SearchResult aiSearchResult = cachedSearch.result;
                filteredItems = applyAiSearchOrdering(aiSearchCandidates, aiSearchResult, localFilteredItems);
                aiItemReasons = aiSearchResult.getItemReasons();
                aiSearchStatus = buildAiSearchResultStatus(aiSearchResult, filteredItems.size());
            } else if (shouldRequestAiSearch && !aiSearchCoolingDown) {
                aiSearchStatus = "\u6b63\u5728\u8054\u673a\u7406\u89e3\u641c\u7d22\u610f\u56fe\uff0c\u5148\u5c55\u793a\u672c\u5730\u7ed3\u679c\u3002";
            } else if (shouldRequestAiSearch) {
                aiSearchStatus = "\u4e0a\u6b21 AI \u641c\u7d22\u5931\u8d25\uff0c\u6682\u65f6\u4f7f\u7528\u672c\u5730\u7ed3\u679c\u3002";
            }

            DiscoveryUiMapper.RecommendationPanel recommendationPanel =
                    currentFeedMode == DiscoveryFeedMode.RECOMMEND && currentUser != null
                            ? buildLocalRecommendationPanel(currentUser, filteredItems.size())
                            : DiscoveryUiMapper.RecommendationPanel.hidden();

            boolean shouldRequestAi = currentFeedMode == DiscoveryFeedMode.RECOMMEND
                    && aiRecommendationEnabled
                    && !(aiSearchEnabled && !isBlank(query))
                    && currentUser != null
                    && deepSeekClient.isConfigured()
                    && !filteredItems.isEmpty();
            String aiCacheKey = shouldRequestAi
                    ? buildAiRecommendationCacheKey(currentUser, query, category, filteredItems)
                    : "";
            CachedRecommendation cachedRecommendation = shouldRequestAi
                    ? getCachedAiRecommendation(aiCacheKey)
                    : null;
            boolean aiCoolingDown = shouldRequestAi && isAiRecommendationCoolingDown(aiCacheKey);

            if (cachedRecommendation != null) {
                DeepSeekClient.RecommendationResult aiResult = cachedRecommendation.result;
                if (aiResult != null) {
                    filteredItems = applyAiOrdering(filteredItems, aiResult.getOrderedItemIds());
                    aiItemReasons = aiResult.getItemReasons();
                    recommendationPanel = buildAiRecommendationPanel(
                            currentUser,
                            filteredItems.size(),
                            aiResult.getSummary()
                    );
                }
            } else if (shouldRequestAi && !aiCoolingDown) {
                recommendationPanel = buildAiPendingRecommendationPanel(currentUser, filteredItems.size());
            }

            DiscoveryState state = new DiscoveryState(
                    false,
                    query,
                    category,
                    currentFeedMode,
                    filteredItems,
                    currentUser,
                    aiItemReasons,
                    aiRecommendationEnabled,
                    aiSearchEnabled,
                    recommendationPanel,
                    aiSearchStatus,
                    DiscoveryUiMapper.resolveEmptyStateText(currentFeedMode)
            );

            if (requestId == requestVersion) {
                activeAiRecommendationKey = shouldRequestAi ? aiCacheKey : "";
                activeAiSearchKey = shouldRequestAiSearch ? aiSearchCacheKey : "";
                uiState.postValue(state);
            }

            if (shouldRequestAiSearch && cachedSearch == null && !aiSearchCoolingDown && requestId == requestVersion) {
                requestAiSearch(
                        currentUser,
                        query,
                        category,
                        currentFeedMode,
                        aiSearchCandidates,
                        localFilteredItems,
                        aiSearchCacheKey
                );
            } else if (shouldRequestAi && cachedRecommendation == null && !aiCoolingDown && requestId == requestVersion) {
                requestAiRecommendation(
                        currentUser,
                        query,
                        category,
                        filteredItems,
                        aiCacheKey
                );
            }
        });
    }

    private void requestAiRecommendation(User currentUser, String query,
                                         String category, List<Item> items, String cacheKey) {
        if (!markAiRecommendationPending(cacheKey)) {
            return;
        }

        List<Item> candidateItems = new ArrayList<>(items);
        aiExecutor.execute(() -> {
            try {
                DeepSeekClient.RecommendationResult aiResult =
                        deepSeekClient.generateRecommendations(currentUser, candidateItems);
                finishAiRecommendation(cacheKey, aiResult);

                if (!isCurrentAiRequest(cacheKey, query, category)) {
                    return;
                }

                List<Item> orderedItems = applyAiOrdering(candidateItems, aiResult.getOrderedItemIds());
                DiscoveryState state = new DiscoveryState(
                        false,
                        query,
                        category,
                        DiscoveryFeedMode.RECOMMEND,
                        orderedItems,
                        currentUser,
                        aiResult.getItemReasons(),
                        true,
                        aiSearchEnabled,
                        buildAiRecommendationPanel(currentUser, orderedItems.size(), aiResult.getSummary()),
                        buildAiSearchStatus(query),
                        DiscoveryUiMapper.resolveEmptyStateText(DiscoveryFeedMode.RECOMMEND)
                );
                uiState.postValue(state);
            } catch (DeepSeekClient.DeepSeekException exception) {
                finishAiRecommendation(cacheKey, null);
            }
        });
    }

    private void requestAiSearch(User currentUser, String query, String category,
                                 DiscoveryFeedMode currentFeedMode, List<Item> candidates,
                                 List<Item> localFallbackItems, String cacheKey) {
        if (!markAiSearchPending(cacheKey)) {
            return;
        }

        List<Item> candidateItems = new ArrayList<>(candidates);
        List<Item> fallbackItems = new ArrayList<>(localFallbackItems);
        aiExecutor.execute(() -> {
            try {
                DeepSeekClient.SearchResult aiResult =
                        deepSeekClient.generateSearchResults(query, currentUser, candidateItems);
                finishAiSearch(cacheKey, aiResult);

                if (!isCurrentAiSearchRequest(cacheKey, query, category, currentFeedMode)) {
                    return;
                }

                List<Item> orderedItems = applyAiSearchOrdering(candidateItems, aiResult, fallbackItems);
                DiscoveryUiMapper.RecommendationPanel recommendationPanel =
                        currentFeedMode == DiscoveryFeedMode.RECOMMEND && currentUser != null
                                ? buildLocalRecommendationPanel(currentUser, orderedItems.size())
                                : DiscoveryUiMapper.RecommendationPanel.hidden();
                DiscoveryState state = new DiscoveryState(
                        false,
                        query,
                        category,
                        currentFeedMode,
                        orderedItems,
                        currentUser,
                        aiResult.getItemReasons(),
                        aiRecommendationEnabled,
                        true,
                        recommendationPanel,
                        buildAiSearchResultStatus(aiResult, orderedItems.size()),
                        DiscoveryUiMapper.resolveEmptyStateText(currentFeedMode)
                );
                uiState.postValue(state);
            } catch (DeepSeekClient.DeepSeekException exception) {
                finishAiSearch(cacheKey, null);
                if (!isCurrentAiSearchRequest(cacheKey, query, category, currentFeedMode)) {
                    return;
                }
                DiscoveryUiMapper.RecommendationPanel recommendationPanel =
                        currentFeedMode == DiscoveryFeedMode.RECOMMEND && currentUser != null
                                ? buildLocalRecommendationPanel(currentUser, fallbackItems.size())
                                : DiscoveryUiMapper.RecommendationPanel.hidden();
                DiscoveryState state = new DiscoveryState(
                        false,
                        query,
                        category,
                        currentFeedMode,
                        fallbackItems,
                        currentUser,
                        Collections.emptyMap(),
                        aiRecommendationEnabled,
                        true,
                        recommendationPanel,
                        "\u8054\u673a\u641c\u7d22\u5931\u8d25\uff0c\u5df2\u56de\u9000\u5230\u672c\u5730\u641c\u7d22\u3002",
                        DiscoveryUiMapper.resolveEmptyStateText(currentFeedMode)
                );
                uiState.postValue(state);
            }
        });
    }

    private String buildAiRecommendationCacheKey(User user, String query, String category, List<Item> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(user.getId())
                .append('|')
                .append(query)
                .append('|')
                .append(category)
                .append('|')
                .append(user.getMajor())
                .append('|')
                .append(user.getInterests());
        int count = Math.min(items.size(), AI_RECOMMENDATION_CANDIDATE_LIMIT);
        for (int i = 0; i < count; i++) {
            Item item = items.get(i);
            builder.append('|').append(item.getId()).append(':').append(item.getCreatedAt());
        }
        return builder.toString();
    }

    private String buildAiSearchCacheKey(User user, String query, String category,
                                         DiscoveryFeedMode mode, List<Item> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(mode.name())
                .append('|')
                .append(query)
                .append('|')
                .append(category);
        if (user != null) {
            builder.append('|')
                    .append(user.getId())
                    .append('|')
                    .append(user.getMajor())
                    .append('|')
                    .append(user.getInterests());
        }
        int count = Math.min(items.size(), AI_SEARCH_CANDIDATE_LIMIT);
        for (int i = 0; i < count; i++) {
            Item item = items.get(i);
            builder.append('|').append(item.getId()).append(':').append(item.getCreatedAt());
        }
        return builder.toString();
    }

    private List<Item> buildAiSearchCandidates(List<Item> items, String query) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<ScoredSearchCandidate> scoredItems = new ArrayList<>();
        String normalizedQuery = normalizeSearchText(query);
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            scoredItems.add(new ScoredSearchCandidate(
                    item,
                    scoreAiSearchCandidate(item, normalizedQuery, i)
            ));
        }
        Collections.sort(scoredItems, (left, right) -> Double.compare(right.score, left.score));

        List<Item> candidates = new ArrayList<>();
        int count = Math.min(scoredItems.size(), AI_SEARCH_CANDIDATE_LIMIT);
        for (int i = 0; i < count; i++) {
            candidates.add(scoredItems.get(i).item);
        }
        return candidates;
    }

    private double scoreAiSearchCandidate(Item item, String normalizedQuery, int position) {
        String title = normalizeSearchText(item.getTitle());
        String description = normalizeSearchText(item.getDescription());
        String tags = normalizeSearchText(item.getTags());
        String category = normalizeSearchText(item.getCategory());
        String location = normalizeSearchText(item.getLocation());
        String combined = title + " " + description + " " + tags + " " + category + " " + location;

        double score = Math.max(0d, 20d - position * 0.12d);
        if (!isBlank(normalizedQuery)) {
            if (title.contains(normalizedQuery)) {
                score += 18d;
            }
            if (tags.contains(normalizedQuery) || category.contains(normalizedQuery)) {
                score += 12d;
            }
            if (combined.contains(normalizedQuery)) {
                score += 8d;
            }
            String[] tokens = normalizedQuery.split("\\s+");
            for (String token : tokens) {
                if (token.length() >= 2 && combined.contains(token)) {
                    score += 4d;
                }
            }
        }
        if (item.getCampusPickupOnly() == 1) {
            score += 1.5d;
        }
        return score;
    }

    private List<Item> applyAiSearchOrdering(List<Item> candidates, DeepSeekClient.SearchResult result,
                                             List<Item> localFallbackItems) {
        if (result == null || result.getOrderedItemIds().isEmpty()) {
            return localFallbackItems;
        }

        Map<Long, Item> itemById = new HashMap<>();
        for (Item item : candidates) {
            itemById.put(item.getId(), item);
        }

        List<Item> orderedItems = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();
        for (Long itemId : result.getOrderedItemIds()) {
            Item item = itemById.get(itemId);
            if (item != null && usedIds.add(item.getId())) {
                orderedItems.add(item);
            }
        }
        for (Item item : localFallbackItems) {
            if (usedIds.add(item.getId())) {
                orderedItems.add(item);
            }
        }
        return orderedItems;
    }

    private List<Item> applyAiOrdering(List<Item> items, List<Long> orderedItemIds) {
        if (orderedItemIds == null || orderedItemIds.isEmpty()) {
            return items;
        }

        Map<Long, Item> itemById = new HashMap<>();
        for (Item item : items) {
            itemById.put(item.getId(), item);
        }

        List<Item> orderedItems = new ArrayList<>();
        for (Long itemId : orderedItemIds) {
            Item item = itemById.remove(itemId);
            if (item != null) {
                orderedItems.add(item);
            }
        }
        for (Item item : items) {
            if (itemById.containsKey(item.getId())) {
                orderedItems.add(item);
            }
        }
        return orderedItems;
    }

    private synchronized CachedRecommendation getCachedAiRecommendation(String cacheKey) {
        CachedRecommendation cachedRecommendation = aiRecommendationCache.get(cacheKey);
        if (cachedRecommendation == null) {
            return null;
        }
        if (System.currentTimeMillis() - cachedRecommendation.cachedAt > AI_RECOMMENDATION_CACHE_TTL_MS) {
            aiRecommendationCache.remove(cacheKey);
            return null;
        }
        return cachedRecommendation;
    }

    private synchronized boolean isAiRecommendationCoolingDown(String cacheKey) {
        Long failedAt = aiRecommendationFailureTimes.get(cacheKey);
        return failedAt != null
                && System.currentTimeMillis() - failedAt < AI_RECOMMENDATION_RETRY_COOLDOWN_MS;
    }

    private synchronized CachedSearch getCachedAiSearch(String cacheKey) {
        CachedSearch cachedSearch = aiSearchCache.get(cacheKey);
        if (cachedSearch == null) {
            return null;
        }
        if (System.currentTimeMillis() - cachedSearch.cachedAt > AI_SEARCH_CACHE_TTL_MS) {
            aiSearchCache.remove(cacheKey);
            return null;
        }
        return cachedSearch;
    }

    private synchronized boolean isAiSearchCoolingDown(String cacheKey) {
        Long failedAt = aiSearchFailureTimes.get(cacheKey);
        return failedAt != null
                && System.currentTimeMillis() - failedAt < AI_SEARCH_RETRY_COOLDOWN_MS;
    }

    private synchronized boolean markAiRecommendationPending(String cacheKey) {
        if (cacheKey == null || cacheKey.isEmpty()) {
            return false;
        }
        if (getCachedAiRecommendation(cacheKey) != null) {
            return false;
        }
        if (isAiRecommendationCoolingDown(cacheKey)) {
            return false;
        }
        if (cacheKey.equals(pendingAiRecommendationKey)) {
            return false;
        }
        pendingAiRecommendationKey = cacheKey;
        return true;
    }

    private synchronized boolean markAiSearchPending(String cacheKey) {
        if (cacheKey == null || cacheKey.isEmpty()) {
            return false;
        }
        if (getCachedAiSearch(cacheKey) != null) {
            return false;
        }
        if (isAiSearchCoolingDown(cacheKey)) {
            return false;
        }
        if (cacheKey.equals(pendingAiSearchKey)) {
            return false;
        }
        pendingAiSearchKey = cacheKey;
        return true;
    }

    private synchronized void finishAiRecommendation(String cacheKey,
                                                    DeepSeekClient.RecommendationResult result) {
        if (cacheKey.equals(pendingAiRecommendationKey)) {
            pendingAiRecommendationKey = "";
        }
        if (result == null) {
            aiRecommendationFailureTimes.put(cacheKey, System.currentTimeMillis());
            return;
        }
        aiRecommendationFailureTimes.remove(cacheKey);
        if (aiRecommendationCache.size() >= AI_RECOMMENDATION_CACHE_LIMIT) {
            aiRecommendationCache.clear();
        }
        aiRecommendationCache.put(cacheKey, new CachedRecommendation(result, System.currentTimeMillis()));
    }

    private synchronized void finishAiSearch(String cacheKey, DeepSeekClient.SearchResult result) {
        if (cacheKey.equals(pendingAiSearchKey)) {
            pendingAiSearchKey = "";
        }
        if (result == null) {
            aiSearchFailureTimes.put(cacheKey, System.currentTimeMillis());
            return;
        }
        aiSearchFailureTimes.remove(cacheKey);
        if (aiSearchCache.size() >= AI_SEARCH_CACHE_LIMIT) {
            aiSearchCache.clear();
        }
        aiSearchCache.put(cacheKey, new CachedSearch(result, System.currentTimeMillis()));
    }

    private boolean isCurrentAiRequest(String cacheKey, String query, String category) {
        return feedMode == DiscoveryFeedMode.RECOMMEND
                && query.equals(searchQuery)
                && category.equals(selectedCategory)
                && cacheKey.equals(activeAiRecommendationKey);
    }

    private boolean isCurrentAiSearchRequest(String cacheKey, String query, String category,
                                             DiscoveryFeedMode mode) {
        return aiSearchEnabled
                && mode == feedMode
                && query.equals(searchQuery)
                && query.equals(aiSearchSubmittedQuery)
                && category.equals(selectedCategory)
                && cacheKey.equals(activeAiSearchKey);
    }

    private DiscoveryUiMapper.RecommendationPanel buildAiRecommendationPanel(User user, int itemCount,
                                                                            String aiSummary) {
        String interests = DiscoveryUiMapper.firstUsefulToken(user.getInterests(), "\u4f60\u7684\u5174\u8da3");
        if (interests.length() > 8) {
            interests = interests.substring(0, 8);
        }
        String summary = aiSummary == null || aiSummary.trim().isEmpty()
                ? "\u5df2\u7ed3\u5408\u4f60\u7684\u4e13\u4e1a\u3001\u5174\u8da3\u548c\u5019\u9009\u95f2\u7f6e\u751f\u6210 AI \u63a8\u8350\u3002"
                : aiSummary.trim();
        return new DiscoveryUiMapper.RecommendationPanel(
                true,
                "AI \u63a8\u8350\u52a9\u624b",
                summary,
                "AI DeepSeek",
                "\u5174\u8da3 \u00b7 " + interests,
                "\u63a8\u8350 " + itemCount
        );
    }

    private DiscoveryUiMapper.RecommendationPanel buildLocalRecommendationPanel(User user, int itemCount) {
        String major = DiscoveryUiMapper.safeText(user.getMajor(), "\u4f60\u7684\u4e13\u4e1a");
        if (major.length() > 8) {
            major = major.substring(0, 8);
        }
        String interests = DiscoveryUiMapper.firstUsefulToken(user.getInterests(), "\u4f60\u7684\u5174\u8da3");
        if (interests.length() > 8) {
            interests = interests.substring(0, 8);
        }
        return new DiscoveryUiMapper.RecommendationPanel(
                true,
                "\u667a\u80fd\u63a8\u8350",
                "\u6839\u636e\u4e13\u4e1a\u3001\u5174\u8da3\u3001\u5386\u53f2\u4e92\u52a8\u3001\u4ef7\u683c\u5339\u914d\u548c\u4e0a\u67b6\u65f6\u95f4\u751f\u6210\u672c\u5730\u63a8\u8350\u3002",
                "\u672c\u5730\u7b97\u6cd5",
                "\u5174\u8da3 \u00b7 " + interests,
                "\u63a8\u8350 " + itemCount
        );
    }

    private DiscoveryUiMapper.RecommendationPanel buildAiPendingRecommendationPanel(User user, int itemCount) {
        String interests = DiscoveryUiMapper.firstUsefulToken(user.getInterests(), "\u4f60\u7684\u5174\u8da3");
        if (interests.length() > 8) {
            interests = interests.substring(0, 8);
        }
        return new DiscoveryUiMapper.RecommendationPanel(
                true,
                "AI \u63a8\u8350\u52a9\u624b",
                "\u5148\u5c55\u793a\u672c\u5730\u63a8\u8350\uff0cAI \u6b63\u5728\u540e\u53f0\u8865\u5145\u6392\u5e8f\u548c\u7406\u7531\u3002",
                "Local First",
                "\u5174\u8da3 \u00b7 " + interests,
                "\u5019\u9009 " + itemCount
        );
    }

    private void publishLoading() {
        DiscoveryState current = uiState.getValue();
        List<Item> currentItems = current != null ? current.getItems() : Collections.emptyList();
        User currentUser = current != null ? current.getCurrentUser() : null;
        Map<Long, String> aiRecommendationReasons =
                current != null
                        && current.getFeedMode() == feedMode
                        && current.getSearchQuery().equals(searchQuery)
                        && current.getSelectedCategory().equals(selectedCategory)
                        && (aiRecommendationEnabled || aiSearchEnabled)
                        ? current.getAiRecommendationReasons()
                        : Collections.emptyMap();
        DiscoveryUiMapper.RecommendationPanel recommendationPanel =
                feedMode == DiscoveryFeedMode.RECOMMEND && current != null
                        ? current.getRecommendationPanel()
                        : DiscoveryUiMapper.RecommendationPanel.hidden();
        String aiSearchStatus = aiSearchEnabled
                ? (current != null ? current.getAiSearchStatus() : buildAiSearchStatus(searchQuery))
                : "";

        uiState.setValue(new DiscoveryState(
                true,
                searchQuery,
                selectedCategory,
                feedMode,
                currentItems,
                currentUser,
                aiRecommendationReasons,
                aiRecommendationEnabled,
                aiSearchEnabled,
                recommendationPanel,
                aiSearchStatus,
                DiscoveryUiMapper.resolveEmptyStateText(feedMode)
        ));
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return DiscoveryUiMapper.CATEGORY_ALL;
        }
        return category;
    }

    private String buildAiSearchStatus(String query) {
        if (!aiSearchEnabled) {
            return "";
        }
        if (isBlank(query)) {
            return "\u8f93\u5165\u5173\u952e\u8bcd\u540e\uff0cAI \u8054\u673a\u641c\u7d22\u4f1a\u7406\u89e3\u8bed\u4e49\u5e76\u91cd\u6392\u7ed3\u679c\u3002";
        }
        if (!query.equals(aiSearchSubmittedQuery)) {
            return "\u5df2\u5207\u5230 AI \u8054\u673a\u641c\u7d22\uff0c\u6309\u641c\u7d22\u540e\u53d1\u8d77\u8054\u673a\u67e5\u8be2\u3002";
        }
        return "\u6b63\u5728\u51c6\u5907 AI \u8054\u673a\u641c\u7d22\u3002";
    }

    private String buildAiSearchResultStatus(DeepSeekClient.SearchResult result, int itemCount) {
        if (result == null) {
            return "\u5df2\u4f7f\u7528 AI \u8054\u673a\u641c\u7d22\u7ed3\u679c\u3002";
        }
        String summary = result.getSummary();
        if (!isBlank(summary)) {
            return summary;
        }
        String rewrittenQuery = result.getRewrittenQuery();
        if (!isBlank(rewrittenQuery)) {
            return "\u5df2\u6309\u201c" + rewrittenQuery + "\u201d\u7406\u89e3\u5e76\u6392\u5e8f " + itemCount + " \u4e2a\u7ed3\u679c\u3002";
        }
        return "\u5df2\u5b8c\u6210 AI \u8054\u673a\u641c\u7d22\uff0c\u5171 " + itemCount + " \u4e2a\u7ed3\u679c\u3002";
    }

    private String normalizeSearchText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        aiExecutor.shutdownNow();
    }

    private static class CachedRecommendation {
        final DeepSeekClient.RecommendationResult result;
        final long cachedAt;

        CachedRecommendation(DeepSeekClient.RecommendationResult result, long cachedAt) {
            this.result = result;
            this.cachedAt = cachedAt;
        }
    }

    private static class CachedSearch {
        final DeepSeekClient.SearchResult result;
        final long cachedAt;

        CachedSearch(DeepSeekClient.SearchResult result, long cachedAt) {
            this.result = result;
            this.cachedAt = cachedAt;
        }
    }

    private static class ScoredSearchCandidate {
        final Item item;
        final double score;

        ScoredSearchCandidate(Item item, double score) {
            this.item = item;
            this.score = score;
        }
    }

    public static class DiscoveryState {
        private final boolean loading;
        private final String searchQuery;
        private final String selectedCategory;
        private final DiscoveryFeedMode feedMode;
        private final List<Item> items;
        private final User currentUser;
        private final Map<Long, String> aiRecommendationReasons;
        private final boolean aiRecommendationEnabled;
        private final boolean aiSearchEnabled;
        private final DiscoveryUiMapper.RecommendationPanel recommendationPanel;
        private final String aiSearchStatus;
        private final String emptyStateText;

        public DiscoveryState(boolean loading, String searchQuery, String selectedCategory,
                              DiscoveryFeedMode feedMode, List<Item> items, User currentUser,
                              Map<Long, String> aiRecommendationReasons,
                              boolean aiRecommendationEnabled,
                              boolean aiSearchEnabled,
                              DiscoveryUiMapper.RecommendationPanel recommendationPanel,
                              String aiSearchStatus,
                              String emptyStateText) {
            this.loading = loading;
            this.searchQuery = searchQuery;
            this.selectedCategory = selectedCategory;
            this.feedMode = feedMode;
            this.items = items;
            this.currentUser = currentUser;
            this.aiRecommendationReasons = aiRecommendationReasons == null
                    ? Collections.emptyMap()
                    : aiRecommendationReasons;
            this.aiRecommendationEnabled = aiRecommendationEnabled;
            this.aiSearchEnabled = aiSearchEnabled;
            this.recommendationPanel = recommendationPanel;
            this.aiSearchStatus = aiSearchStatus == null ? "" : aiSearchStatus;
            this.emptyStateText = emptyStateText;
        }

        public boolean isLoading() {
            return loading;
        }

        public String getSearchQuery() {
            return searchQuery;
        }

        public String getSelectedCategory() {
            return selectedCategory;
        }

        public DiscoveryFeedMode getFeedMode() {
            return feedMode;
        }

        public List<Item> getItems() {
            return items;
        }

        public User getCurrentUser() {
            return currentUser;
        }

        public Map<Long, String> getAiRecommendationReasons() {
            return aiRecommendationReasons;
        }

        public boolean isAiRecommendationEnabled() {
            return aiRecommendationEnabled;
        }

        public boolean isAiSearchEnabled() {
            return aiSearchEnabled;
        }

        public DiscoveryUiMapper.RecommendationPanel getRecommendationPanel() {
            return recommendationPanel;
        }

        public String getAiSearchStatus() {
            return aiSearchStatus;
        }

        public String getEmptyStateText() {
            return emptyStateText;
        }
    }
}
