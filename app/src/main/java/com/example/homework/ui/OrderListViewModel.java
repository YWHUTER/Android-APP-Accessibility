package com.example.homework.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.homework.data.OrderRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.model.OrderRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderListViewModel extends AndroidViewModel {
    public enum Mode {
        BUYER,
        SELLER
    }

    public enum StatusFilter {
        ALL,
        PENDING,
        SHIPPED,
        COMPLETED
    }

    private final OrderRepository orderRepository;
    private final SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<OrderListState> uiState = new MutableLiveData<>();

    private boolean initialized;
    private Mode mode = Mode.BUYER;
    private StatusFilter activeFilter = StatusFilter.ALL;
    private String searchQuery = "";
    private OrderListUiMapper.SortOption activeSortOption = OrderListUiMapper.SortOption.RECENT_ACTIVITY;
    private List<OrderRecord> allOrders = Collections.emptyList();
    private int requestVersion;

    public OrderListViewModel(@NonNull Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
        sessionManager = new SessionManager(application);
        uiState.setValue(OrderListState.empty());
    }

    public LiveData<OrderListState> getUiState() {
        return uiState;
    }

    public void initialize(Mode requestedMode) {
        if (!initialized || requestedMode != mode) {
            initialized = true;
            mode = requestedMode;
            activeFilter = StatusFilter.ALL;
            searchQuery = "";
            activeSortOption = OrderListUiMapper.SortOption.RECENT_ACTIVITY;
            refresh();
        }
    }

    public void setStatusFilter(StatusFilter filter) {
        if (filter == null || filter == activeFilter) {
            return;
        }
        activeFilter = filter;
        OrderListState current = uiState.getValue();
        boolean loading = current != null && current.isLoading();
        uiState.postValue(buildState(loading, allOrders));
    }

    public void setSearchQuery(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.equals(searchQuery)) {
            return;
        }
        searchQuery = normalized;
        OrderListState current = uiState.getValue();
        boolean loading = current != null && current.isLoading();
        uiState.postValue(buildState(loading, allOrders));
    }

    public void setSortOption(OrderListUiMapper.SortOption sortOption) {
        if (sortOption == null || sortOption == activeSortOption) {
            return;
        }
        activeSortOption = sortOption;
        OrderListState current = uiState.getValue();
        boolean loading = current != null && current.isLoading();
        uiState.postValue(buildState(loading, allOrders));
    }

    public void refresh() {
        int requestId = ++requestVersion;
        uiState.postValue(buildState(true, allOrders));
        executor.execute(() -> {
            long userId = sessionManager.getLoggedInUserId();
            List<OrderRecord> orders = mode == Mode.BUYER
                    ? orderRepository.getOrdersByBuyer(userId)
                    : orderRepository.getOrdersBySeller(userId);
            if (requestId == requestVersion) {
                allOrders = new ArrayList<>(orders);
                uiState.postValue(buildState(false, orders));
            }
        });
    }

    private OrderListState buildState(boolean loading, List<OrderRecord> sourceOrders) {
        List<OrderRecord> orders = sourceOrders == null ? Collections.emptyList() : sourceOrders;
        int pendingCount = countByStatus(orders, OrderRepository.STATUS_PENDING);
        int shippedCount = countByStatus(orders, OrderRepository.STATUS_SHIPPED);
        int completedCount = countByStatus(orders, OrderRepository.STATUS_COMPLETED);
        List<OrderRecord> visibleOrders = OrderListUiMapper.filterAndSort(
                orders,
                searchQuery,
                resolveStatus(activeFilter),
                mode == Mode.BUYER,
                activeSortOption
        );

        return new OrderListState(
                loading,
                new ArrayList<>(visibleOrders),
                buildSummary(orders.size(), visibleOrders.size(), pendingCount, shippedCount, completedCount),
                buildEmptyText(),
                activeFilter,
                searchQuery,
                activeSortOption,
                orders.size(),
                pendingCount,
                shippedCount,
                completedCount
        );
    }

    private int countByStatus(List<OrderRecord> orders, String status) {
        int count = 0;
        for (OrderRecord order : orders) {
            if (status.equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private String buildSummary(int totalCount, int visibleCount, int pendingCount, int shippedCount, int completedCount) {
        if (OrderListUiMapper.hasQuery(searchQuery)) {
            if (activeFilter == StatusFilter.ALL) {
                return String.format(
                        Locale.getDefault(),
                        "搜索到 %d 笔匹配订单，共 %d 笔记录。",
                        visibleCount,
                        totalCount
                );
            }
            return String.format(
                    Locale.getDefault(),
                    "在%s中搜索到 %d 笔匹配订单。",
                    resolveFilterLabel(activeFilter),
                    visibleCount
            );
        }

        if (activeFilter == StatusFilter.ALL) {
            if (mode == Mode.BUYER) {
                return String.format(
                        Locale.getDefault(),
                        "你已买到 %d 件商品，待完成 %d 笔。",
                        totalCount,
                        pendingCount + shippedCount
                );
            }
            return String.format(
                    Locale.getDefault(),
                    "你已卖出 %d 件商品，待发货 %d 笔。",
                    totalCount,
                    pendingCount
            );
        }

        if (mode == Mode.BUYER) {
            if (activeFilter == StatusFilter.PENDING) {
                return String.format(Locale.getDefault(), "当前有 %d 笔订单待卖家发货。", pendingCount);
            }
            if (activeFilter == StatusFilter.SHIPPED) {
                return String.format(Locale.getDefault(), "当前有 %d 笔订单待你确认收货。", shippedCount);
            }
            return String.format(Locale.getDefault(), "已完成 %d 笔交易记录。", completedCount);
        }

        if (activeFilter == StatusFilter.PENDING) {
            return String.format(Locale.getDefault(), "当前有 %d 笔订单待你发货。", pendingCount);
        }
        if (activeFilter == StatusFilter.SHIPPED) {
            return String.format(Locale.getDefault(), "当前有 %d 笔订单在等买家收货。", shippedCount);
        }
        return String.format(Locale.getDefault(), "已完成 %d 笔卖出记录。", completedCount);
    }

    private String buildEmptyText() {
        if (OrderListUiMapper.hasQuery(searchQuery)) {
            return mode == Mode.BUYER
                    ? "没有找到匹配的订单，试试商品名、地点或卖家昵称。"
                    : "没有找到匹配的订单，试试商品名、地点或买家昵称。";
        }

        if (mode == Mode.BUYER) {
            if (activeFilter == StatusFilter.ALL) {
                return "还没有买到任何商品，去发现页看看吧。";
            }
            if (activeFilter == StatusFilter.PENDING) {
                return "暂无待发货订单，买到新商品后会显示在这里。";
            }
            if (activeFilter == StatusFilter.SHIPPED) {
                return "暂无待收货订单，卖家发货后会显示在这里。";
            }
            return "还没有已完成的订单记录。";
        }

        if (activeFilter == StatusFilter.ALL) {
            return "还没有卖出任何商品，等同学下单后这里会自动更新。";
        }
        if (activeFilter == StatusFilter.PENDING) {
            return "暂无待发货订单，有同学下单后会第一时间显示。";
        }
        if (activeFilter == StatusFilter.SHIPPED) {
            return "暂无待收货订单，发货后的订单会显示在这里。";
        }
        return "还没有已完成的卖出记录。";
    }

    private String resolveStatus(StatusFilter filter) {
        if (filter == StatusFilter.ALL) {
            return "";
        }
        if (filter == StatusFilter.PENDING) {
            return OrderRepository.STATUS_PENDING;
        }
        if (filter == StatusFilter.SHIPPED) {
            return OrderRepository.STATUS_SHIPPED;
        }
        return OrderRepository.STATUS_COMPLETED;
    }

    private String resolveFilterLabel(StatusFilter filter) {
        if (filter == StatusFilter.PENDING) {
            return "待发货";
        }
        if (filter == StatusFilter.SHIPPED) {
            return "已发货";
        }
        if (filter == StatusFilter.COMPLETED) {
            return "已完成";
        }
        return "全部订单";
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }

    public static class OrderListState {
        private final boolean loading;
        private final List<OrderRecord> orders;
        private final String summary;
        private final String emptyText;
        private final StatusFilter activeFilter;
        private final String searchQuery;
        private final OrderListUiMapper.SortOption activeSortOption;
        private final int allCount;
        private final int pendingCount;
        private final int shippedCount;
        private final int completedCount;

        public OrderListState(boolean loading, List<OrderRecord> orders, String summary, String emptyText,
                              StatusFilter activeFilter, String searchQuery,
                              OrderListUiMapper.SortOption activeSortOption, int allCount, int pendingCount,
                              int shippedCount, int completedCount) {
            this.loading = loading;
            this.orders = orders;
            this.summary = summary;
            this.emptyText = emptyText;
            this.activeFilter = activeFilter;
            this.searchQuery = searchQuery;
            this.activeSortOption = activeSortOption;
            this.allCount = allCount;
            this.pendingCount = pendingCount;
            this.shippedCount = shippedCount;
            this.completedCount = completedCount;
        }

        public static OrderListState empty() {
            return new OrderListState(
                    false,
                    Collections.emptyList(),
                    "",
                    "",
                    StatusFilter.ALL,
                    "",
                    OrderListUiMapper.SortOption.RECENT_ACTIVITY,
                    0,
                    0,
                    0,
                    0
            );
        }

        public boolean isLoading() {
            return loading;
        }

        public List<OrderRecord> getOrders() {
            return orders;
        }

        public String getSummary() {
            return summary;
        }

        public String getEmptyText() {
            return emptyText;
        }

        public StatusFilter getActiveFilter() {
            return activeFilter;
        }

        public String getSearchQuery() {
            return searchQuery;
        }

        public OrderListUiMapper.SortOption getActiveSortOption() {
            return activeSortOption;
        }

        public int getAllCount() {
            return allCount;
        }

        public int getPendingCount() {
            return pendingCount;
        }

        public int getShippedCount() {
            return shippedCount;
        }

        public int getCompletedCount() {
            return completedCount;
        }
    }
}
