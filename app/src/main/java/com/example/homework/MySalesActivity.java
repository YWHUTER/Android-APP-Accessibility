package com.example.homework;

import com.example.homework.util.A11y;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.data.MessageRepository;
import com.example.homework.data.OrderRepository;
import com.example.homework.model.OrderRecord;
import com.example.homework.ui.OrderListAdapter;
import com.example.homework.ui.OrderListUiMapper;
import com.example.homework.ui.OrderListViewModel;

import java.util.ArrayList;
import java.util.Locale;

public class MySalesActivity extends AppCompatActivity {
    private OrderRepository orderRepository;
    private MessageRepository messageRepository;
    private TextView tvSummary;
    private TextView tvEmpty;
    private TextView tabAll;
    private TextView tabPending;
    private TextView tabShipped;
    private TextView tabCompleted;
    private TextView sortRecent;
    private TextView sortPriceHigh;
    private TextView sortPriceLow;
    private EditText etOrderSearch;
    private ProgressBar progressOrders;
    private RecyclerView recyclerOrders;
    private OrderListAdapter orderListAdapter;
    private OrderListViewModel viewModel;
    private boolean applyingSearchState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_sales);

        orderRepository = new OrderRepository(this);
        messageRepository = new MessageRepository(this);
        viewModel = new ViewModelProvider(this).get(OrderListViewModel.class);

        ImageButton btnBack = findViewById(R.id.btn_back);
        tvSummary = findViewById(R.id.tv_order_summary);
        tvEmpty = findViewById(R.id.tv_order_empty);
        etOrderSearch = findViewById(R.id.et_order_search);
        tabAll = findViewById(R.id.tab_orders_all);
        tabPending = findViewById(R.id.tab_orders_pending);
        tabShipped = findViewById(R.id.tab_orders_shipped);
        tabCompleted = findViewById(R.id.tab_orders_completed);
        sortRecent = findViewById(R.id.tab_sort_recent);
        sortPriceHigh = findViewById(R.id.tab_sort_price_high);
        sortPriceLow = findViewById(R.id.tab_sort_price_low);
        progressOrders = findViewById(R.id.progress_orders);
        recyclerOrders = findViewById(R.id.recycler_order_list);

        btnBack.setOnClickListener(v -> finish());
        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupSorting();
        viewModel.getUiState().observe(this, this::renderState);
        viewModel.initialize(OrderListViewModel.Mode.SELLER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    private void renderState(OrderListViewModel.OrderListState state) {
        if (!etOrderSearch.getText().toString().equals(state.getSearchQuery())) {
            applyingSearchState = true;
            etOrderSearch.setText(state.getSearchQuery());
            etOrderSearch.setSelection(etOrderSearch.getText().length());
            applyingSearchState = false;
        }
        tvSummary.setText(state.getSummary());
        tvEmpty.setText(state.getEmptyText());
        tvEmpty.setVisibility(!state.isLoading() && state.getOrders().isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        progressOrders.setVisibility(state.isLoading() ? android.view.View.VISIBLE : android.view.View.GONE);
        orderListAdapter.submitList(new ArrayList<>(state.getOrders()));
        updateFilterTabs(state);
        updateSortTabs(state);
        updateEmptyAction(state);
    }

    private void setupSearch() {
        etOrderSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (applyingSearchState) {
                    return;
                }
                viewModel.setSearchQuery(s.toString());
            }
        });
    }

    private void setupFilters() {
        tabAll.setOnClickListener(v -> viewModel.setStatusFilter(OrderListViewModel.StatusFilter.ALL));
        tabPending.setOnClickListener(v -> viewModel.setStatusFilter(OrderListViewModel.StatusFilter.PENDING));
        tabShipped.setOnClickListener(v -> viewModel.setStatusFilter(OrderListViewModel.StatusFilter.SHIPPED));
        tabCompleted.setOnClickListener(v -> viewModel.setStatusFilter(OrderListViewModel.StatusFilter.COMPLETED));
    }

    private void setupSorting() {
        sortRecent.setOnClickListener(v -> viewModel.setSortOption(OrderListUiMapper.SortOption.RECENT_ACTIVITY));
        sortPriceHigh.setOnClickListener(v -> viewModel.setSortOption(OrderListUiMapper.SortOption.PRICE_HIGH_TO_LOW));
        sortPriceLow.setOnClickListener(v -> viewModel.setSortOption(OrderListUiMapper.SortOption.PRICE_LOW_TO_HIGH));
    }

    private void updateFilterTabs(OrderListViewModel.OrderListState state) {
        tabAll.setText(String.format(Locale.getDefault(), "全部 %d", state.getAllCount()));
        tabPending.setText(String.format(Locale.getDefault(), "待发货 %d", state.getPendingCount()));
        tabShipped.setText(String.format(Locale.getDefault(), "已发货 %d", state.getShippedCount()));
        tabCompleted.setText(String.format(Locale.getDefault(), "已完成 %d", state.getCompletedCount()));

        styleFilterTab(tabAll, state.getActiveFilter() == OrderListViewModel.StatusFilter.ALL);
        styleFilterTab(tabPending, state.getActiveFilter() == OrderListViewModel.StatusFilter.PENDING);
        styleFilterTab(tabShipped, state.getActiveFilter() == OrderListViewModel.StatusFilter.SHIPPED);
        styleFilterTab(tabCompleted, state.getActiveFilter() == OrderListViewModel.StatusFilter.COMPLETED);
    }

    private void updateSortTabs(OrderListViewModel.OrderListState state) {
        styleFilterTab(sortRecent, state.getActiveSortOption() == OrderListUiMapper.SortOption.RECENT_ACTIVITY);
        styleFilterTab(sortPriceHigh, state.getActiveSortOption() == OrderListUiMapper.SortOption.PRICE_HIGH_TO_LOW);
        styleFilterTab(sortPriceLow, state.getActiveSortOption() == OrderListUiMapper.SortOption.PRICE_LOW_TO_HIGH);
    }

    private void updateEmptyAction(OrderListViewModel.OrderListState state) {
        tvEmpty.setOnClickListener(v -> {
            if (OrderListUiMapper.hasQuery(state.getSearchQuery())) {
                viewModel.setSearchQuery("");
                return;
            }
            openPostEditor();
        });
    }

    private void styleFilterTab(TextView tab, boolean active) {
        tab.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        tab.setTextColor(getColor(active ? R.color.white : R.color.text_secondary));
        tab.setAlpha(active ? 1f : 0.9f);
    }

    private void confirmShipment(OrderRecord order) {
        new AlertDialog.Builder(this)
                .setTitle("确认发货")
                .setMessage("确认后，订单状态会更新为已发货，等待买家确认收货。")
                .setPositiveButton("确认发货", (dialog, which) -> {
                    if (orderRepository.markShipped(order.getId())) {
                        A11y.toast(this, "已发货，等待买家收货");
                    } else {
                        A11y.toast(this, "订单状态已变化，请刷新后再试");
                    }
                    viewModel.refresh();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openChat(OrderRecord order) {
        long conversationId = messageRepository.getOrCreateConversation(order.getItemId(), order.getBuyerId(), order.getSellerId());
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
        startActivity(intent);
    }

    private void openDetail(long itemId) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra(ItemDetailActivity.EXTRA_ITEM_ID, itemId);
        startActivity(intent);
    }

    private void openPostEditor() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_POST);
        startActivity(intent);
    }

    private void setupRecyclerView() {
        orderListAdapter = new OrderListAdapter(OrderListAdapter.Mode.SELLER, new OrderListAdapter.OrderActionListener() {
            @Override
            public void onSecondaryAction(OrderRecord order) {
                openChat(order);
            }

            @Override
            public void onPrimaryAction(OrderRecord order) {
                confirmShipment(order);
            }

            @Override
            public void onItemClick(OrderRecord order) {
                openDetail(order.getItemId());
            }
        });
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrders.setAdapter(orderListAdapter);
        recyclerOrders.setItemAnimator(null);
    }
}
