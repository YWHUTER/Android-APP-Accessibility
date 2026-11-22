package com.example.homework.ui;

import com.example.homework.util.A11y;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.ChatActivity;
import com.example.homework.MainNavigator;
import com.example.homework.MyPurchasesActivity;
import com.example.homework.R;
import com.example.homework.data.CartRepository;
import com.example.homework.data.MessageRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.model.Item;

import java.util.ArrayList;
import java.util.Locale;

public class CartFragment extends Fragment {
    private MainNavigator navigator;
    private MessageRepository messageRepository;
    private SessionManager sessionManager;
    private CartViewModel viewModel;
    private CartListAdapter cartListAdapter;

    private TextView tvCartSubtitle;
    private View emptyState;
    private RecyclerView recyclerCartRows;
    private ProgressBar progressBar;
    private AppCompatCheckBox cbSelectAll;
    private TextView tvCartTotalLabel;
    private TextView tvCartTotalPrice;
    private AppCompatButton btnCartExplore;
    private AppCompatButton btnCartCheckout;

    private boolean applyingSelectionUi;
    private long lastHandledEventId = -1L;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        navigator = (MainNavigator) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        messageRepository = new MessageRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(CartViewModel.class);

        tvCartSubtitle = view.findViewById(R.id.cart_subtitle);
        emptyState = view.findViewById(R.id.layout_cart_empty_state);
        recyclerCartRows = view.findViewById(R.id.recycler_cart_rows);
        progressBar = view.findViewById(R.id.progress_cart);
        cbSelectAll = view.findViewById(R.id.cb_cart_select_all);
        tvCartTotalLabel = view.findViewById(R.id.cart_total_label);
        tvCartTotalPrice = view.findViewById(R.id.cart_total_price);
        btnCartExplore = view.findViewById(R.id.btn_cart_explore);
        btnCartCheckout = view.findViewById(R.id.btn_cart_checkout);

        setupRecyclerView();

        btnCartExplore.setOnClickListener(v -> navigator.openDiscovery());
        btnCartCheckout.setOnClickListener(v -> showCheckoutDialog());
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (applyingSelectionUi) {
                return;
            }
            viewModel.toggleSelectAll(isChecked);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getEvents().observe(getViewLifecycleOwner(), this::handleEvent);
        viewModel.refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void setupRecyclerView() {
        cartListAdapter = new CartListAdapter(new CartListAdapter.CartRowListener() {
            @Override
            public void onItemSelectionChanged(long itemId, boolean selected) {
                viewModel.setItemSelected(itemId, selected);
            }

            @Override
            public void onItemClick(Item item) {
                navigator.openItemDetail(item.getId());
            }

            @Override
            public void onContactSeller(Item item) {
                openChat(item);
            }

            @Override
            public void onRemoveItem(Item item) {
                viewModel.removeItem(item.getId());
            }
        });
        recyclerCartRows.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCartRows.setAdapter(cartListAdapter);
        recyclerCartRows.setItemAnimator(null);
    }

    private void renderState(CartViewModel.CartState state) {
        tvCartSubtitle.setText(state.getSubtitle());
        cartListAdapter.submitList(new ArrayList<>(state.getRows()));

        boolean empty = state.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerCartRows.setVisibility(empty ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

        CartUiMapper.CartSummary summary = state.getSummary();
        applyingSelectionUi = true;
        cbSelectAll.setChecked(summary.isSelectAllChecked());
        applyingSelectionUi = false;
        cbSelectAll.setEnabled(summary.isSelectAllEnabled());
        cbSelectAll.setAlpha(summary.isSelectAllEnabled() ? 1f : 0.6f);

        tvCartTotalLabel.setText(summary.getTotalLabel());
        tvCartTotalPrice.setText(summary.getTotalPrice());
        btnCartCheckout.setEnabled(summary.isCheckoutEnabled());
        btnCartCheckout.setAlpha(summary.isCheckoutEnabled() ? 1f : 0.6f);
        btnCartCheckout.setText(summary.getCheckoutLabel());
        navigator.refreshNavigationIndicators();
    }

    private void handleEvent(CartViewModel.CartEvent event) {
        if (event == null || event.getEventId() <= lastHandledEventId) {
            return;
        }
        lastHandledEventId = event.getEventId();

        if (event.getType() == CartViewModel.CartEvent.TYPE_TOAST) {
            A11y.toast(requireContext(), event.getMessage());
            return;
        }

        CartRepository.CheckoutResult result = event.getCheckoutResult();
        if (result == null) {
            return;
        }

        if (result.getCreatedCount() > 0) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("结算完成")
                    .setMessage(CartUiMapper.buildCheckoutMessage(result))
                    .setPositiveButton("查看订单", (dialog, which) ->
                            startActivity(new Intent(requireContext(), MyPurchasesActivity.class)))
                    .setNegativeButton("继续逛逛", null)
                    .show();
            return;
        }

        if (result.getAlreadyPurchasedCount() > 0) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("购物车已同步")
                    .setMessage(CartUiMapper.buildCheckoutMessage(result))
                    .setPositiveButton("查看订单", (dialog, which) ->
                            startActivity(new Intent(requireContext(), MyPurchasesActivity.class)))
                    .setNegativeButton("知道了", null)
                    .show();
            return;
        }

        A11y.toast(requireContext(), "这些勾选商品当前无法下单，已从购物车移除");
    }

    private void showCheckoutDialog() {
        CartViewModel.CartState state = viewModel.getUiState().getValue();
        if (state == null || state.getSummary().getSelectedCount() <= 0) {
            return;
        }

        CartUiMapper.CartSummary summary = state.getSummary();
        new AlertDialog.Builder(requireContext())
                .setTitle("确认结算")
                .setMessage(String.format(
                        Locale.getDefault(),
                        "将为你创建 %d 笔订单，合计 %s。",
                        summary.getSelectedCount(),
                        CartUiMapper.formatPrice(summary.getSelectedTotal())
                ))
                .setPositiveButton("确认结算", (dialog, which) -> viewModel.checkoutSelected())
                .setNegativeButton("取消", null)
                .show();
    }

    private void openChat(Item item) {
        long conversationId = messageRepository.getOrCreateConversation(
                item.getId(),
                sessionManager.getLoggedInUserId(),
                item.getSellerId()
        );
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
        startActivity(intent);
    }
}
