package com.example.homework;

import com.example.homework.util.A11y;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.data.MessageRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.model.ChatMessage;
import com.example.homework.model.ConversationSummary;
import com.example.homework.ui.ChatMessageAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_ITEM_ID = "item_id";
    public static final String EXTRA_SELLER_ID = "seller_id";

    private static final String STATE_DRAFT_MESSAGE = "state_draft_message";
    private static final String INPUT_HINT = "输入消息...";

    private MessageRepository messageRepository;
    private SessionManager sessionManager;
    private ConversationSummary conversation;
    private RecyclerView recyclerMessages;
    private TextView tvEmptyState;
    private EditText etChatInput;
    private TextView btnSendMessage;
    private ProgressBar progressBar;
    private ChatMessageAdapter chatMessageAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int requestVersion;
    private boolean loading;
    private String draftMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (savedInstanceState != null) {
            draftMessage = savedInstanceState.getString(STATE_DRAFT_MESSAGE, "");
        }

        messageRepository = new MessageRepository(this);
        sessionManager = new SessionManager(this);
        recyclerMessages = findViewById(R.id.recycler_chat_messages);
        tvEmptyState = findViewById(R.id.tv_chat_empty_state);
        etChatInput = findViewById(R.id.et_chat_input);
        btnSendMessage = findViewById(R.id.btn_send_message);
        progressBar = findViewById(R.id.progress_chat);

        setupRecyclerView();
        setupInput();

        if (!draftMessage.isEmpty()) {
            etChatInput.setText(draftMessage);
            etChatInput.setSelection(etChatInput.getText().length());
        }

        findViewById(R.id.btn_chat_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_chat_more).setOnClickListener(this::showMoreOptions);
        tvEmptyState.setOnClickListener(v -> fillQuickReply());
        btnSendMessage.setOnClickListener(v -> sendCurrentMessage());
        loadConversationData(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (conversation != null) {
            loadConversationData(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_DRAFT_MESSAGE, etChatInput.getText().toString());
    }

    private void bindHeader() {
        TextView tvChatName = findViewById(R.id.tv_chat_name);
        TextView tvChatSubtitle = findViewById(R.id.tv_chat_subtitle);
        TextView tvChatItemTitle = findViewById(R.id.tv_chat_item_title);
        TextView tvChatPrice = findViewById(R.id.tv_chat_price);

        tvChatName.setText(conversation.getOtherUserName());
        tvChatSubtitle.setText(conversation.getOtherUserMajor());
        tvChatItemTitle.setText(conversation.getItemTitle());
        tvChatPrice.setText(String.format(Locale.getDefault(), "¥%.0f", conversation.getItemPrice()));
    }

    private void bindMessages(List<ChatMessage> messages, boolean scrollToLatest) {
        boolean empty = messages.isEmpty();
        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        tvEmptyState.setClickable(empty);
        if (empty) {
            tvEmptyState.setText("还没有历史消息，点这里快速发一句“你好，这个还在吗？”。");
        }
        chatMessageAdapter.submitList(new ArrayList<>(messages), () -> {
            if (scrollToLatest) {
                scrollToBottom();
            }
        });
    }

    private void sendCurrentMessage() {
        if (loading) {
            return;
        }
        if (conversation == null) {
            A11y.toast(this, "聊天还在加载，稍等一下再发送。");
            return;
        }

        String content = etChatInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            A11y.toast(this, "请输入要发送的内容。");
            return;
        }

        long currentUserId = sessionManager.getLoggedInUserId();
        if (currentUserId <= 0L) {
            A11y.toast(this, "登录状态已失效，请重新登录。");
            return;
        }

        int requestId = ++requestVersion;
        showLoading(true);
        executor.execute(() -> {
            messageRepository.sendMessage(conversation.getId(), currentUserId, content);
            ConversationSummary updatedConversation = messageRepository.getConversation(
                    conversation.getId(),
                    currentUserId
            );
            List<ChatMessage> messages = messageRepository.getMessages(conversation.getId());
            if (requestId == requestVersion) {
                mainHandler.post(() -> {
                    conversation = updatedConversation;
                    etChatInput.setText("");
                    bindHeader();
                    bindMessages(messages, true);
                    showLoading(false);
                });
            }
        });
    }

    private void showMoreOptions(View anchor) {
        if (conversation == null) {
            return;
        }

        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, 1, 0, "查看商品详情");
        popupMenu.getMenu().add(0, 2, 1, "清空聊天记录");
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == 1) {
                long itemId = messageRepository.getConversationItemId(conversation.getId());
                if (itemId > 0) {
                    Intent intent = new Intent(this, ItemDetailActivity.class);
                    intent.putExtra(ItemDetailActivity.EXTRA_ITEM_ID, itemId);
                    startActivity(intent);
                } else {
                    A11y.toast(this, "找不到关联的商品信息。");
                }
                return true;
            } else if (menuItem.getItemId() == 2) {
                new AlertDialog.Builder(this)
                        .setTitle("清空聊天记录")
                        .setMessage("确定要清空这段聊天的所有消息吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            int requestId = ++requestVersion;
                            showLoading(true);
                            executor.execute(() -> {
                                messageRepository.deleteMessages(conversation.getId());
                                ConversationSummary updatedConversation = messageRepository.getConversation(
                                        conversation.getId(),
                                        sessionManager.getLoggedInUserId()
                                );
                                List<ChatMessage> messages = messageRepository.getMessages(conversation.getId());
                                if (requestId == requestVersion) {
                                    mainHandler.post(() -> {
                                        conversation = updatedConversation;
                                        bindHeader();
                                        bindMessages(messages, false);
                                        showLoading(false);
                                        A11y.toast(this, "聊天记录已清空。");
                                    });
                                }
                            });
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void setupRecyclerView() {
        chatMessageAdapter = new ChatMessageAdapter(sessionManager.getLoggedInUserId());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(false);
        recyclerMessages.setLayoutManager(layoutManager);
        recyclerMessages.setAdapter(chatMessageAdapter);
        recyclerMessages.setItemAnimator(null);
    }

    private void setupInput() {
        etChatInput.setOnFocusChangeListener((v, hasFocus) -> updateInputHint());
        etChatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage();
                return true;
            }
            return false;
        });
        etChatInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateInputHint();
                updateSendButtonState();
            }
        });
        updateInputHint();
        updateSendButtonState();
    }

    private void loadConversationData(boolean markAsRead) {
        int requestId = ++requestVersion;
        showLoading(true);

        long currentUserId = sessionManager.getLoggedInUserId();
        long requestedConversationId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1L);
        long requestedItemId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1L);
        long requestedSellerId = getIntent().getLongExtra(EXTRA_SELLER_ID, -1L);

        executor.execute(() -> {
            ConversationSummary resolvedConversation = resolveConversation(
                    currentUserId,
                    requestedConversationId,
                    requestedItemId,
                    requestedSellerId
            );
            if (resolvedConversation == null) {
                if (requestId == requestVersion) {
                    mainHandler.post(() -> {
                        showLoading(false);
                        A11y.toast(this, "暂时无法打开这段聊天。");
                        finish();
                    });
                }
                return;
            }

            if (markAsRead) {
                messageRepository.markConversationAsRead(resolvedConversation.getId(), currentUserId);
                resolvedConversation = messageRepository.getConversation(resolvedConversation.getId(), currentUserId);
            }

            List<ChatMessage> messages = messageRepository.getMessages(resolvedConversation.getId());
            ConversationSummary finalConversation = resolvedConversation;
            if (requestId == requestVersion) {
                mainHandler.post(() -> {
                    conversation = finalConversation;
                    bindHeader();
                    bindMessages(messages, false);
                    showLoading(false);
                });
            }
        });
    }

    private ConversationSummary resolveConversation(long currentUserId, long conversationId,
                                                    long itemId, long sellerId) {
        if (conversationId > 0) {
            return messageRepository.getConversation(conversationId, currentUserId);
        }

        if (itemId > 0 && sellerId > 0 && sellerId != currentUserId) {
            long createdConversationId = messageRepository.getOrCreateConversation(itemId, currentUserId, sellerId);
            return messageRepository.getConversation(createdConversationId, currentUserId);
        }
        return null;
    }

    private void showLoading(boolean loading) {
        this.loading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        updateSendButtonState();
    }

    private void scrollToBottom() {
        if (chatMessageAdapter.getItemCount() == 0) {
            return;
        }
        recyclerMessages.scrollToPosition(chatMessageAdapter.getItemCount() - 1);
    }

    private void updateSendButtonState() {
        if (btnSendMessage == null || etChatInput == null) {
            return;
        }
        boolean enabled = !loading
                && conversation != null
                && !TextUtils.isEmpty(etChatInput.getText().toString().trim());
        btnSendMessage.setEnabled(enabled);
        btnSendMessage.setAlpha(enabled ? 1f : 0.58f);
    }

    private void updateInputHint() {
        if (etChatInput == null) {
            return;
        }
        boolean hasInput = !TextUtils.isEmpty(etChatInput.getText().toString());
        etChatInput.setHint(etChatInput.hasFocus() || hasInput ? "" : INPUT_HINT);
    }

    private void fillQuickReply() {
        if (conversation == null || !TextUtils.isEmpty(etChatInput.getText().toString().trim())) {
            return;
        }

        String itemTitle = conversation.getItemTitle();
        String quickReply = TextUtils.isEmpty(itemTitle)
                ? "你好，这个还在吗？方便什么时候看一下？"
                : "你好，这个“" + itemTitle + "”还在吗？方便什么时候看一下？";
        etChatInput.setText(quickReply);
        etChatInput.setSelection(etChatInput.getText().length());
        etChatInput.requestFocus();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
