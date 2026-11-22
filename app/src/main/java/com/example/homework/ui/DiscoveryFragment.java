package com.example.homework.ui;

import com.example.homework.util.A11y;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.MainNavigator;
import com.example.homework.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscoveryFragment extends Fragment {
    private static final String STATE_SEARCH_QUERY = "state_search_query";
    private static final String STATE_SELECTED_CATEGORY = "state_selected_category";
    private static final String STATE_FEED_MODE = "state_feed_mode";
    private static final String STATE_AI_SEARCH_ENABLED = "state_ai_search_enabled";
    private static final List<Integer> CATEGORY_TILE_IDS = Arrays.asList(
            R.id.category_textbooks,
            R.id.category_electronics,
            R.id.category_dorm,
            R.id.category_fashion,
            R.id.category_sports,
            R.id.category_cycles,
            R.id.category_art,
            R.id.category_beauty,
            R.id.category_food,
            R.id.category_more
    );

    private MainNavigator navigator;
    private DiscoveryViewModel viewModel;
    private DiscoveryItemAdapter adapter;

    private EditText etSearch;
    private RecyclerView recyclerItems;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView btnEmptyResetFilters;
    private TextView tvTabFollow;
    private TextView tvTabRecommend;
    private TextView tvTabLatest;
    private View indicatorFollow;
    private View layoutRecommendationPanel;
    private TextView tvRecommendationTitle;
    private TextView tvRecommendationSummary;
    private TextView tvRecommendationChipProfile;
    private TextView tvRecommendationChipInterest;
    private TextView tvRecommendationChipCount;
    private TextView btnAiRecommendToggle;
    private TextView btnLocalRecommendToggle;
    private TextView btnAiSearchToggle;
    private TextView btnLocalSearchToggle;
    private TextView tvAiSearchStatus;

    private boolean applyingUiState;
    private String selectedCategory = DiscoveryUiMapper.CATEGORY_ALL;
    private DiscoveryFeedMode currentFeedMode = DiscoveryFeedMode.FOLLOW;
    private String restoredQuery = "";
    private String restoredCategory = DiscoveryUiMapper.CATEGORY_ALL;
    private DiscoveryFeedMode restoredFeedMode = DiscoveryFeedMode.FOLLOW;
    private boolean restoredAiSearchEnabled;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        navigator = (MainNavigator) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            restoredQuery = savedInstanceState.getString(STATE_SEARCH_QUERY, "");
            restoredCategory = savedInstanceState.getString(
                    STATE_SELECTED_CATEGORY,
                    DiscoveryUiMapper.CATEGORY_ALL
            );
            restoredFeedMode = DiscoveryFeedMode.fromName(
                    savedInstanceState.getString(STATE_FEED_MODE, DiscoveryFeedMode.FOLLOW.name())
            );
            restoredAiSearchEnabled = savedInstanceState.getBoolean(STATE_AI_SEARCH_ENABLED, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discovery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DiscoveryViewModel.class);

        etSearch = view.findViewById(R.id.et_search);
        recyclerItems = view.findViewById(R.id.recycler_discovery_items);
        progressBar = view.findViewById(R.id.progress_discovery);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        btnEmptyResetFilters = view.findViewById(R.id.btn_empty_reset_filters);
        tvTabFollow = view.findViewById(R.id.tv_tab_follow);
        tvTabFollow.setText("\u53d1\u73b0");
        tvTabRecommend = view.findViewById(R.id.tv_tab_recommend);
        tvTabLatest = view.findViewById(R.id.tv_tab_latest);
        indicatorFollow = view.findViewById(R.id.indicator_tab_follow);
        layoutRecommendationPanel = view.findViewById(R.id.layout_recommendation_panel);
        tvRecommendationTitle = view.findViewById(R.id.tv_recommendation_title);
        tvRecommendationSummary = view.findViewById(R.id.tv_recommendation_summary);
        tvRecommendationChipProfile = view.findViewById(R.id.tv_recommendation_chip_profile);
        tvRecommendationChipInterest = view.findViewById(R.id.tv_recommendation_chip_interest);
        tvRecommendationChipCount = view.findViewById(R.id.tv_recommendation_chip_count);
        btnAiRecommendToggle = view.findViewById(R.id.btn_ai_recommend_toggle);
        btnLocalRecommendToggle = view.findViewById(R.id.btn_local_recommend_toggle);
        btnAiSearchToggle = view.findViewById(R.id.btn_ai_search_toggle);
        btnLocalSearchToggle = view.findViewById(R.id.btn_local_search_toggle);
        tvAiSearchStatus = view.findViewById(R.id.tv_ai_search_status);

        setupRecyclerView();
        setupSearch(view);
        setupFeedTabs(view);
        setupCategoryTiles(view);

        view.findViewById(R.id.btn_scan).setOnClickListener(v -> refreshDiscovery());
        view.findViewById(R.id.iv_search_camera).setOnClickListener(v -> navigator.openPostEditor(-1L));
        btnAiRecommendToggle.setOnClickListener(v -> viewModel.setAiRecommendationsEnabled(true));
        btnLocalRecommendToggle.setOnClickListener(v -> viewModel.setAiRecommendationsEnabled(false));
        btnAiSearchToggle.setOnClickListener(v -> viewModel.setAiSearchEnabled(true));
        btnLocalSearchToggle.setOnClickListener(v -> viewModel.setAiSearchEnabled(false));
        btnEmptyResetFilters.setOnClickListener(v -> clearFilters());
        tvEmptyState.setOnClickListener(v -> {
            DiscoveryViewModel.DiscoveryState state = viewModel.getUiState().getValue();
            if (state != null && hasActiveFilters(state)) {
                clearFilters();
            }
        });

        viewModel.initialize(restoredQuery, restoredCategory, restoredFeedMode, restoredAiSearchEnabled);
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshImmediate();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SEARCH_QUERY, etSearch.getText().toString().trim());
        outState.putString(STATE_SELECTED_CATEGORY, selectedCategory);
        outState.putString(STATE_FEED_MODE, currentFeedMode.name());
        DiscoveryViewModel.DiscoveryState state = viewModel == null ? null : viewModel.getUiState().getValue();
        outState.putBoolean(STATE_AI_SEARCH_ENABLED, state != null && state.isAiSearchEnabled());
    }

    private void setupRecyclerView() {
        adapter = new DiscoveryItemAdapter(navigator::openItemDetail);
        recyclerItems.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerItems.setAdapter(adapter);
        recyclerItems.setItemAnimator(null);
        recyclerItems.addItemDecoration(new DiscoverySpacingDecoration(dp(12)));
    }

    private void setupSearch(View root) {
        etSearch.setHint(DiscoveryUiMapper.SEARCH_HINT);
        View searchBox = root.findViewById(R.id.layout_search_box);
        A11y.asButton(searchBox, getString(R.string.cd_search_box));
        searchBox.setOnClickListener(v -> focusSearchField());
        root.findViewById(R.id.iv_search_icon).setOnClickListener(v -> focusSearchField());
        etSearch.setOnClickListener(v -> focusSearchField());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch();
                return true;
            }
            return false;
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (applyingUiState) {
                    return;
                }
                viewModel.setSearchQuery(s.toString().trim());
            }
        });

        updateCategorySelection(root);
    }

    private void focusSearchField() {
        etSearch.requestFocus();
        etSearch.postDelayed(() -> {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 80L);
    }

    private void setupFeedTabs(View root) {
        View tabFollow = root.findViewById(R.id.tab_follow);
        A11y.asButton(tabFollow, getString(R.string.cd_tab_follow));
        A11y.asButton(tvTabRecommend, getString(R.string.cd_tab_recommend));
        A11y.asButton(tvTabLatest, getString(R.string.cd_tab_latest));
        tabFollow.setOnClickListener(v -> switchFeedMode(DiscoveryFeedMode.FOLLOW));
        tvTabRecommend.setOnClickListener(v -> switchFeedMode(DiscoveryFeedMode.RECOMMEND));
        tvTabLatest.setOnClickListener(v -> switchFeedMode(DiscoveryFeedMode.LATEST));
        updateFeedTabSelection();
    }

    private void setupCategoryTiles(View root) {
        bindCategoryTile(root, R.id.category_textbooks, "教材");
        bindCategoryTile(root, R.id.category_electronics, "数码");
        bindCategoryTile(root, R.id.category_dorm, "宿舍");
        bindCategoryTile(root, R.id.category_fashion, "穿搭");
        bindCategoryTile(root, R.id.category_sports, "运动");
        bindCategoryTile(root, R.id.category_cycles, "骑行");
        bindCategoryTile(root, R.id.category_art, "手作");
        bindCategoryTile(root, R.id.category_beauty, "美妆");
        bindCategoryTile(root, R.id.category_food, "美食");
        bindCategoryTile(root, R.id.category_more, DiscoveryUiMapper.CATEGORY_ALL);
    }

    private void bindCategoryTile(View root, int tileId, String category) {
        View tile = root.findViewById(tileId);
        // 分类格子是 LinearLayout + 图标 + 文字，需合并为一个「按钮」节点朗读
        A11y.asButton(tile, getString(R.string.cd_category_tile, category));
        tile.setTag(category);
        tile.setOnClickListener(v -> {
            String tappedCategory = (String) v.getTag();
            if (TextUtils.equals(selectedCategory, tappedCategory)) {
                viewModel.setSelectedCategory(DiscoveryUiMapper.CATEGORY_ALL);
            } else {
                viewModel.setSelectedCategory(tappedCategory);
            }
            recyclerItems.scrollToPosition(0);
        });
    }

    private void switchFeedMode(DiscoveryFeedMode feedMode) {
        viewModel.setFeedMode(feedMode);
        recyclerItems.scrollToPosition(0);
    }

    private void clearFilters() {
        applyingUiState = true;
        etSearch.setText("");
        applyingUiState = false;
        recyclerItems.scrollToPosition(0);
        viewModel.resetFilters();
    }

    private void refreshDiscovery() {
        submitSearch();
        recyclerItems.scrollToPosition(0);
    }

    private void renderState(DiscoveryViewModel.DiscoveryState state) {
        currentFeedMode = state.getFeedMode();
        selectedCategory = state.getSelectedCategory();

        if (!TextUtils.equals(etSearch.getText().toString(), state.getSearchQuery())) {
            applyingUiState = true;
            etSearch.setText(state.getSearchQuery());
            etSearch.setSelection(etSearch.getText().length());
            applyingUiState = false;
        }

        updateFeedTabSelection();
        updateCategorySelection(getView());
        updateRecommendationPanel(state);
        updateSearchModeSegments(state);

        adapter.updateContext(state.getFeedMode(), state.getCurrentUser(), state.getAiRecommendationReasons());
        adapter.submitList(new ArrayList<>(state.getItems()));

        boolean showEmptyState = !state.isLoading() && state.getItems().isEmpty();
        boolean showResetAction = showEmptyState && hasActiveFilters(state);
        progressBar.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        tvEmptyState.setText(showResetAction
                ? "没有找到符合当前筛选条件的闲置，试试放宽条件。"
                : state.getEmptyStateText());
        tvEmptyState.setVisibility(showEmptyState ? View.VISIBLE : View.GONE);
        tvEmptyState.setClickable(showResetAction);
        btnEmptyResetFilters.setVisibility(showResetAction ? View.VISIBLE : View.GONE);
    }

    private void updateRecommendationPanel(DiscoveryViewModel.DiscoveryState state) {
        DiscoveryUiMapper.RecommendationPanel panel = state.getRecommendationPanel();
        if (panel == null || !panel.isVisible()) {
            layoutRecommendationPanel.setVisibility(View.GONE);
            return;
        }

        layoutRecommendationPanel.setVisibility(View.VISIBLE);
        tvRecommendationTitle.setText(panel.getTitle());
        tvRecommendationSummary.setText(panel.getSummary());
        tvRecommendationChipProfile.setText(panel.getProfileChip());
        tvRecommendationChipInterest.setText(panel.getInterestChip());
        tvRecommendationChipCount.setText(panel.getCountChip());
        btnAiRecommendToggle.setVisibility(
                state.getFeedMode() == DiscoveryFeedMode.RECOMMEND ? View.VISIBLE : View.GONE
        );
        btnAiRecommendToggle.setText(state.isAiRecommendationEnabled() ? "本地推荐" : "AI推荐");
        btnLocalRecommendToggle.setVisibility(
                state.getFeedMode() == DiscoveryFeedMode.RECOMMEND ? View.VISIBLE : View.GONE
        );
        updateRecommendationModeSegments(state);
    }

    private void updateAiRecommendToggleLabel(DiscoveryViewModel.DiscoveryState state) {
        btnAiRecommendToggle.setText(state.isAiRecommendationEnabled()
                ? "\u5207\u56de\u672c\u5730\u63a8\u8350"
                : "\u542f\u7528 AI \u63a8\u8350");
    }

    private void updateRecommendationModeSegments(DiscoveryViewModel.DiscoveryState state) {
        boolean aiEnabled = state.isAiRecommendationEnabled();
        btnAiRecommendToggle.setText("AI \u63a8\u8350");
        btnLocalRecommendToggle.setText("\u672c\u5730\u7b97\u6cd5");
        styleRecommendationModeSegment(btnAiRecommendToggle, aiEnabled);
        styleRecommendationModeSegment(btnLocalRecommendToggle, !aiEnabled);
    }

    private void updateSearchModeSegments(DiscoveryViewModel.DiscoveryState state) {
        boolean aiEnabled = state.isAiSearchEnabled();
        styleRecommendationModeSegment(btnAiSearchToggle, aiEnabled);
        styleRecommendationModeSegment(btnLocalSearchToggle, !aiEnabled);
        String status = state.getAiSearchStatus();
        tvAiSearchStatus.setText(status);
        tvAiSearchStatus.setVisibility(aiEnabled && !TextUtils.isEmpty(status) ? View.VISIBLE : View.GONE);
    }

    private void styleRecommendationModeSegment(TextView segment, boolean active) {
        segment.setBackgroundResource(active
                ? R.drawable.bg_recommend_segment_active
                : R.drawable.bg_recommend_segment_inactive);
        segment.setTextColor(ContextCompat.getColor(
                requireContext(),
                active ? R.color.primary_blue : R.color.text_tertiary
        ));
    }

    private void updateFeedTabSelection() {
        boolean followSelected = currentFeedMode == DiscoveryFeedMode.FOLLOW;
        boolean recommendSelected = currentFeedMode == DiscoveryFeedMode.RECOMMEND;
        boolean latestSelected = currentFeedMode == DiscoveryFeedMode.LATEST;

        indicatorFollow.setVisibility(followSelected ? View.VISIBLE : View.INVISIBLE);
        styleFeedTab(tvTabFollow, followSelected);
        styleFeedTab(tvTabRecommend, recommendSelected);
        styleFeedTab(tvTabLatest, latestSelected);
        // 让 TalkBack 朗读当前选中的信息流
        View followTab = getView() == null ? null : getView().findViewById(R.id.tab_follow);
        if (followTab != null) {
            followTab.setSelected(followSelected);
        }
        tvTabRecommend.setSelected(recommendSelected);
        tvTabLatest.setSelected(latestSelected);
    }

    private void styleFeedTab(TextView tab, boolean active) {
        tab.setTextColor(ContextCompat.getColor(
                requireContext(),
                active ? R.color.primary_blue : R.color.text_tertiary
        ));
        tab.setAlpha(active ? 1f : 0.78f);
        tab.setScaleX(active ? 1f : 0.96f);
        tab.setScaleY(active ? 1f : 0.96f);
    }

    private void updateCategorySelection(@Nullable View root) {
        if (root == null) {
            return;
        }

        boolean allSelected = DiscoveryUiMapper.CATEGORY_ALL.equals(selectedCategory);
        for (int tileId : CATEGORY_TILE_IDS) {
            View tile = root.findViewById(tileId);
            boolean active = !allSelected && TextUtils.equals(selectedCategory, (String) tile.getTag());
            tile.setAlpha(active ? 1f : 0.92f);
            tile.setSelected(active);
            tile.setScaleX(active ? 1.06f : 1f);
            tile.setScaleY(active ? 1.06f : 1f);

            ViewGroup group = (ViewGroup) tile;
            TextView iconView = (TextView) group.getChildAt(0);
            TextView labelView = (TextView) group.getChildAt(1);
            iconView.setAlpha(active ? 1f : 0.82f);
            labelView.setTextColor(ContextCompat.getColor(
                    requireContext(),
                    active ? R.color.primary_blue : R.color.text_primary
            ));
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private boolean hasActiveFilters(DiscoveryViewModel.DiscoveryState state) {
        return state != null
                && (!TextUtils.isEmpty(state.getSearchQuery())
                || !DiscoveryUiMapper.CATEGORY_ALL.equals(state.getSelectedCategory())
                || state.getFeedMode() != DiscoveryFeedMode.FOLLOW);
    }

    private void submitSearch() {
        etSearch.clearFocus();
        InputMethodManager inputMethodManager =
                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
        recyclerItems.scrollToPosition(0);
        viewModel.submitSearch();
    }

    private static class DiscoverySpacingDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        DiscoverySpacingDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            if (!(layoutParams instanceof GridLayoutManager.LayoutParams)) {
                outRect.bottom = spacing;
                return;
            }

            GridLayoutManager.LayoutParams params = (GridLayoutManager.LayoutParams) layoutParams;
            int halfSpacing = spacing / 2;
            if (params.getSpanIndex() == 0) {
                outRect.left = 0;
                outRect.right = halfSpacing;
            } else {
                outRect.left = halfSpacing;
                outRect.right = 0;
            }
            outRect.bottom = spacing;
        }
    }
}
