package com.example.homework.ui;

import android.app.Application;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.homework.data.CartRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.model.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CartViewModel extends AndroidViewModel {
    private final CartRepository cartRepository;
    private final SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<CartState> uiState = new MutableLiveData<>();
    private final MutableLiveData<CartEvent> events = new MutableLiveData<>();
    private final Object stateLock = new Object();

    private final Set<Long> selectedItemIds = new HashSet<>();
    private Set<Long> knownItemIds = new HashSet<>();
    private List<Item> currentItems = new ArrayList<>();
    private boolean selectionInitialized;
    private int requestVersion;
    private long nextEventId;

    public CartViewModel(@NonNull Application application) {
        super(application);
        cartRepository = new CartRepository(application);
        sessionManager = new SessionManager(application);
        uiState.setValue(new CartState(
                false,
                CartUiMapper.buildSubtitle(0),
                Collections.emptyList(),
                CartUiMapper.buildSummary(Collections.emptyList(), Collections.emptySet())
        ));
    }

    public LiveData<CartState> getUiState() {
        return uiState;
    }

    public LiveData<CartEvent> getEvents() {
        return events;
    }

    public void refresh() {
        int requestId = ++requestVersion;
        publishLoading();
        executor.execute(() -> {
            List<Item> items = cartRepository.getCartItems(currentUserId());
            CartState state;
            synchronized (stateLock) {
                currentItems = new ArrayList<>(items);
                syncSelectionStateLocked(currentItems);
                state = buildStateLocked(false);
            }
            if (requestId == requestVersion) {
                publishState(state);
            }
        });
    }

    public void setItemSelected(long itemId, boolean selected) {
        synchronized (stateLock) {
            if (selected) {
                selectedItemIds.add(itemId);
            } else {
                selectedItemIds.remove(itemId);
            }
            publishState(buildStateLocked(false));
        }
    }

    public void toggleSelectAll(boolean checked) {
        synchronized (stateLock) {
            selectedItemIds.clear();
            if (checked) {
                for (Item item : currentItems) {
                    selectedItemIds.add(item.getId());
                }
            }
            publishState(buildStateLocked(false));
        }
    }

    public void removeItem(long itemId) {
        publishLoading();
        executor.execute(() -> {
            cartRepository.removeItem(currentUserId(), itemId);
            synchronized (stateLock) {
                selectedItemIds.remove(itemId);
                knownItemIds.remove(itemId);
            }
            emitToast("已从购物车移除");
            refresh();
        });
    }

    public void checkoutSelected() {
        List<Long> selectedIds = new ArrayList<>();
        synchronized (stateLock) {
            for (Item item : currentItems) {
                if (selectedItemIds.contains(item.getId())) {
                    selectedIds.add(item.getId());
                }
            }
        }

        if (selectedIds.isEmpty()) {
            return;
        }

        publishLoading();
        executor.execute(() -> {
            CartRepository.CheckoutResult result = cartRepository.checkout(currentUserId(), selectedIds);
            synchronized (stateLock) {
                selectedItemIds.removeAll(selectedIds);
            }
            emitCheckoutResult(result);
            refresh();
        });
    }

    private void syncSelectionStateLocked(List<Item> items) {
        Set<Long> currentIds = new HashSet<>();
        for (Item item : items) {
            currentIds.add(item.getId());
        }

        if (!selectionInitialized) {
            if (!currentIds.isEmpty()) {
                selectedItemIds.clear();
                selectedItemIds.addAll(currentIds);
                selectionInitialized = true;
            }
            knownItemIds = currentIds;
            return;
        }

        selectedItemIds.retainAll(currentIds);
        for (Long itemId : currentIds) {
            if (!knownItemIds.contains(itemId)) {
                selectedItemIds.add(itemId);
            }
        }
        knownItemIds = currentIds;
    }

    private CartState buildStateLocked(boolean loading) {
        List<CartUiMapper.RowModel> rows = CartUiMapper.buildRows(currentItems, selectedItemIds);
        CartUiMapper.CartSummary summary = CartUiMapper.buildSummary(currentItems, selectedItemIds);
        return new CartState(
                loading,
                summary.getSubtitle(),
                rows,
                summary
        );
    }

    private void publishLoading() {
        synchronized (stateLock) {
            publishState(buildStateLocked(true));
        }
    }

    private void publishState(CartState state) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            uiState.setValue(state);
        } else {
            uiState.postValue(state);
        }
    }

    private void emitToast(String message) {
        events.postValue(new CartEvent(++nextEventId, CartEvent.TYPE_TOAST, message, null));
    }

    private void emitCheckoutResult(CartRepository.CheckoutResult result) {
        events.postValue(new CartEvent(++nextEventId, CartEvent.TYPE_CHECKOUT_RESULT, null, result));
    }

    private long currentUserId() {
        return sessionManager.getLoggedInUserId();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }

    public static class CartState {
        private final boolean loading;
        private final String subtitle;
        private final List<CartUiMapper.RowModel> rows;
        private final CartUiMapper.CartSummary summary;

        public CartState(boolean loading, String subtitle, List<CartUiMapper.RowModel> rows,
                         CartUiMapper.CartSummary summary) {
            this.loading = loading;
            this.subtitle = subtitle;
            this.rows = rows;
            this.summary = summary;
        }

        public boolean isLoading() {
            return loading;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public List<CartUiMapper.RowModel> getRows() {
            return rows;
        }

        public CartUiMapper.CartSummary getSummary() {
            return summary;
        }

        public boolean isEmpty() {
            return rows.isEmpty();
        }
    }

    public static class CartEvent {
        public static final int TYPE_TOAST = 1;
        public static final int TYPE_CHECKOUT_RESULT = 2;

        private final long eventId;
        private final int type;
        private final String message;
        private final CartRepository.CheckoutResult checkoutResult;

        public CartEvent(long eventId, int type, String message, CartRepository.CheckoutResult checkoutResult) {
            this.eventId = eventId;
            this.type = type;
            this.message = message;
            this.checkoutResult = checkoutResult;
        }

        public long getEventId() {
            return eventId;
        }

        public int getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public CartRepository.CheckoutResult getCheckoutResult() {
            return checkoutResult;
        }
    }
}
