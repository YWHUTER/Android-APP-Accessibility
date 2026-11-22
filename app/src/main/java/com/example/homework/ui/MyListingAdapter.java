package com.example.homework.ui;

import com.example.homework.util.A11y;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.R;
import com.example.homework.model.Item;
import com.example.homework.util.RemoteImageLoader;

import java.util.Objects;

public class MyListingAdapter extends ListAdapter<Item, MyListingAdapter.MyListingViewHolder> {
    private final ListingActionListener listingActionListener;

    public MyListingAdapter(ListingActionListener listingActionListener) {
        super(DIFF_CALLBACK);
        this.listingActionListener = listingActionListener;
        setHasStableIds(true);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @NonNull
    @Override
    public MyListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_listing, parent, false);
        return new MyListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyListingViewHolder holder, int position) {
        holder.bind(getItem(position), listingActionListener);
    }

    static class MyListingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView imageFallbackView;
        private final TextView titleView;
        private final TextView metaView;
        private final Button editButton;
        private final Button deleteButton;

        MyListingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_my_item_image);
            imageFallbackView = itemView.findViewById(R.id.tv_my_item_image_fallback);
            titleView = itemView.findViewById(R.id.tv_my_item_title);
            metaView = itemView.findViewById(R.id.tv_my_item_meta);
            editButton = itemView.findViewById(R.id.btn_edit_item);
            deleteButton = itemView.findViewById(R.id.btn_delete_item);
        }

        void bind(Item item, ListingActionListener listener) {
            titleView.setText(item.getTitle());
            metaView.setText(ProfileUiMapper.formatListingMeta(item));

            if (TextUtils.isEmpty(item.getImageUrl())) {
                imageFallbackView.setVisibility(View.VISIBLE);
                imageFallbackView.setText(TextUtils.isEmpty(item.getCategory()) ? "在售" : item.getCategory());
            } else {
                imageFallbackView.setVisibility(View.GONE);
            }
            RemoteImageLoader.load(imageView, item.getImageUrl());
            A11y.itemImage(imageView, imageView.getContext()
                    .getString(R.string.cd_item_image_with_name, item.getTitle()));

            itemView.setOnClickListener(v -> listener.onOpen(item));
            editButton.setOnClickListener(v -> listener.onEdit(item));
            deleteButton.setOnClickListener(v -> listener.onDelete(item));
        }
    }

    public interface ListingActionListener {
        void onOpen(Item item);

        void onEdit(Item item);

        void onDelete(Item item);
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
                    && oldItem.getCreatedAt() == newItem.getCreatedAt()
                    && oldItem.getCampusPickupOnly() == newItem.getCampusPickupOnly()
                    && Objects.equals(oldItem.getTitle(), newItem.getTitle())
                    && Objects.equals(oldItem.getCategory(), newItem.getCategory())
                    && Objects.equals(oldItem.getCondition(), newItem.getCondition())
                    && Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl());
        }
    };
}
