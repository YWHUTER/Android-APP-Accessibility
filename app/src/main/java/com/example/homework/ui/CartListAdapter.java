package com.example.homework.ui;

import com.example.homework.util.A11y;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.R;
import com.example.homework.model.Item;
import com.example.homework.util.RemoteImageLoader;

import java.util.Objects;

public class CartListAdapter extends ListAdapter<CartUiMapper.RowModel, RecyclerView.ViewHolder> {
    private final CartRowListener cartRowListener;

    public CartListAdapter(CartRowListener cartRowListener) {
        super(DIFF_CALLBACK);
        this.cartRowListener = cartRowListener;
        setHasStableIds(true);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getStableId();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == CartUiMapper.VIEW_TYPE_SELLER_HEADER) {
            View view = inflater.inflate(R.layout.item_cart_section, parent, false);
            return new SellerHeaderViewHolder(view);
        }

        View view = inflater.inflate(R.layout.item_cart_entry, parent, false);
        return new CartItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CartUiMapper.RowModel rowModel = getItem(position);
        if (holder instanceof SellerHeaderViewHolder) {
            ((SellerHeaderViewHolder) holder).bind((CartUiMapper.SellerHeaderRow) rowModel);
            return;
        }
        ((CartItemViewHolder) holder).bind((CartUiMapper.ItemRow) rowModel, cartRowListener);
    }

    static class SellerHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView metaView;

        SellerHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.tv_cart_section_title);
            metaView = itemView.findViewById(R.id.tv_cart_section_meta);
        }

        void bind(CartUiMapper.SellerHeaderRow row) {
            titleView.setText(row.getTitle());
            metaView.setText(row.getMeta());
        }
    }

    static class CartItemViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatCheckBox checkBox;
        private final ImageView imageView;
        private final TextView titleView;
        private final TextView tagView;
        private final TextView metaView;
        private final TextView priceView;
        private final TextView quantityView;
        private final AppCompatButton contactButton;
        private final AppCompatButton removeButton;

        CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cb_cart_item_select);
            imageView = itemView.findViewById(R.id.iv_cart_item_image);
            titleView = itemView.findViewById(R.id.tv_cart_item_title);
            tagView = itemView.findViewById(R.id.tv_cart_item_tag);
            metaView = itemView.findViewById(R.id.tv_cart_item_meta);
            priceView = itemView.findViewById(R.id.tv_cart_item_price);
            quantityView = itemView.findViewById(R.id.tv_cart_item_quantity);
            contactButton = itemView.findViewById(R.id.btn_cart_item_contact);
            removeButton = itemView.findViewById(R.id.btn_cart_item_remove);
        }

        void bind(CartUiMapper.ItemRow row, CartRowListener listener) {
            Item item = row.getItem();

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(row.isSelected());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    listener.onItemSelectionChanged(item.getId(), isChecked));

            RemoteImageLoader.load(imageView, item.getImageUrl());
            A11y.itemImage(imageView, imageView.getContext()
                    .getString(R.string.cd_item_image_with_name, item.getTitle()));
            // 复选框原本只会朗读「未勾选」，补充所属商品名
            A11y.label(checkBox, checkBox.getContext()
                    .getString(R.string.cd_cart_item_checkbox, item.getTitle()));
            titleView.setText(item.getTitle());
            tagView.setText(CartUiMapper.safeText(item.getCondition(), "待确认"));
            metaView.setText(CartUiMapper.formatItemMeta(item));
            priceView.setText(CartUiMapper.formatPrice(item.getPrice()));
            quantityView.setText("x1");

            itemView.setOnClickListener(v -> listener.onItemClick(item));
            contactButton.setOnClickListener(v -> listener.onContactSeller(item));
            removeButton.setOnClickListener(v -> listener.onRemoveItem(item));
        }
    }

    public interface CartRowListener {
        void onItemSelectionChanged(long itemId, boolean selected);

        void onItemClick(Item item);

        void onContactSeller(Item item);

        void onRemoveItem(Item item);
    }

    private static final DiffUtil.ItemCallback<CartUiMapper.RowModel> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CartUiMapper.RowModel>() {
                @Override
                public boolean areItemsTheSame(@NonNull CartUiMapper.RowModel oldItem,
                                               @NonNull CartUiMapper.RowModel newItem) {
                    return oldItem.getStableId() == newItem.getStableId()
                            && oldItem.getViewType() == newItem.getViewType();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CartUiMapper.RowModel oldItem,
                                                  @NonNull CartUiMapper.RowModel newItem) {
                    if (oldItem instanceof CartUiMapper.SellerHeaderRow
                            && newItem instanceof CartUiMapper.SellerHeaderRow) {
                        CartUiMapper.SellerHeaderRow oldRow = (CartUiMapper.SellerHeaderRow) oldItem;
                        CartUiMapper.SellerHeaderRow newRow = (CartUiMapper.SellerHeaderRow) newItem;
                        return oldRow.getSellerId() == newRow.getSellerId()
                                && Objects.equals(oldRow.getTitle(), newRow.getTitle())
                                && Objects.equals(oldRow.getMeta(), newRow.getMeta());
                    }

                    if (oldItem instanceof CartUiMapper.ItemRow
                            && newItem instanceof CartUiMapper.ItemRow) {
                        CartUiMapper.ItemRow oldRow = (CartUiMapper.ItemRow) oldItem;
                        CartUiMapper.ItemRow newRow = (CartUiMapper.ItemRow) newItem;
                        Item oldValue = oldRow.getItem();
                        Item newValue = newRow.getItem();
                        return oldRow.isSelected() == newRow.isSelected()
                                && oldValue.getId() == newValue.getId()
                                && oldValue.getSellerId() == newValue.getSellerId()
                                && oldValue.getPrice() == newValue.getPrice()
                                && Objects.equals(oldValue.getTitle(), newValue.getTitle())
                                && Objects.equals(oldValue.getCondition(), newValue.getCondition())
                                && Objects.equals(oldValue.getCategory(), newValue.getCategory())
                                && Objects.equals(oldValue.getLocation(), newValue.getLocation())
                                && Objects.equals(oldValue.getImageUrl(), newValue.getImageUrl())
                                && Objects.equals(oldValue.getSellerName(), newValue.getSellerName())
                                && Objects.equals(oldValue.getSellerMajor(), newValue.getSellerMajor());
                    }
                    return false;
                }
            };
}
