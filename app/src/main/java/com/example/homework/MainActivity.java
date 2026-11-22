package com.example.homework;

import com.example.homework.util.A11y;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.example.homework.data.CartRepository;
import com.example.homework.data.MessageRepository;
import com.example.homework.data.SessionManager;
import com.example.homework.ui.CartFragment;
import com.example.homework.ui.DiscoveryFragment;
import com.example.homework.ui.MessageFragment;
import com.example.homework.ui.PostFragment;
import com.example.homework.ui.ProfileFragment;

public class MainActivity extends AppCompatActivity implements MainNavigator {
    public static final String TAB_DISCOVERY = "discovery";
    public static final String TAB_MESSAGES = "messages";
    public static final String TAB_POST = "post";
    public static final String TAB_CART = "cart";
    public static final String TAB_PROFILE = "profile";
    public static final String EXTRA_INITIAL_TAB = "initial_tab";
    public static final String EXTRA_EDIT_ITEM_ID = "edit_item_id";
    private static final String STATE_ACTIVE_TAB = "state_active_tab";

    private ConstraintLayout mainRoot;
    private FrameLayout fragmentContainer;
    private ConstraintLayout navShell;
    private View navBar;
    private ConstraintLayout navDiscovery;
    private ConstraintLayout navMessages;
    private ConstraintLayout navPost;
    private ConstraintLayout navCart;
    private ConstraintLayout navProfile;
    private ImageView navDiscoveryIcon;
    private TextView navDiscoveryLabel;
    private ImageView navMessagesIcon;
    private TextView navMessagesLabel;
    private TextView navMessagesBadge;
    private ImageView navPostIcon;
    private TextView navPostLabel;
    private View navPostGlow;
    private ImageView navCartIcon;
    private TextView navCartLabel;
    private TextView navCartBadge;
    private ImageView navProfileIcon;
    private TextView navProfileLabel;
    private ImageView navBlurBackground;
    private SessionManager sessionManager;
    private CartRepository cartRepository;
    private MessageRepository messageRepository;
    private Bitmap navigationBlurBitmap;
    private Canvas navigationBlurCanvas;
    private final Runnable blurUpdater = this::refreshNavigationBlur;
    private String currentTab = TAB_DISCOVERY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        cartRepository = new CartRepository(this);
        messageRepository = new MessageRepository(this);

        bindViews();
        applySystemInsets();
        setupNavigation();
        mainRoot.post(this::scheduleNavigationBlur);

        if (savedInstanceState == null) {
            handleLaunchIntent(getIntent());
        } else {
            currentTab = savedInstanceState.getString(STATE_ACTIVE_TAB, TAB_DISCOVERY);
            updateNav(currentTab);
        }
        refreshNavigationIndicators();
    }

    private void bindViews() {
        mainRoot = findViewById(R.id.main_root);
        fragmentContainer = findViewById(R.id.fragment_container);
        navShell = findViewById(R.id.nav_shell);
        navBar = findViewById(R.id.nav_bar);
        navDiscovery = findViewById(R.id.nav_discovery);
        navMessages = findViewById(R.id.nav_messages);
        navPost = findViewById(R.id.nav_post);
        navCart = findViewById(R.id.nav_cart);
        navProfile = findViewById(R.id.nav_profile);
        navDiscoveryIcon = findViewById(R.id.nav_discovery_icon);
        navDiscoveryLabel = findViewById(R.id.nav_discovery_label);
        navMessagesIcon = findViewById(R.id.nav_messages_icon);
        navMessagesLabel = findViewById(R.id.nav_messages_label);
        navMessagesBadge = findViewById(R.id.nav_messages_badge);
        navPostIcon = findViewById(R.id.nav_post_icon);
        navPostLabel = findViewById(R.id.nav_post_label);
        navPostGlow = findViewById(R.id.nav_post_glow);
        navCartIcon = findViewById(R.id.nav_cart_icon);
        navCartLabel = findViewById(R.id.nav_cart_label);
        navCartBadge = findViewById(R.id.nav_cart_badge);
        navProfileIcon = findViewById(R.id.nav_profile_icon);
        navProfileLabel = findViewById(R.id.nav_profile_label);
        navBlurBackground = findViewById(R.id.nav_blur_background);
    }

    private void applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ConstraintLayout.LayoutParams navLayoutParams =
                    (ConstraintLayout.LayoutParams) navShell.getLayoutParams();
            navLayoutParams.bottomMargin = dp(24) + bars.bottom;
            navShell.setLayoutParams(navLayoutParams);

            fragmentContainer.setPadding(
                    fragmentContainer.getPaddingLeft(),
                    fragmentContainer.getPaddingTop(),
                    fragmentContainer.getPaddingRight(),
                    dp(130) + bars.bottom
            );
            return insets;
        });
    }

    private void setupNavigation() {
        // 底部导航是 ConstraintLayout 容器：声明为按钮角色并给出可朗读标签，
        // 同时保证键盘 / 方向键可聚焦（XML 中已声明 focusable="true"）。
        A11y.asButton(navDiscovery, getString(R.string.cd_nav_discovery));
        A11y.asButton(navMessages, getString(R.string.cd_nav_messages));
        A11y.asButton(navPost, getString(R.string.cd_nav_post));
        A11y.asButton(navCart, getString(R.string.cd_nav_cart));
        A11y.asButton(navProfile, getString(R.string.cd_nav_profile));
        navDiscovery.setOnClickListener(v -> showTab(TAB_DISCOVERY, -1));
        navMessages.setOnClickListener(v -> showTab(TAB_MESSAGES, -1));
        navPost.setOnClickListener(v -> showTab(TAB_POST, -1));
        navCart.setOnClickListener(v -> showTab(TAB_CART, -1));
        navProfile.setOnClickListener(v -> showTab(TAB_PROFILE, -1));
    }

    private void showTab(String tab, long editItemId) {
        currentTab = tab;
        updateNav(tab);
        // 页面切换后主动播报当前页，避免视障用户不知道界面已变化
        A11y.announce(this, getString(resolveTabAnnouncement(tab)));
        refreshNavigationIndicators();
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tab);
        if (fragment == null) {
            fragment = createFragment(tab, editItemId);
            transaction.add(R.id.fragment_container, fragment, tab);
        } else if (TAB_POST.equals(tab) && editItemId > 0 && fragment instanceof PostFragment) {
            ((PostFragment) fragment).showEditorForItem(editItemId);
        }

        boolean targetHandled = false;
        for (Fragment existingFragment : getSupportFragmentManager().getFragments()) {
            if (existingFragment == null || !existingFragment.isAdded()) {
                continue;
            }

            if (existingFragment == fragment) {
                targetHandled = true;
                transaction.show(existingFragment);
                transaction.setMaxLifecycle(existingFragment, Lifecycle.State.RESUMED);
            } else {
                transaction.hide(existingFragment);
                transaction.setMaxLifecycle(existingFragment, Lifecycle.State.STARTED);
            }
        }

        if (!targetHandled) {
            transaction.show(fragment);
            transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED);
        }

        transaction.setPrimaryNavigationFragment(fragment);
        transaction.runOnCommit(() -> mainRoot.postDelayed(this::scheduleNavigationBlur, 48L));
        transaction.commit();
    }

    private int resolveTabAnnouncement(String tab) {
        if (TAB_MESSAGES.equals(tab)) {
            return R.string.announce_tab_messages;
        }
        if (TAB_POST.equals(tab)) {
            return R.string.announce_tab_post;
        }
        if (TAB_CART.equals(tab)) {
            return R.string.announce_tab_cart;
        }
        if (TAB_PROFILE.equals(tab)) {
            return R.string.announce_tab_profile;
        }
        return R.string.announce_tab_discovery;
    }

    private Fragment createFragment(String tab, long editItemId) {
        if (TAB_MESSAGES.equals(tab)) {
            return new MessageFragment();
        }
        if (TAB_POST.equals(tab)) {
            return PostFragment.newInstance(editItemId);
        }
        if (TAB_CART.equals(tab)) {
            return new CartFragment();
        }
        if (TAB_PROFILE.equals(tab)) {
            return new ProfileFragment();
        }
        return new DiscoveryFragment();
    }

    private void handleLaunchIntent(Intent intent) {
        String initialTab = resolveInitialTab(intent);
        long editItemId = intent != null ? intent.getLongExtra(EXTRA_EDIT_ITEM_ID, -1L) : -1L;
        showTab(initialTab, editItemId);
    }

    private String resolveInitialTab(Intent intent) {
        if (intent == null) {
            return TAB_DISCOVERY;
        }
        String initialTab = intent.getStringExtra(EXTRA_INITIAL_TAB);
        if (TAB_POST.equals(initialTab)
                || TAB_MESSAGES.equals(initialTab)
                || TAB_CART.equals(initialTab)
                || TAB_PROFILE.equals(initialTab)) {
            return initialTab;
        }
        return TAB_DISCOVERY;
    }

    private void updateNav(String tab) {
        setNavSelected(navDiscovery, navDiscoveryIcon, navDiscoveryLabel, TAB_DISCOVERY.equals(tab));
        setNavSelected(navMessages, navMessagesIcon, navMessagesLabel, TAB_MESSAGES.equals(tab));
        setPostNavSelected(TAB_POST.equals(tab));
        setNavSelected(navCart, navCartIcon, navCartLabel, TAB_CART.equals(tab));
        setNavSelected(navProfile, navProfileIcon, navProfileLabel, TAB_PROFILE.equals(tab));
    }

    private void setNavSelected(View root, ImageView icon, TextView label, boolean selected) {
        // View 的 selected 状态会写入无障碍节点，TalkBack 朗读「已选中 / 未选中」
        root.setSelected(selected);
        root.setBackgroundResource(selected ? R.drawable.bg_nav_glass_active : android.R.color.transparent);
        int color = getColor(selected ? R.color.primary_blue : R.color.text_secondary);
        icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        label.setTextColor(color);
        root.animate()
                .scaleX(selected ? 1f : 0.96f)
                .scaleY(selected ? 1f : 0.96f)
                .alpha(selected ? 1f : 0.88f)
                .setDuration(180L)
                .start();
    }

    private void setPostNavSelected(boolean selected) {
        navPost.setSelected(selected);
        navPost.setBackgroundResource(android.R.color.transparent);
        navPostIcon.setBackgroundResource(R.drawable.bg_nav_post_glass);
        navPostIcon.setColorFilter(getColor(selected ? R.color.primary_blue : R.color.text_primary), PorterDuff.Mode.SRC_IN);
        navPostLabel.setVisibility(View.GONE);
        navPostGlow.animate().alpha(selected ? 1f : 0.78f).setDuration(180L).start();
        navPost.animate()
                .scaleX(selected ? 1.04f : 1f)
                .scaleY(selected ? 1.04f : 1f)
                .setDuration(180L)
                .start();
        navPostIcon.animate()
                .translationY(selected ? -dpFloat(26) : -dpFloat(22))
                .setDuration(180L)
                .start();
        navPostGlow.animate()
                .translationY(selected ? -dpFloat(26) : -dpFloat(22))
                .setDuration(180L)
                .start();
    }

    private void scheduleNavigationBlur() {
        navBlurBackground.removeCallbacks(blurUpdater);
        navBlurBackground.post(blurUpdater);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNavigationIndicators();
    }

    private void refreshNavigationBlur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            clearNavigationBlur();
            return;
        }

        int width = navBar.getWidth();
        int height = navBar.getHeight();
        if (width <= 0 || height <= 0 || fragmentContainer.getWidth() <= 0 || fragmentContainer.getHeight() <= 0) {
            return;
        }

        ensureNavigationBlurBuffer(width, height);
        if (navigationBlurBitmap == null || navigationBlurCanvas == null) {
            return;
        }

        navigationBlurBitmap.eraseColor(0);
        int[] navLocation = new int[2];
        int[] contentLocation = new int[2];
        navBar.getLocationOnScreen(navLocation);
        fragmentContainer.getLocationOnScreen(contentLocation);

        navigationBlurCanvas.save();
        navigationBlurCanvas.translate(contentLocation[0] - navLocation[0], contentLocation[1] - navLocation[1]);
        fragmentContainer.draw(navigationBlurCanvas);
        navigationBlurCanvas.restore();

        navBlurBackground.setImageBitmap(navigationBlurBitmap);
        navBlurBackground.setRenderEffect(RenderEffect.createBlurEffect(36f, 36f, Shader.TileMode.CLAMP));
    }

    private void ensureNavigationBlurBuffer(int width, int height) {
        if (navigationBlurBitmap != null
                && navigationBlurBitmap.getWidth() == width
                && navigationBlurBitmap.getHeight() == height) {
            return;
        }

        releaseNavigationBlurBuffer();
        navigationBlurBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        navigationBlurCanvas = new Canvas(navigationBlurBitmap);
    }

    private void clearNavigationBlur() {
        if (navBlurBackground == null) {
            releaseNavigationBlurBuffer();
            return;
        }
        navBlurBackground.setImageDrawable(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            navBlurBackground.setRenderEffect(null);
        }
        releaseNavigationBlurBuffer();
    }

    private void releaseNavigationBlurBuffer() {
        if (navigationBlurBitmap != null && !navigationBlurBitmap.isRecycled()) {
            navigationBlurBitmap.recycle();
        }
        navigationBlurBitmap = null;
        navigationBlurCanvas = null;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private float dpFloat(int value) {
        return getResources().getDisplayMetrics().density * value;
    }

    @Override
    public void openItemDetail(long itemId) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra(ItemDetailActivity.EXTRA_ITEM_ID, itemId);
        startActivity(intent);
    }

    @Override
    public void openPostEditor(long itemId) {
        showTab(TAB_POST, itemId);
    }

    @Override
    public void openProfile() {
        showTab(TAB_PROFILE, -1);
    }

    @Override
    public void openDiscovery() {
        showTab(TAB_DISCOVERY, -1);
    }

    @Override
    public void refreshNavigationIndicators() {
        long currentUserId = sessionManager != null ? sessionManager.getLoggedInUserId() : -1L;
        int unreadMessageCount = messageRepository != null
                ? messageRepository.getUnreadMessageCount(currentUserId)
                : 0;
        int cartItemCount = cartRepository != null
                ? cartRepository.getCartItemCount(currentUserId)
                : 0;

        updateBadge(navMessagesBadge, unreadMessageCount, R.string.cd_badge_unread);
        updateBadge(navCartBadge, cartItemCount, R.string.cd_badge_cart);
    }

    private void updateBadge(TextView badgeView, int count, int descriptionTemplate) {
        if (badgeView == null) {
            return;
        }
        if (count <= 0) {
            badgeView.setVisibility(View.GONE);
            badgeView.setContentDescription(null);
            return;
        }
        badgeView.setVisibility(View.VISIBLE);
        badgeView.setText(formatBadgeCount(count));
        // 「3」改为「3 条未读消息」/「购物车中有 2 件商品」
        A11y.label(badgeView, getString(descriptionTemplate, count));
    }

    private String formatBadgeCount(int count) {
        return count > 99 ? "99+" : String.valueOf(count);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunchIntent(intent);
        refreshNavigationIndicators();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ACTIVE_TAB, currentTab);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            scheduleNavigationBlur();
        }
    }

    @Override
    protected void onDestroy() {
        if (navBlurBackground != null) {
            navBlurBackground.removeCallbacks(blurUpdater);
        }
        clearNavigationBlur();
        super.onDestroy();
    }
}
