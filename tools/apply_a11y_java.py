#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
揭榜任务二「视障友好适配」Java 运行时适配补丁（改造记录，可复现、可审阅）。

XML 只能表达静态语义；以下内容必须在运行时完成：
  - 底部导航 / 分类格子 / 搜索框 / 筛选 Tab：声明为 Button 角色 + 选中状态；
  - 页面切换、角标数量、列表图片、文字头像：动态语义描述；
  - 操作结果：由 apply_a11y_patch.py 已完成 Toast -> A11y.toast 替换。

用法：python tools/apply_a11y_java.py           # 只应运行一次（幂等）
      python tools/apply_a11y_java.py --dry-run
"""

import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, "app", "src", "main", "java")
PKG = "com/example/homework"
IMPORT_LINE = "import com.example.homework.util.A11y;"

# (相对路径, 原文, 替换后文本, 说明)
PATCHES = [
    # ------------------------------------------------------------ MainActivity
    (PKG + "/MainActivity.java",
     "    private void setupNavigation() {\n"
     "        navDiscovery.setOnClickListener(v -> showTab(TAB_DISCOVERY, -1));\n"
     "        navMessages.setOnClickListener(v -> showTab(TAB_MESSAGES, -1));\n"
     "        navPost.setOnClickListener(v -> showTab(TAB_POST, -1));\n"
     "        navCart.setOnClickListener(v -> showTab(TAB_CART, -1));\n"
     "        navProfile.setOnClickListener(v -> showTab(TAB_PROFILE, -1));\n"
     "    }",
     "    private void setupNavigation() {\n"
     "        // 底部导航是 ConstraintLayout 容器：声明为按钮角色并给出可朗读标签，\n"
     "        // 同时保证键盘 / 方向键可聚焦（XML 中已声明 focusable=\"true\"）。\n"
     "        A11y.asButton(navDiscovery, getString(R.string.cd_nav_discovery));\n"
     "        A11y.asButton(navMessages, getString(R.string.cd_nav_messages));\n"
     "        A11y.asButton(navPost, getString(R.string.cd_nav_post));\n"
     "        A11y.asButton(navCart, getString(R.string.cd_nav_cart));\n"
     "        A11y.asButton(navProfile, getString(R.string.cd_nav_profile));\n"
     "        navDiscovery.setOnClickListener(v -> showTab(TAB_DISCOVERY, -1));\n"
     "        navMessages.setOnClickListener(v -> showTab(TAB_MESSAGES, -1));\n"
     "        navPost.setOnClickListener(v -> showTab(TAB_POST, -1));\n"
     "        navCart.setOnClickListener(v -> showTab(TAB_CART, -1));\n"
     "        navProfile.setOnClickListener(v -> showTab(TAB_PROFILE, -1));\n"
     "    }",
     "底部导航按钮角色与语义标签"),

    (PKG + "/MainActivity.java",
     "    private void showTab(String tab, long editItemId) {\n"
     "        currentTab = tab;\n"
     "        updateNav(tab);\n"
     "        refreshNavigationIndicators();",
     "    private void showTab(String tab, long editItemId) {\n"
     "        currentTab = tab;\n"
     "        updateNav(tab);\n"
     "        // 页面切换后主动播报当前页，避免视障用户不知道界面已变化\n"
     "        A11y.announce(this, getString(resolveTabAnnouncement(tab)));\n"
     "        refreshNavigationIndicators();",
     "Fragment 切换播报"),

    (PKG + "/MainActivity.java",
     "    private Fragment createFragment(String tab, long editItemId) {",
     "    private int resolveTabAnnouncement(String tab) {\n"
     "        if (TAB_MESSAGES.equals(tab)) {\n"
     "            return R.string.announce_tab_messages;\n"
     "        }\n"
     "        if (TAB_POST.equals(tab)) {\n"
     "            return R.string.announce_tab_post;\n"
     "        }\n"
     "        if (TAB_CART.equals(tab)) {\n"
     "            return R.string.announce_tab_cart;\n"
     "        }\n"
     "        if (TAB_PROFILE.equals(tab)) {\n"
     "            return R.string.announce_tab_profile;\n"
     "        }\n"
     "        return R.string.announce_tab_discovery;\n"
     "    }\n\n"
     "    private Fragment createFragment(String tab, long editItemId) {",
     "页面播报文案映射"),

    (PKG + "/MainActivity.java",
     "    private void setNavSelected(View root, ImageView icon, TextView label, boolean selected) {\n"
     "        root.setBackgroundResource(",
     "    private void setNavSelected(View root, ImageView icon, TextView label, boolean selected) {\n"
     "        // View 的 selected 状态会写入无障碍节点，TalkBack 朗读「已选中 / 未选中」\n"
     "        root.setSelected(selected);\n"
     "        root.setBackgroundResource(",
     "导航项选中状态"),

    (PKG + "/MainActivity.java",
     "    private void setPostNavSelected(boolean selected) {\n"
     "        navPost.setBackgroundResource(",
     "    private void setPostNavSelected(boolean selected) {\n"
     "        navPost.setSelected(selected);\n"
     "        navPost.setBackgroundResource(",
     "发布项选中状态"),

    (PKG + "/MainActivity.java",
     "        updateBadge(navMessagesBadge, unreadMessageCount);\n"
     "        updateBadge(navCartBadge, cartItemCount);",
     "        updateBadge(navMessagesBadge, unreadMessageCount, R.string.cd_badge_unread);\n"
     "        updateBadge(navCartBadge, cartItemCount, R.string.cd_badge_cart);",
     "角标语义模板"),

    (PKG + "/MainActivity.java",
     "    private void updateBadge(TextView badgeView, int count) {\n"
     "        if (badgeView == null) {\n"
     "            return;\n"
     "        }\n"
     "        if (count <= 0) {\n"
     "            badgeView.setVisibility(View.GONE);\n"
     "            return;\n"
     "        }\n"
     "        badgeView.setVisibility(View.VISIBLE);\n"
     "        badgeView.setText(formatBadgeCount(count));\n"
     "    }",
     "    private void updateBadge(TextView badgeView, int count, int descriptionTemplate) {\n"
     "        if (badgeView == null) {\n"
     "            return;\n"
     "        }\n"
     "        if (count <= 0) {\n"
     "            badgeView.setVisibility(View.GONE);\n"
     "            badgeView.setContentDescription(null);\n"
     "            return;\n"
     "        }\n"
     "        badgeView.setVisibility(View.VISIBLE);\n"
     "        badgeView.setText(formatBadgeCount(count));\n"
     "        // 「3」改为「3 条未读消息」/「购物车中有 2 件商品」\n"
     "        A11y.label(badgeView, getString(descriptionTemplate, count));\n"
     "    }",
     "角标动态语义"),

    # ------------------------------------------------------- DiscoveryFragment
    (PKG + "/ui/DiscoveryFragment.java",
     "        View searchBox = root.findViewById(R.id.layout_search_box);\n"
     "        searchBox.setOnClickListener(v -> focusSearchField());",
     "        View searchBox = root.findViewById(R.id.layout_search_box);\n"
     "        A11y.asButton(searchBox, getString(R.string.cd_search_box));\n"
     "        searchBox.setOnClickListener(v -> focusSearchField());",
     "搜索框按钮角色"),

    (PKG + "/ui/DiscoveryFragment.java",
     "    private void setupFeedTabs(View root) {\n"
     "        root.findViewById(R.id.tab_follow).setOnClickListener(v -> switchFeedMode(DiscoveryFeedMode.FOLLOW));\n"
     "        tvTabRecommend.setOnClickListener(v -> switchFeedMode(DiscoveryFeedMode.RECOMMEND));\n"
     "        tvTabLatest.setOnClickListener(v -> switchFeedMode(DiscoveryFeedMode.LATEST));\n"
     "        updateFeedTabSelection();\n"
     "    }",
     "    private void setupFeedTabs(View root) {\n"
     "        View tabFollow = root.findViewById(R.id.tab_follow);\n"
     "        A11y.asButton(tabFollow, getString(R.string.cd_tab_follow));\n"
     "        A11y.asButton(tvTabRecommend, getString(R.string.cd_tab_recommend));\n"
     "        A11y.asButton(tvTabLatest, getString(R.string.cd_tab_latest));\n"
     "        tabFollow.setOnClickListener(v -> switchFeedMode(DiscoveryFeedMode.FOLLOW));\n"
     "        tvTabRecommend.setOnClickListener(v -> switchFeedMode(DiscoveryFeedMode.RECOMMEND));\n"
     "        tvTabLatest.setOnClickListener(v -> switchFeedMode(DiscoveryFeedMode.LATEST));\n"
     "        updateFeedTabSelection();\n"
     "    }",
     "信息流 Tab 按钮角色"),

    (PKG + "/ui/DiscoveryFragment.java",
     "        styleFeedTab(tvTabLatest, latestSelected);\n"
     "    }",
     "        styleFeedTab(tvTabLatest, latestSelected);\n"
     "        // 让 TalkBack 朗读当前选中的信息流\n"
     "        View followTab = getView() == null ? null : getView().findViewById(R.id.tab_follow);\n"
     "        if (followTab != null) {\n"
     "            followTab.setSelected(followSelected);\n"
     "        }\n"
     "        tvTabRecommend.setSelected(recommendSelected);\n"
     "        tvTabLatest.setSelected(latestSelected);\n"
     "    }",
     "信息流 Tab 选中状态"),

    (PKG + "/ui/DiscoveryFragment.java",
     "    private void bindCategoryTile(View root, int tileId, String category) {\n"
     "        View tile = root.findViewById(tileId);\n"
     "        tile.setTag(category);",
     "    private void bindCategoryTile(View root, int tileId, String category) {\n"
     "        View tile = root.findViewById(tileId);\n"
     "        // 分类格子是 LinearLayout + 图标 + 文字，需合并为一个「按钮」节点朗读\n"
     "        A11y.asButton(tile, getString(R.string.cd_category_tile, category));\n"
     "        tile.setTag(category);",
     "分类格子动态语义"),

    (PKG + "/ui/DiscoveryFragment.java",
     "            tile.setAlpha(active ? 1f : 0.92f);\n"
     "            tile.setScaleX(active ? 1.06f : 1f);",
     "            tile.setAlpha(active ? 1f : 0.92f);\n"
     "            tile.setSelected(active);\n"
     "            tile.setScaleX(active ? 1.06f : 1f);",
     "分类格子选中状态"),

    # ------------------------------------------------------------- Adapters
    (PKG + "/ui/DiscoveryItemAdapter.java",
     "            titleView.setText(item.getTitle());\n"
     "            priceView.setText(DiscoveryUiMapper.formatPrice(item.getPrice()));",
     "            titleView.setText(item.getTitle());\n"
     "            // 商品图片朗读具体商品名；文字头像为装饰，不再朗读单字\n"
     "            A11y.itemImage(imageView, imageView.getContext()\n"
     "                    .getString(R.string.cd_item_image_with_name, item.getTitle()));\n"
     "            A11y.decorative(avatarView);\n"
     "            priceView.setText(DiscoveryUiMapper.formatPrice(item.getPrice()));",
     "发现页列表动态语义"),

    (PKG + "/ui/CartListAdapter.java",
     "            RemoteImageLoader.load(imageView, item.getImageUrl());\n"
     "            titleView.setText(item.getTitle());",
     "            RemoteImageLoader.load(imageView, item.getImageUrl());\n"
     "            A11y.itemImage(imageView, imageView.getContext()\n"
     "                    .getString(R.string.cd_item_image_with_name, item.getTitle()));\n"
     "            // 复选框原本只会朗读「未勾选」，补充所属商品名\n"
     "            A11y.label(checkBox, checkBox.getContext()\n"
     "                    .getString(R.string.cd_cart_item_checkbox, item.getTitle()));\n"
     "            titleView.setText(item.getTitle());",
     "购物车列表动态语义"),

    (PKG + "/ui/MyListingAdapter.java",
     "            RemoteImageLoader.load(imageView, item.getImageUrl());\n"
     "\n"
     "            itemView.setOnClickListener(v -> listener.onOpen(item));",
     "            RemoteImageLoader.load(imageView, item.getImageUrl());\n"
     "            A11y.itemImage(imageView, imageView.getContext()\n"
     "                    .getString(R.string.cd_item_image_with_name, item.getTitle()));\n"
     "\n"
     "            itemView.setOnClickListener(v -> listener.onOpen(item));",
     "我的发布列表动态语义"),

    (PKG + "/ui/OrderListAdapter.java",
     "            bindThumb(order);\n"
     "            applyStatusStyle(order.getStatus());",
     "            bindThumb(order);\n"
     "            A11y.itemImage(thumbImageView, thumbImageView.getContext()\n"
     "                    .getString(R.string.cd_item_image_with_name, order.getItemTitle()));\n"
     "            applyStatusStyle(order.getStatus());",
     "订单列表动态语义"),

    (PKG + "/ui/ConversationAdapter.java",
     "            timeView.setText(relativeTime);\n"
     "\n"
     "            if (!TextUtils.isEmpty(conversation.getItemImageUrl())) {",
     "            timeView.setText(relativeTime);\n"
     "            // 会话头像由姓名文本替代；商品缩略图朗读商品名；未读角标朗读「N 条未读消息」\n"
     "            A11y.decorative(avatarTextView);\n"
     "            A11y.itemImage(itemImageView, itemImageView.getContext()\n"
     "                    .getString(R.string.cd_item_image_with_name, conversation.getItemTitle()));\n"
     "            A11y.label(unreadBadgeView, unreadBadgeView.getContext()\n"
     "                    .getString(R.string.cd_unread_badge, conversation.getUnreadCount()));\n"
     "\n"
     "            if (!TextUtils.isEmpty(conversation.getItemImageUrl())) {",
     "会话列表动态语义"),
]


def ensure_import(text):
    if IMPORT_LINE in text:
        return text, 0
    return re.sub(r'(package\s+[\w.]+\s*;\s*\n)',
                  r'\1' + IMPORT_LINE + '\n', text, count=1), 1


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    cache = {}       # path -> 当前累积文本（同一文件的多处补丁必须叠加）
    applied = 0
    for rel, old, new, desc in PATCHES:
        path = os.path.join(JAVA, rel)
        if not os.path.isfile(path):
            print("  [缺失文件] %s (%s)" % (rel, desc))
            continue
        text = cache.get(path)
        if text is None:
            text = open(path, encoding="utf-8").read()
        if new in text:
            print("  [已应用] %-42s %s" % (os.path.basename(rel), desc))
            continue
        n = text.count(old)
        if n != 1:
            print("  [未命中=%d] %-38s %s" % (n, os.path.basename(rel), desc))
            continue
        text = text.replace(old, new, 1)
        cache[path] = text
        applied += 1
        print("  [应用]   %-42s %s" % (os.path.basename(rel), desc))

    import_count = 0
    for path in list(cache):
        text, added = ensure_import(cache[path])
        cache[path] = text
        import_count += added

    if not args.dry_run:
        for path, text in cache.items():
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(text)

    print("\n共应用 %d 处补丁，涉及 %d 个 Java 文件，新增 import %d 处%s" % (
        applied, len(cache), import_count,
        "（dry-run）" if args.dry_run else ""))

    # 校验：所有目标文件都含 A11y import（A11y.java 自身除外）
    missing = []
    for rel in sorted(set(p[0] for p in PATCHES)):
        path = os.path.join(JAVA, rel)
        if os.path.isfile(path) and IMPORT_LINE not in open(path, encoding="utf-8").read():
            missing.append(rel)
    print("校验：缺少 A11y import 的目标文件 %d 个" % len(missing))
    for m in missing:
        print("  " + m)
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
