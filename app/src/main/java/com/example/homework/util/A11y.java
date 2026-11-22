package com.example.homework.util;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/**
 * 视障友好适配工具类（揭榜任务二）。
 *
 * <p>本类只提供无障碍语义与播报能力，不包含任何业务逻辑，也不新增功能：
 * <ul>
 *   <li>把「用 TextView / 容器 + setOnClickListener 模拟按钮」的控件，
 *       在无障碍节点上声明为 Button 角色，使 TalkBack 朗读为「xxx，按钮」，
 *       并让键盘 / 方向键可以聚焦与激活；</li>
 *   <li>把仅用于装饰的图片标记为对无障碍不重要，避免无意义朗读；</li>
 *   <li>为操作结果（原 Toast 提示）同步发送无障碍播报事件，
 *       使视障用户能感知成功 / 失败 / 校验错误；</li>
 *   <li>为列表图片提供动态语义描述、为页面标题声明 heading、为动态区域声明 liveRegion。</li>
 * </ul>
 *
 * <p>参考：Android Developers - Make apps more accessible / AccessibilityNodeInfo /
 * TalkBack 与键盘导航规范。
 */
public final class A11y {

    private A11y() {
    }

    /** 通过无障碍服务播报一段文字（适用于只有 Context、拿不到具体 View 的场景）。 */
    public static void announce(@NonNull Context context, @Nullable CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        AccessibilityManager manager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null || !manager.isEnabled()) {
            return;
        }
        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        event.setPackageName(context.getPackageName());
        event.getText().add(text);
        manager.sendAccessibilityEvent(event);
    }

    /** 通过指定 View 播报，优先使用系统 View 的 announceForAccessibility。 */
    public static void announce(@NonNull View view, @Nullable CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        view.announceForAccessibility(text);
    }

    /** 显示 Toast 的同时向无障碍服务播报，保证操作结果可被 TalkBack 感知。 */
    public static void toast(@NonNull Context context, @Nullable CharSequence text) {
        android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show();
        announce(context, text);
    }

    public static void toast(@NonNull Context context, int resId, Object... args) {
        toast(context, context.getString(resId, args));
    }

    /**
     * 设置控件的无障碍标签（朗读内容）。
     * 有标签时确保节点重新进入无障碍树（用于 XML 中先标记为装饰性、
     * 运行时由 Adapter / 角标逻辑动态填充语义的控件）；无标签时反之。
     */
    public static void label(@NonNull View view, @Nullable CharSequence text) {
        view.setContentDescription(text);
        ViewCompat.setImportantForAccessibility(
                view,
                TextUtils.isEmpty(text)
                        ? ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
                        : ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /** 把纯装饰性视图从无障碍树中排除，避免 TalkBack 朗读无意义内容。 */
    public static void decorative(@NonNull View... views) {
        for (View view : views) {
            if (view == null) {
                continue;
            }
            ViewCompat.setImportantForAccessibility(
                    view, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    /** 声明为标题，TalkBack 支持「按标题导航」。 */
    public static void heading(@NonNull View view) {
        ViewCompat.setAccessibilityHeading(view, true);
    }

    /** 声明为 polite 实时区域：内容变化时由 TalkBack 自动播报，不打断当前朗读。 */
    public static void liveRegion(@NonNull View view) {
        ViewCompat.setAccessibilityLiveRegion(
                view, View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }

    /**
     * 把任意可点击视图（TextView、ImageView、容器等）在无障碍语义上声明为「按钮」。
     * 同时保证键盘 / 方向键可聚焦，并给出可朗读标签。
     */
    public static void asButton(@NonNull final View view, @Nullable final CharSequence label) {
        view.setFocusable(true);
        view.setClickable(true);
        if (!TextUtils.isEmpty(label)) {
            view.setContentDescription(label);
        }
        ViewCompat.setAccessibilityDelegate(view, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                    @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
                info.setClickable(true);
                info.setFocusable(true);
                if (!TextUtils.isEmpty(label)) {
                    info.setContentDescription(label);
                }
            }
        });
    }

    /**
     * 声明为「可切换选中状态的标签 / 导航项」，TalkBack 会朗读「已选中 / 未选中」。
     * 用于底部导航、筛选 Tab、排序 Tab 等单选组。
     */
    public static void asTab(@NonNull final View view,
                             @Nullable final CharSequence label,
                             final boolean selected) {
        asButton(view, label);
        view.setSelected(selected);
        ViewCompat.setAccessibilityDelegate(view, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                    @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
                info.setClickable(true);
                info.setFocusable(true);
                info.setSelected(selected);
                if (!TextUtils.isEmpty(label)) {
                    info.setContentDescription(label);
                }
            }
        });
    }

    /**
     * 为列表中的商品图片设置动态语义描述。
     * 有名称时朗读「xxx 的商品图片」，无图 / 占位时标记为装饰性，避免朗读「图片」。
     */
    public static void itemImage(@NonNull View image, @Nullable CharSequence itemName) {
        if (TextUtils.isEmpty(itemName)) {
            image.setContentDescription(null);
            ViewCompat.setImportantForAccessibility(
                    image, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
        } else {
            ViewCompat.setImportantForAccessibility(
                    image, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            image.setContentDescription(itemName);
        }
    }

    /** 从 Activity 获取可用于播报的根 View。 */
    @Nullable
    public static View rootView(@Nullable Context context) {
        while (context instanceof android.content.ContextWrapper) {
            if (context instanceof Activity) {
                return ((Activity) context).getWindow().getDecorView();
            }
            context = ((android.content.ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
