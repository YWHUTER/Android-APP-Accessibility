package com.example.homework.ui;

import com.example.homework.util.A11y;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.homework.AmapPlacePickerActivity;
import com.example.homework.MainNavigator;
import com.example.homework.R;
import com.example.homework.ai.DeepSeekClient;
import com.example.homework.data.ItemRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.model.Item;
import com.example.homework.util.ImageStorageHelper;
import com.example.homework.util.ItemImageCodec;
import com.example.homework.util.RemoteImageLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostFragment extends Fragment {
    private static final String ARG_ITEM_ID = "item_id";
    private static final int MAX_PHOTOS = 10;

    private MainNavigator navigator;
    private ItemRepository itemRepository;
    private SessionManager sessionManager;
    private long editingItemId = -1L;
    private EditText etTitle;
    private EditText etDescription;
    private EditText etPrice;
    private EditText etCondition;
    private EditText etLocation;
    private Spinner spCategory;
    private Spinner spPreferredMajor;
    private SwitchCompat switchPickup;
    private TextView tvPostHeader;
    private View btnSave;
    private TextView btnAiPostAssist;
    private TextView btnAiPriceAssist;
    private TextView tvAiPostTags;
    private TextView tvAiPriceSuggestion;
    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> majorAdapter;
    private DeepSeekClient deepSeekClient;
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean aiPostLoading;
    private boolean aiPriceLoading;
    private int aiRequestVersion;
    private int aiPriceRequestVersion;
    private String aiSuggestedTags = "";
    private final Map<String, DeepSeekClient.PostDraft> aiPostDraftCache = new HashMap<>();
    private final Map<String, DeepSeekClient.PriceSuggestion> aiPriceSuggestionCache = new HashMap<>();

    private final List<String> selectedImagePaths = new ArrayList<>();
    private ImageView ivPreview1;
    private ImageView ivPreview2;
    private ImageView ivPreview3;
    private View framePreview1;
    private View framePreview2;
    private View framePreview3;
    private View btnRemovePreview1;
    private View btnRemovePreview2;
    private View btnRemovePreview3;
    private TextView tvPreviewMore;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris == null || uris.isEmpty()) {
                    return;
                }
                int remain = MAX_PHOTOS - selectedImagePaths.size();
                if (remain <= 0) {
                    A11y.toast(requireContext(), "最多只能添加 10 张图片。");
                    return;
                }
                int added = 0;
                for (Uri uri : uris) {
                    if (added >= remain) {
                        break;
                    }
                    String savedPath = ImageStorageHelper.copyToInternalStorage(requireContext(), uri);
                    if (!TextUtils.isEmpty(savedPath)) {
                        selectedImagePaths.add(savedPath);
                        added++;
                    }
                }
                renderImagePreviewStrip();
                if (added > 0) {
                    A11y.toast(requireContext(), "已添加 " + added + " 张图片。");
                } else {
                    A11y.toast(requireContext(), "图片保存失败，请重试。");
                }
            });

    private final ActivityResultLauncher<Intent> amapPlacePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || etLocation == null) {
                    return;
                }
                String selectedAddress = result.getData().getStringExtra(AmapPlacePickerActivity.EXTRA_SELECTED_ADDRESS);
                if (TextUtils.isEmpty(selectedAddress)) {
                    return;
                }
                etLocation.setText(selectedAddress);
                etLocation.setSelection(etLocation.getText().length());
                if (isAdded()) {
                    A11y.toast(requireContext(), "\u5df2\u56de\u586b\u9ad8\u5fb7\u5730\u70b9");
                }
            });

    public static PostFragment newInstance(long itemId) {
        PostFragment fragment = new PostFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ITEM_ID, itemId);
        fragment.setArguments(args);
        return fragment;
    }

    public void showEditorForItem(long itemId) {
        editingItemId = itemId;
        if (getArguments() != null) {
            getArguments().putLong(ARG_ITEM_ID, itemId);
        }
        if (getView() != null) {
            populateFormForEditing(itemId);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        navigator = (MainNavigator) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        itemRepository = new ItemRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        deepSeekClient = new DeepSeekClient();
        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            editingItemId = getArguments().getLong(ARG_ITEM_ID, editingItemId);
        }

        etTitle = view.findViewById(R.id.et_post_title);
        etDescription = view.findViewById(R.id.et_post_description);
        etPrice = view.findViewById(R.id.et_post_price);
        etCondition = view.findViewById(R.id.et_post_condition);
        etLocation = view.findViewById(R.id.et_post_location);
        spCategory = view.findViewById(R.id.sp_category);
        spPreferredMajor = view.findViewById(R.id.sp_post_preferred_major);
        switchPickup = view.findViewById(R.id.switch_pickup);
        tvPostHeader = view.findViewById(R.id.tv_post_header);
        btnSave = view.findViewById(R.id.btn_save_item);
        btnAiPostAssist = view.findViewById(R.id.btn_ai_post_assist);
        btnAiPriceAssist = view.findViewById(R.id.btn_ai_price_assist);
        tvAiPostTags = view.findViewById(R.id.tv_ai_post_tags);
        tvAiPriceSuggestion = view.findViewById(R.id.tv_ai_price_suggestion);
        View btnPickLocationAmap = view.findViewById(R.id.btn_pick_location_amap);
        View layoutUpload = view.findViewById(R.id.layout_upload_photos);
        View layoutProTip = view.findViewById(R.id.layout_pro_tip);
        View btnCloseProTip = view.findViewById(R.id.btn_close_pro_tip);

        if (btnCloseProTip != null && layoutProTip != null) {
            btnCloseProTip.setOnClickListener(v -> layoutProTip.setVisibility(View.GONE));
        }

        framePreview1 = view.findViewById(R.id.frame_preview_1);
        framePreview2 = view.findViewById(R.id.frame_preview_2);
        framePreview3 = view.findViewById(R.id.frame_preview_3);
        btnRemovePreview1 = view.findViewById(R.id.btn_remove_preview_1);
        btnRemovePreview2 = view.findViewById(R.id.btn_remove_preview_2);
        btnRemovePreview3 = view.findViewById(R.id.btn_remove_preview_3);
        ivPreview1 = view.findViewById(R.id.iv_preview_1);
        ivPreview2 = view.findViewById(R.id.iv_preview_2);
        ivPreview3 = view.findViewById(R.id.iv_preview_3);
        tvPreviewMore = view.findViewById(R.id.tv_preview_more);

        btnRemovePreview1.setOnClickListener(v -> removeImageAt(0));
        btnRemovePreview2.setOnClickListener(v -> removeImageAt(1));
        btnRemovePreview3.setOnClickListener(v -> removeImageAt(2));

        setupHintBehavior(etTitle);
        setupHintBehavior(etDescription);
        setupHintBehavior(etPrice);
        setupHintBehavior(etCondition);
        setupHintBehavior(etLocation);

        String[] categories = {"教材", "电子产品", "宿舍用品", "自行车", "其他"};
        categoryAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, categories
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);

        String[] preferredMajors = {
                "不限专业", "计算机科学与技术", "软件工程", "通信工程", "电子信息工程",
                "自动化", "机械设计制造及其自动化", "工商管理", "会计学",
                "法学", "汉语言文学", "英语", "数学与应用数学", "物理学", "其他专业"
        };
        majorAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, preferredMajors
        );
        majorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPreferredMajor.setAdapter(majorAdapter);

        layoutUpload.setOnClickListener(v -> openImagePicker());
        btnAiPostAssist.setOnClickListener(v -> generatePostDraftWithAi());
        btnAiPriceAssist.setOnClickListener(v -> generatePriceSuggestionWithAi());
        if (btnPickLocationAmap != null) {
            btnPickLocationAmap.setOnClickListener(v -> openAmapPlacePicker());
        }

        if (editingItemId > 0) {
            populateFormForEditing(editingItemId);
        }

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String priceText = etPrice.getText().toString().trim();
            long currentUserId = sessionManager.getLoggedInUserId();

            if (currentUserId <= 0L) {
                A11y.toast(requireContext(), "登录状态已失效，请重新登录后再发布。");
                return;
            }

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(priceText)) {
                A11y.toast(requireContext(), "标题、描述和价格为必填项。");
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceText);
            } catch (NumberFormatException exception) {
                A11y.toast(requireContext(), "请输入正确的价格数字。");
                return;
            }

            if (price <= 0d) {
                A11y.toast(requireContext(), "价格需要大于 0。");
                return;
            }

            Item item = editingItemId > 0 ? itemRepository.getItemById(editingItemId) : new Item();
            if (editingItemId > 0) {
                if (item == null) {
                    A11y.toast(requireContext(), "没找到要编辑的闲置。");
                    return;
                }
                if (item.getSellerId() != currentUserId) {
                    A11y.toast(requireContext(), "只能编辑自己发布的闲置。");
                    navigator.openProfile();
                    return;
                }
            } else if (item == null) {
                item = new Item();
            }

            item.setId(editingItemId);
            item.setSellerId(currentUserId);
            item.setTitle(title);
            item.setDescription(description);
            item.setCategory(spCategory.getSelectedItem().toString());
            item.setPrice(price);

            String condition = etCondition.getText().toString().trim();
            item.setCondition(TextUtils.isEmpty(condition) ? "九成新" : condition);

            String location = etLocation.getText().toString().trim();
            item.setLocation(TextUtils.isEmpty(location) ? "校内" : location);

            item.setImageUrl(ItemImageCodec.encode(selectedImagePaths));

            if (!TextUtils.isEmpty(aiSuggestedTags)) {
                item.setTags(aiSuggestedTags);
            } else if (TextUtils.isEmpty(item.getTags())) {
                item.setTags(spCategory.getSelectedItem().toString());
            }

            String preferredMajor = spPreferredMajor.getSelectedItem().toString();
            item.setPreferredMajor("不限专业".equals(preferredMajor) ? "" : preferredMajor);
            item.setCampusPickupOnly(switchPickup.isChecked() ? 1 : 0);

            itemRepository.save(item);
            A11y.toast(requireContext(), editingItemId > 0 ? "修改已保存。" : "闲置已发布。");
            resetDraftForm();
            navigator.openProfile();
        });
    }

    private void populateFormForEditing(long itemId) {
        if (itemId <= 0L || getView() == null) {
            return;
        }

        Item item = itemRepository.getItemById(itemId);
        if (item == null) {
            A11y.toast(requireContext(), "没找到要编辑的闲置。");
            return;
        }
        if (item.getSellerId() != sessionManager.getLoggedInUserId()) {
            A11y.toast(requireContext(), "只能编辑自己发布的闲置。");
            editingItemId = -1L;
            navigator.openProfile();
            return;
        }

        editingItemId = itemId;
        tvPostHeader.setText("编辑闲置");
        if (btnSave instanceof TextView) {
            ((TextView) btnSave).setText("保存修改");
        }
        etTitle.setText(item.getTitle());
        etDescription.setText(item.getDescription());
        etPrice.setText(String.valueOf(item.getPrice()));
        etCondition.setText(item.getCondition());
        etLocation.setText(item.getLocation());
        switchPickup.setChecked(item.getCampusPickupOnly() == 1);

        int categoryIndex = categoryAdapter.getPosition(item.getCategory());
        if (categoryIndex >= 0) {
            spCategory.setSelection(categoryIndex);
        }

        String preferredMajor = TextUtils.isEmpty(item.getPreferredMajor())
                ? "不限专业"
                : item.getPreferredMajor();
        int majorIndex = majorAdapter.getPosition(preferredMajor);
        if (majorIndex >= 0) {
            spPreferredMajor.setSelection(majorIndex);
        }

        selectedImagePaths.clear();
        selectedImagePaths.addAll(ItemImageCodec.decode(item.getImageUrl()));
        renderImagePreviewStrip();
    }

    private void resetDraftForm() {
        editingItemId = -1L;
        if (getArguments() != null) {
            getArguments().putLong(ARG_ITEM_ID, -1L);
        }
        if (tvPostHeader != null) {
            tvPostHeader.setText("发布闲置");
        }
        if (btnSave instanceof TextView) {
            ((TextView) btnSave).setText("发布");
        }
        etTitle.setText("");
        etDescription.setText("");
        etPrice.setText("");
        etCondition.setText("");
        etLocation.setText("");
        spCategory.setSelection(0);
        spPreferredMajor.setSelection(0);
        switchPickup.setChecked(true);
        selectedImagePaths.clear();
        aiSuggestedTags = "";
        if (tvAiPostTags != null) {
            tvAiPostTags.setVisibility(View.GONE);
            tvAiPostTags.setText("");
        }
        if (tvAiPriceSuggestion != null) {
            tvAiPriceSuggestion.setVisibility(View.GONE);
            tvAiPriceSuggestion.setText("");
        }
        renderImagePreviewStrip();
    }

    private void generatePostDraftWithAi() {
        if (aiPostLoading) {
            return;
        }
        if (deepSeekClient == null || !deepSeekClient.isConfigured()) {
            A11y.toast(requireContext(), "\u8bf7\u5148\u5728 local.properties \u914d\u7f6e deepseek.apiKey\u3002");
            return;
        }

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(description)) {
            A11y.toast(requireContext(), "\u8bf7\u5148\u586b\u5199\u6807\u9898\u6216\u63cf\u8ff0\uff0c\u518d\u8ba9 AI \u4f18\u5316\u3002");
            return;
        }

        int requestId = ++aiRequestVersion;
        String category = spCategory.getSelectedItem() == null ? "" : spCategory.getSelectedItem().toString();
        String price = etPrice.getText().toString().trim();
        String condition = etCondition.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String cacheKey = buildPostDraftCacheKey(title, description, category, price, condition, location);
        DeepSeekClient.PostDraft cachedDraft = aiPostDraftCache.get(cacheKey);
        if (cachedDraft != null) {
            applyAiPostDraft(cachedDraft);
            A11y.toast(requireContext(), "\u5df2\u590d\u7528\u4e0a\u6b21 AI \u6587\u6848");
            return;
        }

        setAiPostLoading(true);
        aiExecutor.execute(() -> {
            try {
                DeepSeekClient.PostDraft draft = deepSeekClient.generatePostDraft(
                        title,
                        description,
                        category,
                        price,
                        condition,
                        location
                );
                if (requestId == aiRequestVersion) {
                    mainHandler.post(() -> {
                        cacheAiPostDraft(cacheKey, draft, category, price, condition, location);
                        applyAiPostDraft(draft);
                        setAiPostLoading(false);
                        A11y.toast(requireContext(), "AI \u5df2\u751f\u6210\u53d1\u5e03\u6587\u6848");
                    });
                }
            } catch (DeepSeekClient.DeepSeekException exception) {
                if (requestId == aiRequestVersion) {
                    mainHandler.post(() -> {
                        setAiPostLoading(false);
                        A11y.toast(requireContext(), "AI \u751f\u6210\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
                    });
                }
            }
        });
    }

    private void cacheAiPostDraft(String originalKey, DeepSeekClient.PostDraft draft, String category,
                                  String price, String condition, String location) {
        if (draft == null) {
            return;
        }
        aiPostDraftCache.put(originalKey, draft);
        aiPostDraftCache.put(
                buildPostDraftCacheKey(
                        draft.getTitle(),
                        draft.getDescription(),
                        category,
                        price,
                        condition,
                        location
                ),
                draft
        );
        if (aiPostDraftCache.size() > 8) {
            aiPostDraftCache.clear();
            aiPostDraftCache.put(originalKey, draft);
        }
    }

    private String buildPostDraftCacheKey(String title, String description, String category,
                                          String price, String condition, String location) {
        return safeKeyPart(title)
                + "|" + safeKeyPart(description)
                + "|" + safeKeyPart(category)
                + "|" + safeKeyPart(price)
                + "|" + safeKeyPart(condition)
                + "|" + safeKeyPart(location);
    }

    private String safeKeyPart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void applyAiPostDraft(DeepSeekClient.PostDraft draft) {
        if (draft == null) {
            return;
        }
        if (!TextUtils.isEmpty(draft.getTitle())) {
            etTitle.setText(draft.getTitle());
            etTitle.setSelection(etTitle.getText().length());
        }
        if (!TextUtils.isEmpty(draft.getDescription())) {
            etDescription.setText(draft.getDescription());
            etDescription.setSelection(etDescription.getText().length());
        }
        aiSuggestedTags = draft.getTags();
        if (tvAiPostTags != null) {
            if (TextUtils.isEmpty(aiSuggestedTags)) {
                tvAiPostTags.setVisibility(View.GONE);
                tvAiPostTags.setText("");
            } else {
                tvAiPostTags.setVisibility(View.VISIBLE);
                tvAiPostTags.setText("AI Tags: " + aiSuggestedTags);
            }
        }
    }

    private void setAiPostLoading(boolean loading) {
        aiPostLoading = loading;
        if (btnAiPostAssist == null) {
            return;
        }
        btnAiPostAssist.setEnabled(!loading);
        btnAiPostAssist.setAlpha(loading ? 0.62f : 1f);
        btnAiPostAssist.setText(loading ? "AI..." : "AI Generate");
    }

    private void generatePriceSuggestionWithAi() {
        if (aiPriceLoading) {
            return;
        }
        if (deepSeekClient == null || !deepSeekClient.isConfigured()) {
            A11y.toast(requireContext(), "\u8bf7\u5148\u5728 local.properties \u914d\u7f6e deepseek.apiKey\u3002");
            return;
        }

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(description)) {
            A11y.toast(requireContext(), "\u8bf7\u5148\u586b\u5199\u6807\u9898\u6216\u63cf\u8ff0\uff0cAI \u624d\u80fd\u4f30\u4ef7\u3002");
            return;
        }

        String category = spCategory.getSelectedItem() == null ? "" : spCategory.getSelectedItem().toString();
        String condition = etCondition.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String currentPrice = etPrice.getText().toString().trim();
        String cacheKey = buildPriceSuggestionCacheKey(title, description, category, condition, location, currentPrice);
        DeepSeekClient.PriceSuggestion cachedSuggestion = aiPriceSuggestionCache.get(cacheKey);
        if (cachedSuggestion != null) {
            applyAiPriceSuggestion(cachedSuggestion);
            A11y.toast(requireContext(), "\u5df2\u590d\u7528\u4e0a\u6b21 AI \u5b9a\u4ef7");
            return;
        }

        int requestId = ++aiPriceRequestVersion;
        setAiPriceLoading(true);
        aiExecutor.execute(() -> {
            try {
                DeepSeekClient.PriceSuggestion suggestion = deepSeekClient.generatePriceSuggestion(
                        title,
                        description,
                        category,
                        condition,
                        location,
                        currentPrice
                );
                if (requestId == aiPriceRequestVersion) {
                    mainHandler.post(() -> {
                        cacheAiPriceSuggestion(cacheKey, suggestion);
                        applyAiPriceSuggestion(suggestion);
                        setAiPriceLoading(false);
                        A11y.toast(requireContext(), "AI \u5df2\u751f\u6210\u5b9a\u4ef7\u5efa\u8bae");
                    });
                }
            } catch (DeepSeekClient.DeepSeekException exception) {
                if (requestId == aiPriceRequestVersion) {
                    mainHandler.post(() -> {
                        setAiPriceLoading(false);
                        A11y.toast(requireContext(), "AI \u5b9a\u4ef7\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
                    });
                }
            }
        });
    }

    private void cacheAiPriceSuggestion(String cacheKey, DeepSeekClient.PriceSuggestion suggestion) {
        if (suggestion == null) {
            return;
        }
        if (aiPriceSuggestionCache.size() > 8) {
            aiPriceSuggestionCache.clear();
        }
        aiPriceSuggestionCache.put(cacheKey, suggestion);
    }

    private String buildPriceSuggestionCacheKey(String title, String description, String category,
                                                String condition, String location, String currentPrice) {
        return safeKeyPart(title)
                + "|" + safeKeyPart(description)
                + "|" + safeKeyPart(category)
                + "|" + safeKeyPart(condition)
                + "|" + safeKeyPart(location)
                + "|" + safeKeyPart(currentPrice);
    }

    private void applyAiPriceSuggestion(DeepSeekClient.PriceSuggestion suggestion) {
        if (suggestion == null) {
            return;
        }
        if (TextUtils.isEmpty(etPrice.getText().toString().trim()) && suggestion.getSuggestedPrice() > 0d) {
            etPrice.setText(formatPriceValue(suggestion.getSuggestedPrice()));
            etPrice.setSelection(etPrice.getText().length());
        }
        if (tvAiPriceSuggestion != null) {
            String reason = TextUtils.isEmpty(suggestion.getReason())
                    ? "\u6839\u636e\u5546\u54c1\u4fe1\u606f\u7ed9\u51fa\u7684\u6821\u56ed\u4ea4\u6613\u4ef7\u3002"
                    : suggestion.getReason();
            tvAiPriceSuggestion.setVisibility(View.VISIBLE);
            tvAiPriceSuggestion.setText(
                    "\u5efa\u8bae \u00a5" + formatPriceValue(suggestion.getSuggestedPrice())
                            + " | \u533a\u95f4 \u00a5" + formatPriceValue(suggestion.getMinPrice())
                            + "-\u00a5" + formatPriceValue(suggestion.getMaxPrice())
                            + "\n" + reason
            );
        }
    }

    private void setAiPriceLoading(boolean loading) {
        aiPriceLoading = loading;
        if (btnAiPriceAssist == null) {
            return;
        }
        btnAiPriceAssist.setEnabled(!loading);
        btnAiPriceAssist.setAlpha(loading ? 0.62f : 1f);
        btnAiPriceAssist.setText(loading ? "AI..." : "AI \u5b9a\u4ef7");
    }

    private String formatPriceValue(double price) {
        long rounded = Math.round(price);
        if (Math.abs(price - rounded) < 0.01d) {
            return String.format(Locale.getDefault(), "%d", rounded);
        }
        return String.format(Locale.getDefault(), "%.2f", price);
    }

    private void setupHintBehavior(EditText editText) {
        CharSequence originalHint = editText.getHint();
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                editText.setHint("");
            } else if (TextUtils.isEmpty(editText.getText().toString().trim())) {
                editText.setHint(originalHint);
            }
        });
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void openAmapPlacePicker() {
        Intent intent = new Intent(requireContext(), AmapPlacePickerActivity.class);
        if (etLocation != null) {
            String currentLocation = etLocation.getText().toString().trim();
            if (!TextUtils.isEmpty(currentLocation)) {
                intent.putExtra(AmapPlacePickerActivity.EXTRA_INITIAL_QUERY, currentLocation);
            }
        }
        amapPlacePickerLauncher.launch(intent);
    }

    private void renderImagePreviewStrip() {
        bindPreview(framePreview1, ivPreview1, getPathAt(0));
        bindPreview(framePreview2, ivPreview2, getPathAt(1));
        bindPreview(framePreview3, ivPreview3, getPathAt(2));

        if (selectedImagePaths.size() > 3) {
            tvPreviewMore.setVisibility(View.VISIBLE);
            tvPreviewMore.setText("+" + (selectedImagePaths.size() - 3));
        } else {
            tvPreviewMore.setVisibility(View.GONE);
        }
    }

    private String getPathAt(int index) {
        if (index < 0 || index >= selectedImagePaths.size()) {
            return null;
        }
        return selectedImagePaths.get(index);
    }

    private void removeImageAt(int index) {
        if (index < 0 || index >= selectedImagePaths.size()) {
            return;
        }
        selectedImagePaths.remove(index);
        renderImagePreviewStrip();
        A11y.toast(requireContext(), "已移除 1 张图片。");
    }

    private void bindPreview(View frame, ImageView imageView, @Nullable String path) {
        if (TextUtils.isEmpty(path)) {
            frame.setVisibility(View.GONE);
            return;
        }
        frame.setVisibility(View.VISIBLE);
        RemoteImageLoader.load(imageView, path);
    }

    @Override
    public void onDestroy() {
        aiRequestVersion++;
        aiPriceRequestVersion++;
        aiExecutor.shutdownNow();
        super.onDestroy();
    }
}
