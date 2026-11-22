package com.example.homework.ui;

import com.example.homework.util.A11y;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.R;
import com.example.homework.model.Item;
import com.example.homework.model.User;
import com.example.homework.util.RemoteImageLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DiscoveryItemAdapter extends ListAdapter<Item, DiscoveryItemAdapter.DiscoveryItemViewHolder> {
    private final OnDiscoveryItemClickListener onItemClickListener;
    private DiscoveryFeedMode feedMode = DiscoveryFeedMode.FOLLOW;
    private User currentUser;
    private Map<Long, String> aiCardReasons = Collections.emptyMap();

    public DiscoveryItemAdapter(OnDiscoveryItemClickListener onItemClickListener) {
        super(DIFF_CALLBACK);
        this.onItemClickListener = onItemClickListener;
        setHasStableIds(true);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    public void updateContext(DiscoveryFeedMode feedMode, User currentUser,
                              Map<Long, String> aiCardReasons) {
        Map<Long, String> normalizedReasons = aiCardReasons == null
                ? Collections.emptyMap()
                : new HashMap<>(aiCardReasons);
        if (this.feedMode == feedMode
                && Objects.equals(this.currentUser, currentUser)
                && Objects.equals(this.aiCardReasons, normalizedReasons)) {
            return;
        }
        this.feedMode = feedMode;
        this.currentUser = currentUser;
        this.aiCardReasons = normalizedReasons;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiscoveryItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_marketplace_card, parent, false);
        return new DiscoveryItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscoveryItemViewHolder holder, int position) {
        holder.bind(getItem(position), position, feedMode, currentUser, aiCardReasons, onItemClickListener);
    }

    static class DiscoveryItemViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout imageContainer;
        private final ImageView imageView;
        private final TextView titleView;
        private final TextView priceView;
        private final TextView timeView;
        private final TextView avatarView;
        private final TextView sellerView;
        private final TextView locationView;
        private final TextView subtitleView;

        DiscoveryItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageContainer = itemView.findViewById(R.id.layout_image_container);
            imageView = itemView.findViewById(R.id.iv_item_image);
            titleView = itemView.findViewById(R.id.tv_item_title);
            priceView = itemView.findViewById(R.id.tv_item_price);
            timeView = itemView.findViewById(R.id.tv_item_time);
            avatarView = itemView.findViewById(R.id.tv_item_avatar);
            sellerView = itemView.findViewById(R.id.tv_item_seller);
            locationView = itemView.findViewById(R.id.tv_item_location);
            subtitleView = itemView.findViewById(R.id.tv_item_subtitle);
        }

        void bind(Item item, int position, DiscoveryFeedMode feedMode, User currentUser,
                  Map<Long, String> aiCardReasons,
                  OnDiscoveryItemClickListener onItemClickListener) {
            ViewGroup.LayoutParams imageParams = imageContainer.getLayoutParams();
            imageParams.height = dpToPx(DiscoveryUiMapper.resolveImageHeight(item, position));
            imageContainer.setLayoutParams(imageParams);

            if (TextUtils.isEmpty(item.getImageUrl())) {
                imageContainer.setVisibility(View.GONE);
            } else {
                imageContainer.setVisibility(View.VISIBLE);
                RemoteImageLoader.load(imageView, item.getImageUrl());
            }

            titleView.setText(item.getTitle());
            // 商品图片朗读具体商品名；文字头像为装饰，不再朗读单字
            A11y.itemImage(imageView, imageView.getContext()
                    .getString(R.string.cd_item_image_with_name, item.getTitle()));
            A11y.decorative(avatarView);
            priceView.setText(DiscoveryUiMapper.formatPrice(item.getPrice()));
            timeView.setText(DiscoveryUiMapper.formatRelativeTime(item.getCreatedAt()));
            avatarView.setText(DiscoveryUiMapper.buildAvatar(item.getSellerName()));
            sellerView.setText(DiscoveryUiMapper.safeText(item.getSellerName(), "校园卖家"));
            locationView.setText(DiscoveryUiMapper.shortLocation(item.getLocation()));

            String cardReason = "";
            if (aiCardReasons != null) {
                cardReason = aiCardReasons.get(item.getId());
            }
            if (TextUtils.isEmpty(cardReason)) {
                cardReason = DiscoveryUiMapper.buildCardReason(item, position, feedMode, currentUser);
            }
            if (cardReason.isEmpty()) {
                subtitleView.setVisibility(View.GONE);
            } else {
                subtitleView.setVisibility(View.VISIBLE);
                subtitleView.setText(cardReason);
            }

            itemView.setOnClickListener(v -> onItemClickListener.onItemClick(item.getId()));
        }

        private int dpToPx(int dp) {
            return Math.round(dp * itemView.getResources().getDisplayMetrics().density);
        }
    }

    public interface OnDiscoveryItemClickListener {
        void onItemClick(long itemId);
    }

    private static final DiffUtil.ItemCallback<Item> DIFF_CALLBACK = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.getId() == newItem.getId()
                    && oldItem.getSellerId() == newItem.getSellerId()
                    && oldItem.getPrice() == newItem.getPrice()
                    && oldItem.getCampusPickupOnly() == newItem.getCampusPickupOnly()
                    && oldItem.getCreatedAt() == newItem.getCreatedAt()
                    && Objects.equals(oldItem.getSellerName(), newItem.getSellerName())
                    && Objects.equals(oldItem.getSellerMajor(), newItem.getSellerMajor())
                    && Objects.equals(oldItem.getTitle(), newItem.getTitle())
                    && Objects.equals(oldItem.getDescription(), newItem.getDescription())
                    && Objects.equals(oldItem.getCategory(), newItem.getCategory())
                    && Objects.equals(oldItem.getLocation(), newItem.getLocation())
                    && Objects.equals(oldItem.getCondition(), newItem.getCondition())
                    && Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl())
                    && Objects.equals(oldItem.getTags(), newItem.getTags())
                    && Objects.equals(oldItem.getPreferredMajor(), newItem.getPreferredMajor());
        }
    };
}
