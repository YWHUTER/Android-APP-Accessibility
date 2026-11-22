#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
揭榜任务二「视障友好适配」一次性改造脚本（改造记录，可复现、可审阅）。

职责：
  1. 为布局中所有图片类控件补 contentDescription 或标记为装饰性元素；
  2. 把「符号文字按钮」（返 / ← / ? / ⚙ / × / 刷 …）补上明确的中文语义标签；
  3. 把 Java 中 setOnClickListener 绑定、但 XML 未声明可聚焦的控件补 focusable；
  4. 把小于 48dp 的固定高度交互控件提升到 48dp（触摸目标）；
  5. 为所有 EditText 建立显式 nextFocusForward 键盘导航链；
  6. 为标题声明 accessibilityHeading、为空状态 / 状态文本声明 liveRegion；
  7. 把 Java 中的 Toast 提示统一替换为 A11y.toast（Toast + 无障碍播报）。

用法：python tools/apply_a11y_patch.py        # 在项目根目录执行，只应运行一次
      python tools/apply_a11y_patch.py --dry-run
"""

import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LAYOUT = os.path.join(ROOT, "app", "src", "main", "res", "layout")
JAVA = os.path.join(ROOT, "app", "src", "main", "java")

# ---------------------------------------------------------------- 语义映射表
# 图片 / 图标：明确的语义标签
CD_MAP = {
    # 返回与通用按钮
    "btn_back": "cd_back",
    "btn_place_back": "cd_back",
    "btn_chat_back": "cd_back",
    "btn_community_back": "cd_back",
    "btn_edit_profile_back": "cd_back",
    "btn_settings_back": "cd_back",
    "btn_profile_help": "cd_back",
    "btn_chat_more": "cd_more_chat",
    "btn_detail_help": "cd_help_detail",
    "btn_profile_settings": "cd_settings",
    "btn_close_pro_tip": "cd_close",
    "btn_scan": "cd_scan",
    "btn_remove_preview_1": "cd_remove_preview_1",
    "btn_remove_preview_2": "cd_remove_preview_2",
    "btn_remove_preview_3": "cd_remove_preview_3",
    "iv_search_camera": "cd_camera_publish",
    "fab_new_message": "cd_new_message",
    # 底部导航
    "nav_discovery": "cd_nav_discovery",
    "nav_messages": "cd_nav_messages",
    "nav_post": "cd_nav_post",
    "nav_cart": "cd_nav_cart",
    "nav_profile": "cd_nav_profile",
    # 可点击容器
    "layout_search_box": "cd_search_box",
    "card_purchases": "cd_card_purchases",
    "card_sales": "cd_card_sales",
    "layout_upload_photos": "cd_upload_image",
    "layout_unread_summary": "cd_row_community",
    "row_account_listings": "cd_row_my_listings",
    "row_account_community": "cd_row_community",
    "row_account_settings": "cd_row_settings",
    "tab_follow": "cd_tab_follow",
    # 表单控件
    "switch_pickup": "cd_switch_pickup",
    "sp_major": "cd_spinner_major",
    "sp_edit_major": "cd_spinner_edit_major",
    "sp_category": "cd_spinner_category",
    "sp_post_preferred_major": "cd_spinner_preferred_major",
    "btn_ai_post_assist": "cd_ai_post_assist",
    "btn_ai_price_assist": "cd_ai_price_assist",
    "btn_pick_location_amap": "cd_pick_location",
    "btn_save_item": "cd_save_post",
    # 商品 / 配图（Adapter 会在运行时覆盖为具体名称）
    "iv_detail_image": "cd_detail_image",
    "iv_item_image": "cd_item_image",
    "iv_cart_item_image": "cd_cart_item_image",
    "iv_my_item_image": "cd_my_item_image",
    "iv_order_thumb": "cd_order_thumb",
    "iv_post_image": "cd_post_image",
    "iv_selected_post_image": "cd_selected_post_image",
    "iv_conversation_item_image": "cd_conversation_item_image",
    "iv_preview_1": "cd_preview_1",
    "iv_preview_2": "cd_preview_2",
    "iv_preview_3": "cd_preview_3",
}

# 纯装饰性元素：从无障碍树中排除，避免无意义朗读
DECORATIVE_IDS = {
    "nav_blur_background", "nav_post_glow",
    "nav_discovery_icon", "nav_messages_icon", "nav_post_icon",
    "nav_cart_icon", "nav_profile_icon",
    "iv_search_icon", "iv_profile_avatar", "tv_profile_avatar",
    "tv_profile_avatar_badge", "iv_conversation_avatar",
    "tv_preview_more", "nav_messages_badge", "nav_cart_badge",
    "tv_header_unread_badge", "tv_conversation_unread_badge",
}

# 原本硬编码的 contentDescription → 改用字符串资源
HARDCODED_CD = {
    "nav_post": "cd_nav_post",
    "layout_profile_avatar": "cd_change_avatar",
}

# 固定高度小于 48dp 的交互控件：提升触摸目标
TOUCH_FIX_IDS = {
    "layout_search_box", "sp_category", "et_post_condition", "et_post_location",
    "btn_delete_post", "btn_edit_item", "btn_delete_item",
    "btn_order_secondary", "btn_order_primary",
    "btn_cart_item_contact", "btn_cart_item_remove",
}

# 列表条目根布局：Adapter 中绑定了 itemView.setOnClickListener，需键盘可聚焦
ITEM_ROOT_FILES = {
    "item_cart_entry.xml", "item_community_post.xml", "item_conversation.xml",
    "item_marketplace_card.xml", "item_my_listing.xml", "item_order_card.xml",
}

# 正则无法直接提取点击关系的控件（通过变量名 / 循环 / 动态 findViewById 绑定）
EXTRA_FOCUS_IDS = {
    "nav_discovery", "nav_messages", "nav_post", "nav_cart", "nav_profile",
    "layout_search_box", "layout_profile_avatar", "card_purchases", "card_sales",
    "layout_upload_photos", "layout_unread_summary", "layout_pro_tip",
    "row_account_listings", "row_account_community", "row_account_settings",
    "tab_follow", "iv_search_camera", "iv_search_icon", "fab_new_message",
    "category_textbooks", "category_electronics", "category_dorm",
    "category_fashion", "category_sports", "category_cycles", "category_art",
    "category_beauty", "category_food", "category_more",
    "tv_tab_recommend", "tv_tab_latest", "tv_header_unread_badge",
    "tv_summary_toggle_unread", "tv_empty_state", "tv_empty",
    "btn_close_pro_tip",
}

IMAGE_TAGS = {"ImageView", "ImageButton", "AppCompatImageView", "AppCompatImageButton",
              "ShapeableImageView", "FloatingActionButton"}
EDIT_TAGS = {"EditText", "AutoCompleteTextView", "TextInputEditText", "AppCompatEditText"}
FOCUSABLE_BY_DEFAULT = {"Button", "EditText", "CheckBox", "RadioButton", "Switch",
                        "ToggleButton", "Spinner", "SeekBar", "RatingBar",
                        "AutoCompleteTextView", "TextInputEditText", "CheckedTextView",
                        "AppCompatButton", "AppCompatEditText", "AppCompatCheckBox",
                        "AppCompatRadioButton", "SwitchCompat", "MaterialButton",
                        "MaterialCheckBox", "MaterialRadioButton", "SwitchMaterial",
                        "Chip", "TextInputEditText"}
CONTAINER_TAGS = {"LinearLayout", "FrameLayout", "RelativeLayout", "ConstraintLayout",
                  "androidx.constraintlayout.widget.ConstraintLayout", "CardView",
                  "MaterialCardView", "View", "ViewGroup", "Toolbar", "MaterialToolbar"}

TAG_RE = re.compile(
    r'<([A-Za-z0-9_.]+)((?:[^>"\']|"[^"]*"|\'[^\']*\')*?)(/?)>', re.S)
ID_RE = re.compile(r'android:id="@\+id/([A-Za-z0-9_]+)"')
ATTR_NAME_RE = re.compile(r'([\w:.\-]+)\s*=')
DP_RE = re.compile(r'^(\d+(?:\.\d+)?)dp$')

stats = {"files": 0, "cd": 0, "decorative": 0, "focus": 0, "clickable": 0,
         "touch": 0, "next_focus": 0, "heading": 0, "live": 0, "hardcode": 0,
         "item_root": 0, "toast": 0, "import": 0}


def short_tag(tag):
    return tag.split(".")[-1]


def collect_click_ids():
    """从 Java 源码提取所有绑定了点击监听的 R.id.xxx。"""
    ids = set()
    for root, dirs, files in os.walk(JAVA):
        for f in files:
            if not f.endswith(".java"):
                continue
            raw = open(os.path.join(root, f), encoding="utf-8").read()
            # 形式 1：findViewById(R.id.x).setOnClickListener(...)
            ids.update(re.findall(
                r'findViewById\(R\.id\.([A-Za-z0-9_]+)\)\s*\.setOn(?:Click|LongClick|CheckedChange)Listener', raw))
            # 形式 2：变量 = findViewById(R.id.x); ... 变量.setOnClickListener(...)
            vars_clicked = set(re.findall(r'(\w+)\.setOnClickListener\(', raw))
            for var, rid in re.findall(r'(\w+)\s*=\s*(?:\w+\.)?findViewById\(R\.id\.([A-Za-z0-9_]+)\)', raw):
                if var in vars_clicked:
                    ids.add(rid)
            # 形式 3：view.findViewById(R.id.x) 后立即 setOnClickListener（含换行）
            ids.update(re.findall(
                r'findViewById\(R\.id\.([A-Za-z0-9_]+)\)\s*\.\s*setOnClickListener', raw))
    return ids


def layout_elements(text):
    """返回 [(match, tag, attrs, selfclose, id, indent)]，按出现顺序。"""
    out = []
    for m in TAG_RE.finditer(text):
        tag, attrs, selfclose = m.group(1), m.group(2), m.group(3)
        if tag.startswith("?") or tag.startswith("!"):
            continue
        idm = ID_RE.search(attrs)
        ident = idm.group(1) if idm else None
        line_start = text.rfind("\n", 0, m.start()) + 1
        indent = re.match(r"[ \t]*", text[line_start:m.start()]).group(0)
        out.append(dict(m=m, tag=tag, attrs=attrs, selfclose=selfclose,
                        id=ident, indent=indent))
    return out


def patch_layout(path, click_ids, dry_run):
    raw = open(path, encoding="utf-8").read()
    elements = layout_elements(raw)
    filename = os.path.basename(path)

    # 键盘导航链候选：按视觉顺序排列的可聚焦控件
    candidates = []
    for e in elements:
        tag, attrs, ident = short_tag(e["tag"]), e["attrs"], e["id"]
        if not ident:
            continue
        focusable = 'android:focusable="true"' in attrs or 'android:clickable="true"' in attrs
        if (tag in EDIT_TAGS or tag in FOCUSABLE_BY_DEFAULT
                or tag in IMAGE_TAGS or focusable
                or ident in click_ids or ident in EXTRA_FOCUS_IDS):
            candidates.append(ident)

    edits = []  # (start, end, replacement)
    for e in elements:
        m, tag_s, attrs, selfclose = e["m"], short_tag(e["tag"]), e["attrs"], e["selfclose"]
        ident, indent = e["id"], e["indent"]
        existing = set(a.split(":")[-1] for a in ATTR_NAME_RE.findall(attrs))
        adds = []
        new_attrs = attrs

        # --- 1. 硬编码 contentDescription 替换为字符串资源
        if ident in HARDCODED_CD and "contentDescription" in existing:
            new_attrs, n = re.subn(
                r'android:contentDescription="[^"]*"',
                'android:contentDescription="@string/%s"' % HARDCODED_CD[ident],
                new_attrs)
            stats["hardcode"] += n

        # --- 2. 图片类控件 / 装饰性元素
        no_id_image = (tag_s in IMAGE_TAGS and ident is None)
        if (ident in DECORATIVE_IDS or no_id_image) and "importantForAccessibility" not in existing:
            adds.append('android:importantForAccessibility="no"')
            stats["decorative"] += 1
        elif tag_s in IMAGE_TAGS and ident in CD_MAP:
            if "contentDescription" not in existing and ident not in HARDCODED_CD:
                adds.append('android:contentDescription="@string/%s"' % CD_MAP[ident])
                stats["cd"] += 1

        # --- 3. 可点击容器 / 符号按钮的语义标签（非图片类控件）
        if ident in CD_MAP and tag_s not in IMAGE_TAGS:
            if "contentDescription" not in existing and ident not in HARDCODED_CD:
                adds.append('android:contentDescription="@string/%s"' % CD_MAP[ident])
                stats["cd"] += 1

        # --- 4. 键盘可达：Java 绑定点击但 XML 未声明可聚焦
        # 装饰性元素已从无障碍树中排除，不能再被键盘聚焦到「隐形」节点上
        need_focus = ((ident in click_ids or ident in EXTRA_FOCUS_IDS)
                      and ident not in DECORATIVE_IDS)
        if need_focus and tag_s not in FOCUSABLE_BY_DEFAULT:
            if 'android:focusable="false"' in new_attrs:
                # 原布局显式写死了 false，直接纠正为 true
                new_attrs = new_attrs.replace('android:focusable="false"',
                                              'android:focusable="true"')
                stats["focus"] += 1
            elif "focusable" not in existing:
                adds.append('android:focusable="true"')
                stats["focus"] += 1
        # 原本就在 XML 声明 clickable 的容器，补上键盘聚焦
        if ident in ("layout_search_box", "card_purchases", "card_sales", "layout_profile_avatar"):
            if "clickable" not in existing:
                adds.append('android:clickable="true"')
                stats["clickable"] += 1

        # --- 5. 触摸目标提升到 48dp
        if ident in TOUCH_FIX_IDS:
            def fix_height(mm):
                val = mm.group(2)
                dpm = DP_RE.match(val.strip())
                if dpm and 0 < float(dpm.group(1)) < 48:
                    stats["touch"] += 1
                    return '%s="48dp"' % mm.group(1)
                return mm.group(0)
            new_attrs = re.sub(r'(android:layout_height)="(\d+(?:\.\d+)?dp)"',
                               fix_height, new_attrs)

        # --- 6. 显式键盘前进顺序
        if tag_s in EDIT_TAGS and ident in candidates:
            idx = candidates.index(ident)
            target = None
            for j in range(1, len(candidates) + 1):
                cand = candidates[(idx + j) % len(candidates)]
                if cand != ident:
                    target = cand
                    break
            if target and "nextFocusForward" not in existing:
                adds.append('android:nextFocusForward="@+id/%s"' % target)
                stats["next_focus"] += 1

        # --- 7. 标题与实时区域
        if (tag_s in ("TextView", "AppCompatTextView", "MaterialTextView")
                and ident and re.search(r'(title|header)', ident, re.I)
                and "accessibilityHeading" not in existing):
            adds.append('android:accessibilityHeading="true"')
            stats["heading"] += 1
        if (tag_s in ("TextView", "AppCompatTextView", "MaterialTextView")
                and ident and re.search(r'(empty|_count$|status)', ident, re.I)
                and "accessibilityLiveRegion" not in existing):
            adds.append('android:accessibilityLiveRegion="polite"')
            stats["live"] += 1

        # --- 8. 列表条目根布局：键盘可聚焦（运行时由 Adapter 设为 clickable）
        if (filename in ITEM_ROOT_FILES and e is elements[0]
                and "focusable" not in existing):
            adds.append('android:focusable="true"')
            stats["item_root"] += 1

        if not adds and new_attrs == attrs:
            continue

        body = new_attrs.rstrip()
        inner_indent = indent + "    "
        for a in adds:
            body += "\n" + inner_indent + a
        replacement = ("<" + e["tag"] + body + "\n" + indent
                       + ("/>" if selfclose else ">"))
        edits.append((m.start(), m.end(), replacement))

    if not edits:
        return raw
    out = raw
    for start, end, replacement in reversed(edits):
        out = out[:start] + replacement + out[end:]
    stats["files"] += 1
    if not dry_run:
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.write(out)
    return out


# 只替换 makeText(...).show()，保留原有分号：
# 既适用于独立语句，也适用于 lambda 表达式（v -> A11y.toast(...)）。
TOAST_RE = re.compile(
    r'Toast\.makeText\(\s*([^,]+?)\s*,\s*(.+?)\s*,\s*Toast\.LENGTH_(?:SHORT|LONG)\s*\)\s*\.show\(\)',
    re.S)


def patch_java(path, dry_run):
    raw = open(path, encoding="utf-8").read()
    if os.path.basename(path) == "A11y.java":
        return 0
    new, n = TOAST_RE.subn(
        lambda m: "A11y.toast(%s, %s)" % (m.group(1).strip(), m.group(2).strip()),
        raw)
    if not n:
        return 0
    stats["toast"] += n
    if "import com.example.homework.util.A11y;" not in new:
        new = re.sub(r'(package\s+[\w.]+\s*;\s*\n)',
                     r'\1import com.example.homework.util.A11y;\n', new, count=1)
        stats["import"] += 1
    # 移除已不再使用的 Toast import
    body = new.replace("import android.widget.Toast;\n", "")
    if "Toast" not in body:
        new = body
    if not dry_run:
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.write(new)
    return n


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    if not os.path.isdir(LAYOUT):
        print("错误：找不到布局目录 %s" % LAYOUT, file=sys.stderr)
        return 2

    click_ids = collect_click_ids()
    print("Java 中提取到点击绑定控件 %d 个" % len(click_ids))

    for f in sorted(os.listdir(LAYOUT)):
        if f.endswith(".xml"):
            patch_layout(os.path.join(LAYOUT, f), click_ids, args.dry_run)

    for root, dirs, files in os.walk(JAVA):
        for f in sorted(files):
            if f.endswith(".java"):
                patch_java(os.path.join(root, f), args.dry_run)

    print("\n改造统计%s：" % ("（dry-run）" if args.dry_run else ""))
    for k, v in stats.items():
        print("  %-12s %d" % (k, v))

    # 校验：XML 全部可解析、Java 无残留 Toast.makeText(...).show()
    import xml.etree.ElementTree as ET
    bad = []
    for f in sorted(os.listdir(LAYOUT)):
        if f.endswith(".xml"):
            try:
                ET.parse(os.path.join(LAYOUT, f))
            except ET.ParseError as e:
                bad.append("%s: %s" % (f, e))
    leftover = 0
    for root, dirs, files in os.walk(JAVA):
        for f in files:
            if f.endswith(".java") and f != "A11y.java":
                t = open(os.path.join(root, f), encoding="utf-8").read()
                leftover += len(re.findall(r'Toast\.makeText\([^;]*?\.show\(\);', t, re.S))
    print("\n校验：XML 解析失败 %d 个；残留 Toast.makeText(...).show() %d 处" % (len(bad), leftover))
    for b in bad:
        print("  " + b)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
