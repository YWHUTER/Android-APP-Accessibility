package com.example.homework.ui;

import com.example.homework.util.A11y;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.ChatActivity;
import com.example.homework.MainNavigator;
import com.example.homework.R;
import com.example.homework.model.ConversationSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageFragment extends Fragment {
    private static final String STATE_CURRENT_TAB = "state_current_tab";
    private static final String STATE_QUERY = "state_query";
    private static final String STATE_UNREAD_ONLY = "state_unread_only";

    private MessageViewModel viewModel;
    private MainNavigator navigator;
    private ConversationAdapter conversationAdapter;
    private RecyclerView recyclerConversations;
    private EditText etMessageSearch;
    private ProgressBar progressBar;
    private TextView tabAll;
    private TextView tabBuying;
    private TextView tabSelling;
    private TextView tvHeaderUnreadBadge;
    private TextView tvUnreadSummaryTitle;
    private TextView tvUnreadSummarySubtitle;
    private TextView tvSummaryUnreadCount;
    private TextView tvSummaryBuySell;
    private TextView tvSummaryToggleUnread;
    private TextView tvEmptyState;
    private View layoutUnreadSummary;

    private int currentTab = MessageUiMapper.TAB_ALL;
    private boolean loadingData;
    private boolean unreadOnly;
    private List<ConversationSummary> allConversations = new ArrayList<>();

    public MessageFragment() {
        super(R.layout.fragment_message);
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        navigator = (MainNavigator) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt(STATE_CURRENT_TAB, MessageUiMapper.TAB_ALL);
            unreadOnly = savedInstanceState.getBoolean(STATE_UNREAD_ONLY, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MessageViewModel.class);

        recyclerConversations = view.findViewById(R.id.recycler_conversations);
        etMessageSearch = view.findViewById(R.id.et_message_search);
        progressBar = view.findViewById(R.id.progress_message);
        tabAll = view.findViewById(R.id.tab_all);
        tabBuying = view.findViewById(R.id.tab_buying);
        tabSelling = view.findViewById(R.id.tab_selling);
        tvHeaderUnreadBadge = view.findViewById(R.id.tv_header_unread_badge);
        tvUnreadSummaryTitle = view.findViewById(R.id.tv_unread_summary_title);
        tvUnreadSummarySubtitle = view.findViewById(R.id.tv_unread_summary_subtitle);
        tvSummaryUnreadCount = view.findViewById(R.id.tv_summary_unread_count);
        tvSummaryBuySell = view.findViewById(R.id.tv_summary_buy_sell);
        tvSummaryToggleUnread = view.findViewById(R.id.tv_summary_toggle_unread);
        tvEmptyState = view.findViewById(R.id.tv_message_empty);
        layoutUnreadSummary = view.findViewById(R.id.layout_unread_summary);

        setupRecyclerView();
        setupTabs();
        setupSearch(savedInstanceState);
        setupSummaryActions();

        view.findViewById(R.id.fab_new_message).setOnClickListener(v -> {
            navigator.openDiscovery();
            A11y.toast(requireContext(), "先去发现页挑一件商品，再和卖家发起私信吧。");
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderLoadedState);
        updateTabUI();
        viewModel.refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_TAB, currentTab);
        outState.putString(STATE_QUERY, etMessageSearch.getText().toString());
        outState.putBoolean(STATE_UNREAD_ONLY, unreadOnly);
    }

    private void setupRecyclerView() {
        conversationAdapter = new ConversationAdapter(conversation -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
            startActivity(intent);
        });
        recyclerConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerConversations.setAdapter(conversationAdapter);
        recyclerConversations.setItemAnimator(null);
    }

    private void setupTabs() {
        View.OnClickListener tabClickListener = v -> {
            if (v == tabAll) {
                currentTab = MessageUiMapper.TAB_ALL;
            } else if (v == tabBuying) {
                currentTab = MessageUiMapper.TAB_BUYING;
            } else if (v == tabSelling) {
                currentTab = MessageUiMapper.TAB_SELLING;
            }
            updateTabUI();
            renderConversations();
        };

        tabAll.setOnClickListener(tabClickListener);
        tabBuying.setOnClickListener(tabClickListener);
        tabSelling.setOnClickListener(tabClickListener);
    }

    private void setupSearch(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            etMessageSearch.setText(savedInstanceState.getString(STATE_QUERY, ""));
            etMessageSearch.setSelection(etMessageSearch.getText().length());
        }

        etMessageSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                renderConversations();
            }
        });
    }

    private void setupSummaryActions() {
        View.OnClickListener unreadClickListener = v -> toggleUnreadOnly();
        layoutUnreadSummary.setOnClickListener(unreadClickListener);
        tvHeaderUnreadBadge.setOnClickListener(unreadClickListener);
        tvSummaryToggleUnread.setOnClickListener(unreadClickListener);
    }

    private void renderLoadedState(MessageViewModel.MessageState state) {
        loadingData = state.isLoading();
        allConversations = state.getConversations();
        MessageUiMapper.Summary summary = MessageUiMapper.buildSummary(allConversations);
        if (unreadOnly && summary.getUnreadMessages() <= 0) {
            unreadOnly = false;
        }

        progressBar.setVisibility(loadingData ? View.VISIBLE : View.GONE);
        updateSummaryCard(summary);
        updateTabLabels(summary);
        renderConversations();
        navigator.refreshNavigationIndicators();
    }

    private void updateSummaryCard(MessageUiMapper.Summary summary) {
        if (summary.getUnreadMessages() > 0) {
            tvUnreadSummaryTitle.setText("有 " + summary.getUnreadMessages() + " 条新消息待查看");
            tvUnreadSummarySubtitle.setText("来自 "
                    + summary.getUnreadConversations() + " 个会话，优先处理最新回复会更高效。");
            tvSummaryUnreadCount.setText("未读 " + summary.getUnreadMessages());
            tvHeaderUnreadBadge.setVisibility(View.VISIBLE);
            tvHeaderUnreadBadge.setText(MessageUiMapper.formatUnreadCount(summary.getUnreadMessages()));
        } else {
            tvUnreadSummaryTitle.setText("消息都处理完了");
            tvUnreadSummarySubtitle.setText("新的买卖动态来了以后，会第一时间显示在这里。");
            tvSummaryUnreadCount.setText("未读 0");
            tvHeaderUnreadBadge.setVisibility(View.GONE);
        }

        tvSummaryBuySell.setText(String.format(
                Locale.getDefault(),
                "买入 %d · 卖出 %d",
                summary.getBuyingCount(),
                summary.getSellingCount()
        ));
        updateUnreadToggle(summary);
    }

    private void updateUnreadToggle(MessageUiMapper.Summary summary) {
        boolean enabled = summary.getUnreadMessages() > 0 || unreadOnly;
        tvSummaryToggleUnread.setText(unreadOnly ? "显示全部" : "只看未读");
        tvSummaryToggleUnread.setEnabled(enabled);
        tvSummaryToggleUnread.setAlpha(enabled ? 1f : 0.56f);
        tvSummaryToggleUnread.setBackgroundResource(unreadOnly ? R.drawable.bg_chip_active : R.drawable.bg_field);
        tvSummaryToggleUnread.setTextColor(ContextCompat.getColor(
                requireContext(),
                unreadOnly ? R.color.white : R.color.primary_blue
        ));
        layoutUnreadSummary.setAlpha(enabled ? 1f : 0.94f);
    }

    private void updateTabLabels(MessageUiMapper.Summary summary) {
        tabAll.setText(String.format(Locale.getDefault(), "全部消息 %d", allConversations.size()));
        tabBuying.setText(String.format(Locale.getDefault(), "买入 %d", summary.getBuyingCount()));
        tabSelling.setText(String.format(Locale.getDefault(), "卖出 %d", summary.getSellingCount()));
        updateTabUI();
    }

    private void updateTabUI() {
        styleTab(tabAll, currentTab == MessageUiMapper.TAB_ALL);
        styleTab(tabBuying, currentTab == MessageUiMapper.TAB_BUYING);
        styleTab(tabSelling, currentTab == MessageUiMapper.TAB_SELLING);
    }

    private void styleTab(TextView tab, boolean active) {
        tab.setTextColor(ContextCompat.getColor(requireContext(),
                active ? R.color.primary_blue : R.color.text_tertiary));
        tab.setAlpha(active ? 1f : 0.78f);
    }

    private void renderConversations() {
        List<ConversationSummary> filteredList = MessageUiMapper.filterConversations(
                allConversations,
                currentTab,
                etMessageSearch.getText().toString(),
                unreadOnly
        );
        conversationAdapter.submitList(new ArrayList<>(filteredList));

        if (!loadingData && filteredList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(MessageUiMapper.resolveEmptyStateText(
                    currentTab,
                    etMessageSearch.getText().toString(),
                    unreadOnly
            ));
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void toggleUnreadOnly() {
        MessageUiMapper.Summary summary = MessageUiMapper.buildSummary(allConversations);
        if (summary.getUnreadMessages() <= 0 && !unreadOnly) {
            A11y.toast(requireContext(), "当前没有未读消息。");
            return;
        }
        unreadOnly = !unreadOnly;
        updateSummaryCard(summary);
        renderConversations();
    }
}
