package com.example.homework.ui;

import com.example.homework.util.A11y;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.R;
import com.example.homework.data.OrderRepository;
import com.example.homework.model.OrderRecord;
import com.example.homework.util.RemoteImageLoader;

import java.util.Locale;
import java.util.Objects;

public class OrderListAdapter extends ListAdapter<OrderRecord, OrderListAdapter.OrderViewHolder> {
    private final Mode mode;
    private final OrderActionListener orderActionListener;

    public OrderListAdapter(Mode mode, OrderActionListener orderActionListener) {
        super(DIFF_CALLBACK);
        this.mode = mode;
        this.orderActionListener = orderActionListener;
        setHasStableIds(true);
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_card, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(getItem(position), mode, orderActionListener);
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbImageView;
        private final TextView thumbFallbackView;
        private final TextView titleView;
        private final TextView statusView;
        private final TextView priceView;
        private final TextView counterpartView;
        private final TextView metaView;
        private final Button secondaryButton;
        private final Button primaryButton;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbImageView = itemView.findViewById(R.id.iv_order_thumb);
            thumbFallbackView = itemView.findViewById(R.id.tv_order_thumb_text);
            titleView = itemView.findViewById(R.id.tv_order_title);
            statusView = itemView.findViewById(R.id.tv_order_status);
            priceView = itemView.findViewById(R.id.tv_order_price);
            counterpartView = itemView.findViewById(R.id.tv_order_counterpart);
            metaView = itemView.findViewById(R.id.tv_order_meta);
            secondaryButton = itemView.findViewById(R.id.btn_order_secondary);
            primaryButton = itemView.findViewById(R.id.btn_order_primary);
        }

        void bind(OrderRecord order, Mode mode, OrderActionListener orderActionListener) {
            titleView.setText(order.getItemTitle());
            priceView.setText(String.format(Locale.getDefault(), "¥%.2f", order.getItemPrice()));
            counterpartView.setText(mode == Mode.BUYER
                    ? "卖家：" + safeText(order.getSellerName(), "校园卖家")
                    : "买家：" + safeText(order.getBuyerName(), "校园买家"));
            metaView.setText(order.getItemCategory() + " | " + order.getItemCondition() + " | " + order.getItemLocation());

            bindThumb(order);
            A11y.itemImage(thumbImageView, thumbImageView.getContext()
                    .getString(R.string.cd_item_image_with_name, order.getItemTitle()));
            applyStatusStyle(order.getStatus());

            secondaryButton.setText(mode == Mode.BUYER ? "联系卖家" : "联系买家");
            secondaryButton.setOnClickListener(v -> orderActionListener.onSecondaryAction(order));

            PrimaryAction primaryAction = resolvePrimaryAction(mode, order.getStatus());
            stylePrimaryAction(primaryAction);
            if (primaryAction.enabled) {
                primaryButton.setOnClickListener(v -> orderActionListener.onPrimaryAction(order));
            } else {
                primaryButton.setOnClickListener(null);
            }

            itemView.setOnClickListener(v -> orderActionListener.onItemClick(order));
        }

        private void bindThumb(OrderRecord order) {
            if (!TextUtils.isEmpty(order.getItemImageUrl())) {
                RemoteImageLoader.load(thumbImageView, order.getItemImageUrl());
                thumbImageView.setVisibility(View.VISIBLE);
                thumbFallbackView.setVisibility(View.GONE);
                return;
            }

            thumbImageView.setVisibility(View.GONE);
            thumbFallbackView.setVisibility(View.VISIBLE);
            thumbFallbackView.setText(shortLabel(order.getItemCategory()));
        }

        private void applyStatusStyle(String status) {
            statusView.setText(status);
            if (OrderRepository.STATUS_PENDING.equals(status)) {
                statusView.setBackgroundResource(R.drawable.bg_order_status_pending);
                statusView.setTextColor(Color.parseColor("#B26B00"));
            } else if (OrderRepository.STATUS_SHIPPED.equals(status)) {
                statusView.setBackgroundResource(R.drawable.bg_order_status_shipped);
                statusView.setTextColor(Color.parseColor("#0E57D2"));
            } else {
                statusView.setBackgroundResource(R.drawable.bg_order_status_completed);
                statusView.setTextColor(Color.parseColor("#217246"));
            }
        }

        private void stylePrimaryAction(PrimaryAction primaryAction) {
            primaryButton.setText(primaryAction.label);
            primaryButton.setEnabled(primaryAction.enabled);
            primaryButton.setAlpha(primaryAction.enabled ? 1f : 0.7f);
            if (primaryAction.enabled) {
                primaryButton.setBackgroundResource(R.drawable.bg_primary_gradient);
                primaryButton.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            } else {
                primaryButton.setBackgroundResource(R.drawable.bg_chip_inactive);
                primaryButton.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            }
        }

        private PrimaryAction resolvePrimaryAction(Mode mode, String status) {
            if (mode == Mode.BUYER) {
                if (OrderRepository.STATUS_PENDING.equals(status)) {
                    return new PrimaryAction("等待发货", false);
                }
                if (OrderRepository.STATUS_SHIPPED.equals(status)) {
                    return new PrimaryAction("确认收货", true);
                }
                return new PrimaryAction("交易完成", false);
            }

            if (OrderRepository.STATUS_PENDING.equals(status)) {
                return new PrimaryAction("确认发货", true);
            }
            if (OrderRepository.STATUS_SHIPPED.equals(status)) {
                return new PrimaryAction("等待收货", false);
            }
            return new PrimaryAction("交易完成", false);
        }

        private String shortLabel(String text) {
            if (text == null || text.trim().isEmpty()) {
                return "闲置";
            }
            return text.length() > 2 ? text.substring(0, 2) : text;
        }

        private String safeText(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value;
        }
    }

    public enum Mode {
        BUYER,
        SELLER
    }

    public interface OrderActionListener {
        void onSecondaryAction(OrderRecord order);

        void onPrimaryAction(OrderRecord order);

        void onItemClick(OrderRecord order);
    }

    private static final class PrimaryAction {
        private final String label;
        private final boolean enabled;

        private PrimaryAction(String label, boolean enabled) {
            this.label = label;
            this.enabled = enabled;
        }
    }

    private static final DiffUtil.ItemCallback<OrderRecord> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<OrderRecord>() {
                @Override
                public boolean areItemsTheSame(@NonNull OrderRecord oldItem, @NonNull OrderRecord newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull OrderRecord oldItem, @NonNull OrderRecord newItem) {
                    return oldItem.getId() == newItem.getId()
                            && oldItem.getItemId() == newItem.getItemId()
                            && oldItem.getBuyerId() == newItem.getBuyerId()
                            && oldItem.getSellerId() == newItem.getSellerId()
                            && oldItem.getItemPrice() == newItem.getItemPrice()
                            && oldItem.getCreatedAt() == newItem.getCreatedAt()
                            && oldItem.getShippedAt() == newItem.getShippedAt()
                            && oldItem.getCompletedAt() == newItem.getCompletedAt()
                            && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                            && Objects.equals(oldItem.getItemTitle(), newItem.getItemTitle())
                            && Objects.equals(oldItem.getItemLocation(), newItem.getItemLocation())
                            && Objects.equals(oldItem.getItemCondition(), newItem.getItemCondition())
                            && Objects.equals(oldItem.getItemCategory(), newItem.getItemCategory())
                            && Objects.equals(oldItem.getItemImageUrl(), newItem.getItemImageUrl())
                            && Objects.equals(oldItem.getBuyerName(), newItem.getBuyerName())
                            && Objects.equals(oldItem.getSellerName(), newItem.getSellerName());
                }
            };
}
