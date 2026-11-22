package com.example.homework.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.homework.data.MessageRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.model.ConversationSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageViewModel extends AndroidViewModel {
    private final MessageRepository messageRepository;
    private final SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<MessageState> uiState = new MutableLiveData<>();
    private int requestVersion;

    public MessageViewModel(@NonNull Application application) {
        super(application);
        messageRepository = new MessageRepository(application);
        sessionManager = new SessionManager(application);
        uiState.setValue(new MessageState(false, Collections.emptyList()));
    }

    public LiveData<MessageState> getUiState() {
        return uiState;
    }

    public void refresh() {
        int requestId = ++requestVersion;
        MessageState current = uiState.getValue();
        List<ConversationSummary> currentItems = current != null ? current.getConversations() : Collections.emptyList();
        uiState.postValue(new MessageState(true, currentItems));
        executor.execute(() -> {
            List<ConversationSummary> conversations = messageRepository.getConversationSummaries(
                    sessionManager.getLoggedInUserId()
            );
            if (requestId == requestVersion) {
                uiState.postValue(new MessageState(false, new ArrayList<>(conversations)));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }

    public static class MessageState {
        private final boolean loading;
        private final List<ConversationSummary> conversations;

        public MessageState(boolean loading, List<ConversationSummary> conversations) {
            this.loading = loading;
            this.conversations = conversations;
        }

        public boolean isLoading() {
            return loading;
        }

        public List<ConversationSummary> getConversations() {
            return conversations;
        }
    }
}
