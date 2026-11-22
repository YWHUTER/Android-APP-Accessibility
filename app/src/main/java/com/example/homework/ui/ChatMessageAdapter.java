package com.example.homework.ui;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.R;
import com.example.homework.model.ChatMessage;

import java.util.Locale;
import java.util.Objects;

public class ChatMessageAdapter extends ListAdapter<ChatMessage, ChatMessageAdapter.ChatMessageViewHolder> {
    private static final int VIEW_TYPE_INCOMING = 1;
    private static final int VIEW_TYPE_OUTGOING = 2;

    private final long currentUserId;

    public ChatMessageAdapter(long currentUserId) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
        setHasStableIds(true);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getSenderId() == currentUserId
                ? VIEW_TYPE_OUTGOING
                : VIEW_TYPE_INCOMING;
    }

    @NonNull
    @Override
    public ChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == VIEW_TYPE_OUTGOING
                ? R.layout.item_chat_message_out
                : R.layout.item_chat_message_in;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ChatMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView bubbleView;
        private final TextView metaView;

        ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            bubbleView = itemView.findViewById(R.id.tv_chat_bubble);
            metaView = itemView.findViewById(R.id.tv_chat_meta);
        }

        void bind(ChatMessage message) {
            bubbleView.setText(message.getContent());
            metaView.setText(String.format(
                    Locale.getDefault(),
                    "%s  %s",
                    safe(message.getSenderName()),
                    DateFormat.format("MM-dd HH:mm", message.getCreatedAt())
            ));
        }

        private String safe(String text) {
            return text == null ? "对方" : text;
        }
    }

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChatMessage>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                    return oldItem.getId() == newItem.getId()
                            && oldItem.getConversationId() == newItem.getConversationId()
                            && oldItem.getSenderId() == newItem.getSenderId()
                            && oldItem.getCreatedAt() == newItem.getCreatedAt()
                            && Objects.equals(oldItem.getSenderName(), newItem.getSenderName())
                            && Objects.equals(oldItem.getContent(), newItem.getContent());
                }
            };
}
