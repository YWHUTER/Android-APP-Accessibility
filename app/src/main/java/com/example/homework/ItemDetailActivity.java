package com.example.homework;

import com.example.homework.util.A11y;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.homework.data.CartRepository;
import com.example.homework.data.ItemRepository;
import com.example.homework.data.MessageRepository;
import com.example.homework.data.OrderRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.model.Item;
import com.example.homework.model.OrderRecord;
import com.example.homework.util.RemoteImageLoader;

import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ITEM_ID = "item_id";

    private ItemRepository itemRepository;
    private CartRepository cartRepository;
    private MessageRepository messageRepository;
    private OrderRepository orderRepository;
    private SessionManager sessionManager;
    private Item item;

    private ImageView ivDetailImage;
    private TextView tvDetailTitle;
    private TextView tvDetailMeta;
    private TextView tvDetailDescription;
    private TextView tvDetailLocation;
    private TextView tvDetailSeller;
    private TextView tvDetailStats;
    private TextView tvDetailSpecs;
    private TextView tvDetailPrice;
    private Button btnPrimary;
    private Button btnSecondary;
    private Button btnTertiary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        itemRepository = new ItemRepository(this);
        cartRepository = new CartRepository(this);
        messageRepository = new MessageRepository(this);
        orderRepository = new OrderRepository(this);
        sessionManager = new SessionManager(this);

        long itemId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);
        item = itemRepository.getItemById(itemId);
        if (item == null) {
            A11y.toast(this, "没有找到这件商品");
            finish();
            return;
        }

        bindViews();
        bindStaticContent();
        bindDynamicActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        item = itemRepository.getItemById(item.getId());
        if (item == null) {
            A11y.toast(this, "商品已经不存在了");
            finish();
            return;
        }
        bindStaticContent();
        bindDynamicActions();
    }

    private void bindViews() {
        ivDetailImage = findViewById(R.id.iv_detail_image);
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tvDetailMeta = findViewById(R.id.tv_detail_meta);
        tvDetailDescription = findViewById(R.id.tv_detail_description);
        tvDetailLocation = findViewById(R.id.tv_detail_location);
        tvDetailSeller = findViewById(R.id.tv_detail_seller);
        tvDetailStats = findViewById(R.id.tv_detail_stats);
        tvDetailSpecs = findViewById(R.id.tv_detail_specs);
        tvDetailPrice = findViewById(R.id.tv_detail_price);
        TextView btnBack = findViewById(R.id.btn_back);
        TextView btnHelp = findViewById(R.id.btn_detail_help);
        btnTertiary = findViewById(R.id.btn_detail_tertiary);
        btnSecondary = findViewById(R.id.btn_detail_secondary);
        btnPrimary = findViewById(R.id.btn_detail_primary);

        btnBack.setOnClickListener(v -> finish());
        btnHelp.setOnClickListener(v -> A11y.toast(this, "订单会依次经历待发货、已发货、已完成三个阶段。"));
    }

    private void bindStaticContent() {
        RemoteImageLoader.load(ivDetailImage, item.getImageUrl());

        tvDetailTitle.setText(item.getTitle());
        tvDetailMeta.setText(String.format(
                Locale.getDefault(),
                "%s | %s | 预计浏览 %d",
                item.getCategory(),
                item.getCondition(),
                Math.max(36, (int) Math.round(item.getPrice() / 10.0))
        ));
        tvDetailDescription.setText(item.getDescription());
        tvDetailLocation.setText(item.getLocation() + (item.getCampusPickupOnly() == 1
                ? "\n支持校内当面交易"
                : "\n支持校内面交或快递发货"));
        tvDetailSeller.setText(item.getSellerName() + " | " + item.getSellerMajor());
        tvDetailStats.setText("更适合 " + safe(item.getPreferredMajor()) + " 同学关注，标签包括：" + safe(item.getTags()));
        tvDetailSpecs.setText("商品成色：" + item.getCondition()
                + "\n推荐专业：" + safe(item.getPreferredMajor())
                + "\n商品分类：" + item.getCategory());
        tvDetailPrice.setText(String.format(Locale.getDefault(), "¥%.2f", item.getPrice()));
    }

    private void bindDynamicActions() {
        long currentUserId = sessionManager.getLoggedInUserId();
        boolean owner = item.getSellerId() == currentUserId;
        OrderRecord order = orderRepository.getOrderByItemId(item.getId());

        clearButtonActions();

        if (owner) {
            bindOwnerActions(order);
        } else {
            bindBuyerActions(order, currentUserId);
        }
    }

    private void bindOwnerActions(OrderRecord order) {
        if (order == null) {
            setupButton(btnTertiary, "暂无买家", false, false);
            setupButton(btnSecondary, "删除闲置", false, true);
            setupButton(btnPrimary, "编辑闲置", true, true);
            btnSecondary.setOnClickListener(v -> showDeleteDialog());
            btnPrimary.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_POST);
                intent.putExtra(MainActivity.EXTRA_EDIT_ITEM_ID, item.getId());
                startActivity(intent);
                finish();
            });
            return;
        }

        setupButton(btnTertiary, "联系买家", false, true);
        setupButton(btnSecondary, "我的卖出", false, true);
        btnTertiary.setOnClickListener(v -> openOrderChat(order));
        btnSecondary.setOnClickListener(v ->
                startActivity(new Intent(this, MySalesActivity.class)));

        if (OrderRepository.STATUS_PENDING.equals(order.getStatus())) {
            setupButton(btnPrimary, "确认发货", true, true);
            btnPrimary.setOnClickListener(v -> showShipDialog(order));
        } else if (OrderRepository.STATUS_SHIPPED.equals(order.getStatus())) {
            setupButton(btnPrimary, "等待收货", true, false);
        } else {
            setupButton(btnPrimary, "交易完成", true, false);
        }
    }

    private void bindBuyerActions(OrderRecord order, long currentUserId) {
        if (order == null) {
            boolean inCart = cartRepository.isInCart(currentUserId, item.getId());
            setupButton(btnTertiary, "联系商家", false, true);
            setupButton(btnSecondary, inCart ? "去购物车" : "加购物车", false, true);
            setupButton(btnPrimary, "立即购买", true, true);
            btnTertiary.setOnClickListener(v -> openChat(currentUserId, item.getSellerId()));
            btnSecondary.setOnClickListener(v -> handleCartAction(currentUserId, inCart));
            btnPrimary.setOnClickListener(v -> showPurchaseDialog(currentUserId));
            return;
        }

        setupButton(btnTertiary, "联系商家", false, true);
        btnTertiary.setOnClickListener(v -> {
            if (order.getBuyerId() == currentUserId) {
                openOrderChat(order);
            } else {
                openChat(currentUserId, item.getSellerId());
            }
        });

        if (order.getBuyerId() != currentUserId) {
            setupButton(btnSecondary, "商品已售出", false, false);
            setupButton(btnPrimary, "无法购买", true, false);
            return;
        }

        setupButton(btnSecondary, "我的买到", false, true);
        btnSecondary.setOnClickListener(v ->
                startActivity(new Intent(this, MyPurchasesActivity.class)));

        if (OrderRepository.STATUS_PENDING.equals(order.getStatus())) {
            setupButton(btnPrimary, "等待发货", true, false);
        } else if (OrderRepository.STATUS_SHIPPED.equals(order.getStatus())) {
            setupButton(btnPrimary, "确认收货", true, true);
            btnPrimary.setOnClickListener(v -> showCompleteDialog(order));
        } else {
            setupButton(btnPrimary, "交易完成", true, false);
        }
    }

    private void clearButtonActions() {
        btnPrimary.setOnClickListener(null);
        btnSecondary.setOnClickListener(null);
        btnTertiary.setOnClickListener(null);
    }

    private void setupButton(Button button, String text, boolean primary, boolean enabled) {
        button.setText(text);
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.68f);
        if (primary && enabled) {
            button.setBackgroundResource(R.drawable.bg_primary_gradient);
            button.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else if (!primary && enabled) {
            button.setBackgroundResource(R.drawable.bg_button_secondary);
            button.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
        } else {
            button.setBackgroundResource(R.drawable.bg_chip_inactive);
            button.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void showPurchaseDialog(long buyerId) {
        new AlertDialog.Builder(this)
                .setTitle("确认购买")
                .setMessage("购买成功后，订单会进入待发货阶段，卖家发货后你可以确认收货。")
                .setPositiveButton("确认购买", (dialog, which) -> {
                    OrderRepository.CreateOrderResult result = orderRepository.createOrder(item, buyerId);
                    cartRepository.removeItem(buyerId, item.getId());
                    if (result.getState() == OrderRepository.CreateOrderResult.STATE_CREATED) {
                        A11y.toast(this, "已下单，等待卖家发货");
                    } else if (result.getState() == OrderRepository.CreateOrderResult.STATE_ALREADY_PURCHASED) {
                        A11y.toast(this, "你已经下过单了，可以直接去我的买到查看进度");
                    } else if (result.getState() == OrderRepository.CreateOrderResult.STATE_OWN_ITEM) {
                        A11y.toast(this, "不能购买自己发布的闲置");
                    } else if (result.getState() == OrderRepository.CreateOrderResult.STATE_UNAVAILABLE) {
                        A11y.toast(this, "这件商品已下架或不可购买");
                    } else {
                        A11y.toast(this, "这件商品已经被其他同学购买");
                    }
                    bindDynamicActions();
                })
                .setNegativeButton("再想想", null)
                .show();
    }

    private void showShipDialog(OrderRecord order) {
        new AlertDialog.Builder(this)
                .setTitle("确认发货")
                .setMessage("发货后，订单会进入已发货阶段，等待买家确认收货。")
                .setPositiveButton("确认发货", (dialog, which) -> {
                    if (orderRepository.markShipped(order.getId())) {
                        A11y.toast(this, "已发货，等待买家确认收货");
                    } else {
                        A11y.toast(this, "当前订单状态已变化，请刷新后重试");
                    }
                    bindDynamicActions();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCompleteDialog(OrderRecord order) {
        new AlertDialog.Builder(this)
                .setTitle("确认收货")
                .setMessage("确认收货后，这笔交易会变为已完成。")
                .setPositiveButton("确认收货", (dialog, which) -> {
                    if (orderRepository.markCompleted(order.getId())) {
                        A11y.toast(this, "已确认收货，交易完成");
                    } else {
                        A11y.toast(this, "当前订单还不能确认收货，请刷新后重试");
                    }
                    bindDynamicActions();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("删除闲置")
                .setMessage("删除后，这条闲置信息将无法恢复。")
                .setPositiveButton("删除", (dialog, which) -> {
                    itemRepository.delete(item.getId());
                    A11y.toast(this, "闲置已删除");
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void handleCartAction(long currentUserId, boolean inCart) {
        if (inCart) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.TAB_CART);
            startActivity(intent);
            return;
        }

        CartRepository.AddToCartResult result = cartRepository.addItem(currentUserId, item);
        if (result.getState() == CartRepository.AddToCartResult.STATE_ADDED) {
            A11y.toast(this, "已加入购物车");
        } else if (result.getState() == CartRepository.AddToCartResult.STATE_ALREADY_IN_CART) {
            A11y.toast(this, "这件商品已经在购物车里了");
        } else if (result.getState() == CartRepository.AddToCartResult.STATE_OWN_ITEM) {
            A11y.toast(this, "不能把自己发布的商品加入购物车");
        } else {
            A11y.toast(this, "这件商品当前无法加入购物车");
        }
        bindDynamicActions();
    }

    private void openChat(long buyerId, long sellerId) {
        long conversationId = messageRepository.getOrCreateConversation(item.getId(), buyerId, sellerId);
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
        startActivity(intent);
    }

    private void openOrderChat(OrderRecord order) {
        long conversationId = messageRepository.getOrCreateConversation(
                item.getId(),
                order.getBuyerId(),
                order.getSellerId()
        );
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
        startActivity(intent);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "不限" : value;
    }
}
