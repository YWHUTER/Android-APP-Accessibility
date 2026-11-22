package com.example.homework.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.homework.data.ItemRepository;
import com.example.homework.data.OrderRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.data.UserRepository;
import com.example.homework.model.Item;
import com.example.homework.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileViewModel extends AndroidViewModel {
    private final SessionManager sessionManager;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<ProfileState> uiState = new MutableLiveData<>();
    private final MutableLiveData<ProfileEvent> events = new MutableLiveData<>();

    private int requestVersion;
    private long nextEventId;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        sessionManager = new SessionManager(application);
        userRepository = new UserRepository(application);
        itemRepository = new ItemRepository(application);
        orderRepository = new OrderRepository(application);
        uiState.setValue(ProfileState.empty());
    }

    public LiveData<ProfileState> getUiState() {
        return uiState;
    }

    public LiveData<ProfileEvent> getEvents() {
        return events;
    }

    public void refresh() {
        loadProfile(true);
    }

    public void deleteListing(long itemId) {
        int requestId = ++requestVersion;
        publishLoading();
        executor.execute(() -> {
            itemRepository.delete(itemId);
            events.postValue(new ProfileEvent(++nextEventId, "已下架该闲置"));
            ProfileState state = loadState();
            if (requestId == requestVersion) {
                uiState.postValue(state);
            }
        });
    }

    public void updateAvatar(String avatarImageUrl) {
        int requestId = ++requestVersion;
        publishLoading();
        executor.execute(() -> {
            boolean updated = userRepository.updateAvatarImage(
                    sessionManager.getLoggedInUserId(),
                    avatarImageUrl
            );
            events.postValue(new ProfileEvent(
                    ++nextEventId,
                    updated ? "头像已更新" : "头像保存失败，请稍后再试"
            ));
            ProfileState state = loadState();
            if (requestId == requestVersion) {
                uiState.postValue(state);
            }
        });
    }

    private void loadProfile(boolean showLoading) {
        int requestId = ++requestVersion;
        if (showLoading) {
            publishLoading();
        }
        executor.execute(() -> {
            ProfileState state = loadState();
            if (requestId == requestVersion) {
                uiState.postValue(state);
            }
        });
    }

    private ProfileState loadState() {
        long userId = sessionManager.getLoggedInUserId();
        User user = userRepository.getById(userId);
        List<Item> listings = itemRepository.getItemsBySeller(userId);
        int purchaseCount = orderRepository.countOrdersByBuyer(userId);
        int saleCount = orderRepository.countOrdersBySeller(userId);

        return new ProfileState(
                false,
                ProfileUiMapper.buildAvatarText(user),
                user != null ? user.getAvatarImageUrl() : "",
                ProfileUiMapper.safe(user != null ? user.getName() : null),
                ProfileUiMapper.buildMajorLabel(user),
                user != null ? user.getBio() : "",
                String.valueOf(purchaseCount),
                String.valueOf(saleCount),
                ProfileUiMapper.buildInterestSummary(user),
                new ArrayList<>(listings),
                ProfileUiMapper.buildListingsEmptyText()
        );
    }

    private void publishLoading() {
        ProfileState current = uiState.getValue();
        if (current == null) {
            uiState.postValue(ProfileState.loading());
            return;
        }
        uiState.postValue(new ProfileState(
                true,
                current.getAvatarText(),
                current.getAvatarImageUrl(),
                current.getName(),
                current.getMajor(),
                current.getBio(),
                current.getPurchaseCount(),
                current.getSaleCount(),
                current.getInterestSummary(),
                current.getListings(),
                current.getListingsEmptyText()
        ));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }

    public static class ProfileState {
        private final boolean loading;
        private final String avatarText;
        private final String avatarImageUrl;
        private final String name;
        private final String major;
        private final String bio;
        private final String purchaseCount;
        private final String saleCount;
        private final String interestSummary;
        private final List<Item> listings;
        private final String listingsEmptyText;

        public ProfileState(boolean loading, String avatarText, String avatarImageUrl,
                            String name, String major, String bio,
                            String purchaseCount, String saleCount, String interestSummary,
                            List<Item> listings, String listingsEmptyText) {
            this.loading = loading;
            this.avatarText = avatarText;
            this.avatarImageUrl = avatarImageUrl;
            this.name = name;
            this.major = major;
            this.bio = bio;
            this.purchaseCount = purchaseCount;
            this.saleCount = saleCount;
            this.interestSummary = interestSummary;
            this.listings = listings;
            this.listingsEmptyText = listingsEmptyText;
        }

        public static ProfileState empty() {
            return new ProfileState(
                    false,
                    "我",
                    "",
                    "未填写",
                    "未填写",
                    "",
                    "0",
                    "0",
                    ProfileUiMapper.buildInterestSummary(null),
                    Collections.emptyList(),
                    ProfileUiMapper.buildListingsEmptyText()
            );
        }

        public static ProfileState loading() {
            ProfileState state = empty();
            return new ProfileState(
                    true,
                    state.getAvatarText(),
                    state.getAvatarImageUrl(),
                    state.getName(),
                    state.getMajor(),
                    state.getBio(),
                    state.getPurchaseCount(),
                    state.getSaleCount(),
                    state.getInterestSummary(),
                    state.getListings(),
                    state.getListingsEmptyText()
            );
        }

        public boolean isLoading() {
            return loading;
        }

        public String getAvatarText() {
            return avatarText;
        }

        public String getAvatarImageUrl() {
            return avatarImageUrl;
        }

        public String getName() {
            return name;
        }

        public String getMajor() {
            return major;
        }

        public String getBio() {
            return bio;
        }

        public String getPurchaseCount() {
            return purchaseCount;
        }

        public String getSaleCount() {
            return saleCount;
        }

        public String getInterestSummary() {
            return interestSummary;
        }

        public List<Item> getListings() {
            return listings;
        }

        public String getListingsEmptyText() {
            return listingsEmptyText;
        }
    }

    public static class ProfileEvent {
        private final long eventId;
        private final String message;

        public ProfileEvent(long eventId, String message) {
            this.eventId = eventId;
            this.message = message;
        }

        public long getEventId() {
            return eventId;
        }

        public String getMessage() {
            return message;
        }
    }
}
