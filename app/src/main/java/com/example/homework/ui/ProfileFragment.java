package com.example.homework.ui;

import com.example.homework.util.A11y;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homework.CommunityActivity;
import com.example.homework.EditProfileActivity;
import com.example.homework.LoginActivity;
import com.example.homework.MainNavigator;
import com.example.homework.MyPurchasesActivity;
import com.example.homework.MySalesActivity;
import com.example.homework.R;
import com.example.homework.SettingsActivity;
import com.example.homework.data.SessionManager;
import com.example.homework.model.Item;
import com.example.homework.util.ImageStorageHelper;
import com.example.homework.util.RemoteImageLoader;

import java.util.ArrayList;

public class ProfileFragment extends Fragment {
    private MainNavigator navigator;
    private SessionManager sessionManager;
    private ProfileViewModel viewModel;
    private MyListingAdapter listingAdapter;

    private NestedScrollView scrollProfile;
    private View layoutProfileAvatar;
    private ImageView ivProfileAvatar;
    private TextView tvProfileAvatar;
    private TextView tvProfileName;
    private TextView tvProfileMajor;
    private TextView tvProfileBio;
    private TextView tvProfilePurchaseCount;
    private TextView tvProfileSaleCount;
    private TextView tvProfileInterestSummary;
    private TextView tvMyItemsEmpty;
    private ProgressBar progressProfile;
    private RecyclerView recyclerMyItems;

    private long lastHandledEventId = -1L;
    private final ActivityResultLauncher<String> avatarPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleAvatarSelected);

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        navigator = (MainNavigator) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        scrollProfile = view.findViewById(R.id.scroll_profile);
        layoutProfileAvatar = view.findViewById(R.id.layout_profile_avatar);
        ivProfileAvatar = view.findViewById(R.id.iv_profile_avatar);
        tvProfileAvatar = view.findViewById(R.id.tv_profile_avatar);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileMajor = view.findViewById(R.id.tv_profile_major);
        tvProfileBio = view.findViewById(R.id.tv_profile_bio);
        tvProfilePurchaseCount = view.findViewById(R.id.tv_profile_purchase_count);
        tvProfileSaleCount = view.findViewById(R.id.tv_profile_sale_count);
        tvProfileInterestSummary = view.findViewById(R.id.tv_profile_interest_summary);
        tvMyItemsEmpty = view.findViewById(R.id.tv_my_items_empty);
        progressProfile = view.findViewById(R.id.progress_profile);
        recyclerMyItems = view.findViewById(R.id.recycler_my_items);

        setupRecyclerView();

        TextView btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        TextView btnProfileSettings = view.findViewById(R.id.btn_profile_settings);
        View cardPurchases = view.findViewById(R.id.card_purchases);
        View cardSales = view.findViewById(R.id.card_sales);
        View rowMyListings = view.findViewById(R.id.row_account_listings);
        View rowCommunity = view.findViewById(R.id.row_account_community);
        View rowSettings = view.findViewById(R.id.row_account_settings);
        Button btnLogout = view.findViewById(R.id.btn_logout);

        layoutProfileAvatar.setOnClickListener(v -> openAvatarPicker());
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(requireContext(), EditProfileActivity.class)));
        btnProfileSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));
        cardPurchases.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyPurchasesActivity.class)));
        cardSales.setOnClickListener(v -> startActivity(new Intent(requireContext(), MySalesActivity.class)));
        rowMyListings.setOnClickListener(v -> scrollToListings());
        rowCommunity.setOnClickListener(v -> startActivity(new Intent(requireContext(), CommunityActivity.class)));
        rowSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));
        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
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
        listingAdapter = new MyListingAdapter(new MyListingAdapter.ListingActionListener() {
            @Override
            public void onOpen(Item item) {
                navigator.openItemDetail(item.getId());
            }

            @Override
            public void onEdit(Item item) {
                navigator.openPostEditor(item.getId());
            }

            @Override
            public void onDelete(Item item) {
                showDeleteDialog(item);
            }
        });
        recyclerMyItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerMyItems.setAdapter(listingAdapter);
        recyclerMyItems.setItemAnimator(null);
    }

    private void renderState(ProfileViewModel.ProfileState state) {
        bindAvatar(state);
        tvProfileName.setText(state.getName());
        tvProfileMajor.setText(state.getMajor());
        tvProfileBio.setText(state.getBio());
        tvProfilePurchaseCount.setText(state.getPurchaseCount());
        tvProfileSaleCount.setText(state.getSaleCount());
        tvProfileInterestSummary.setText(state.getInterestSummary());
        tvMyItemsEmpty.setText(state.getListingsEmptyText());

        listingAdapter.submitList(new ArrayList<>(state.getListings()));
        progressProfile.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        tvMyItemsEmpty.setVisibility(!state.isLoading() && state.getListings().isEmpty() ? View.VISIBLE : View.GONE);
        recyclerMyItems.setVisibility(state.getListings().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void handleEvent(ProfileViewModel.ProfileEvent event) {
        if (event == null || event.getEventId() <= lastHandledEventId) {
            return;
        }
        lastHandledEventId = event.getEventId();
        A11y.toast(requireContext(), event.getMessage());
    }

    private void showDeleteDialog(Item item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除闲置")
                .setMessage("确定删除“" + item.getTitle() + "”吗？")
                .setPositiveButton("删除", (dialog, which) -> viewModel.deleteListing(item.getId()))
                .setNegativeButton("取消", null)
                .show();
    }

    private void scrollToListings() {
        scrollProfile.post(() -> scrollProfile.smoothScrollTo(0, recyclerMyItems.getTop()));
    }

    private void openAvatarPicker() {
        avatarPickerLauncher.launch("image/*");
    }

    private void handleAvatarSelected(Uri uri) {
        if (uri == null || !isAdded()) {
            return;
        }

        String savedPath = ImageStorageHelper.copyToInternalStorage(requireContext(), uri);
        if (TextUtils.isEmpty(savedPath)) {
            A11y.toast(requireContext(), "头像保存失败，请重试。");
            return;
        }
        viewModel.updateAvatar(savedPath);
    }

    private void bindAvatar(ProfileViewModel.ProfileState state) {
        String avatarImageUrl = state.getAvatarImageUrl();
        if (TextUtils.isEmpty(avatarImageUrl)) {
            ivProfileAvatar.setVisibility(View.GONE);
            tvProfileAvatar.setVisibility(View.VISIBLE);
            tvProfileAvatar.setText(state.getAvatarText());
            return;
        }

        tvProfileAvatar.setVisibility(View.GONE);
        ivProfileAvatar.setVisibility(View.VISIBLE);
        RemoteImageLoader.load(ivProfileAvatar, avatarImageUrl);
    }
}
