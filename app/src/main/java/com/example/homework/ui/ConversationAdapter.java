package com.example.homework.ui;

import com.example.homework.util.A11y;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.R;
import com.example.homework.model.ConversationSummary;
import com.example.homework.util.RemoteImageLoader;

import java.util.Locale;
import java.util.Objects;

public class ConversationAdapter extends ListAdapter<ConversationSummary, ConversationAdapter.ConversationViewHolder> {
    private final OnConversationClickListener onConversationClickListener;

    public ConversationAdapter(OnConversationClickListener onConversationClickListener) {
        super(DIFF_CALLBACK);
        this.onConversationClickListener = onConversationClickListener;
        setHasStableIds(true);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(getItem(position), onConversationClickListener);
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final TextView avatarTextView;
        private final TextView nameView;
        private final TextView timeView;
        private final TextView titleView;
        private final TextView previewView;
        private final TextView unreadBadgeView;
        private final ImageView itemImageView;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarTextView = itemView.findViewById(R.id.tv_conversation_avatar_text);
            nameView = itemView.findViewById(R.id.tv_conversation_name);
            timeView = itemView.findViewById(R.id.tv_conversation_time);
            titleView = itemView.findViewById(R.id.tv_conversation_item_title);
            previewView = itemView.findViewById(R.id.tv_conversation_preview);
            unreadBadgeView = itemView.findViewById(R.id.tv_conversation_unread_badge);
            itemImageView = itemView.findViewById(R.id.iv_conversation_item_image);
        }

        void bind(ConversationSummary conversation, OnConversationClickListener onConversationClickListener) {
            avatarTextView.setText(MessageUiMapper.buildAvatarText(conversation));
            nameView.setText(conversation.getOtherUserName());
            titleView.setText(String.format(
                    Locale.getDefault(),
                    "%s · ¥%.0f",
                    conversation.getItemTitle(),
                    conversation.getItemPrice()
            ));
            previewView.setText(conversation.getLastMessage());

            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    conversation.getLastMessageAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            timeView.setText(relativeTime);
            // 会话头像由姓名文本替代；商品缩略图朗读商品名；未读角标朗读「N 条未读消息」
            A11y.decorative(avatarTextView);
            A11y.itemImage(itemImageView, itemImageView.getContext()
                    .getString(R.string.cd_item_image_with_name, conversation.getItemTitle()));
            A11y.label(unreadBadgeView, unreadBadgeView.getContext()
                    .getString(R.string.cd_unread_badge, conversation.getUnreadCount()));

            if (!TextUtils.isEmpty(conversation.getItemImageUrl())) {
                RemoteImageLoader.load(itemImageView, conversation.getItemImageUrl());
                itemImageView.setColorFilter(null);
            } else {
                itemImageView.setImageResource(android.R.drawable.ic_menu_gallery);
                itemImageView.setColorFilter(ContextCompat.getColor(
                        itemView.getContext(),
                        R.color.primary_blue
                ));
            }

            if (conversation.getUnreadCount() > 0) {
                unreadBadgeView.setVisibility(View.VISIBLE);
                unreadBadgeView.setText(MessageUiMapper.formatUnreadCount(conversation.getUnreadCount()));
                previewView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
                previewView.setTypeface(previewView.getTypeface(), Typeface.BOLD);
            } else {
                unreadBadgeView.setVisibility(View.GONE);
                previewView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                previewView.setTypeface(previewView.getTypeface(), Typeface.NORMAL);
            }

            itemView.setOnClickListener(v -> onConversationClickListener.onConversationClick(conversation));
        }
    }

    public interface OnConversationClickListener {
        void onConversationClick(ConversationSummary conversation);
    }

    private static final DiffUtil.ItemCallback<ConversationSummary> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ConversationSummary>() {
                @Override
                public boolean areItemsTheSame(@NonNull ConversationSummary oldItem,
                                               @NonNull ConversationSummary newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ConversationSummary oldItem,
                                                  @NonNull ConversationSummary newItem) {
                    return oldItem.getId() == newItem.getId()
                            && oldItem.getItemId() == newItem.getItemId()
                            && oldItem.getItemPrice() == newItem.getItemPrice()
                            && oldItem.getOtherUserId() == newItem.getOtherUserId()
                            && oldItem.getLastMessageAt() == newItem.getLastMessageAt()
                            && oldItem.isBuyer() == newItem.isBuyer()
                            && oldItem.getUnreadCount() == newItem.getUnreadCount()
                            && Objects.equals(oldItem.getItemTitle(), newItem.getItemTitle())
                            && Objects.equals(oldItem.getItemImageUrl(), newItem.getItemImageUrl())
                            && Objects.equals(oldItem.getOtherUserName(), newItem.getOtherUserName())
                            && Objects.equals(oldItem.getOtherUserMajor(), newItem.getOtherUserMajor())
                            && Objects.equals(oldItem.getLastMessage(), newItem.getLastMessage());
                }
            };
}
