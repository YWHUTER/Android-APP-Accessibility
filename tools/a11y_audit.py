#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Android 无障碍静态审计工具
==========================

用途：揭榜任务二「视障友好适配」的可复现验证工具。对 res/layout 下的
XML 布局做静态检查，覆盖 Android 官方无障碍规范中可由静态分析判定的部分：

  A11Y-001 图片类控件缺少无障碍标签（无 contentDescription 且未标记装饰性）
  A11Y-002 contentDescription 使用硬编码文本（应引用 @string 资源）
  A11Y-003 输入框缺少 hint / labelFor（TalkBack 无法说明输入用途）
  A11Y-004 可点击控件没有任何可朗读内容（无 text / contentDescription）
  A11Y-005 可点击控件触摸目标小于 48dp
  A11Y-006 文本尺寸使用 dp 而非 sp（字体放大时无法跟随缩放）
  A11Y-007 布局引用的资源不存在（@string / @drawable / @color / ...）
  A11Y-008 自定义可点击控件未声明可聚焦（键盘 / 方向键无法到达）
  A11Y-009 表单控件缺少 nextFocusForward（键盘 Tab 顺序不可预期）
  A11Y-010 Java 中绑定了点击监听、但布局未声明可聚焦（键盘 / 方向键无法到达）

用法：
  python tools/a11y_audit.py                      # 审计并输出报告到 docs/a11y-audit-report.md
  python tools/a11y_audit.py --out report.md     # 指定报告路径
  python tools/a11y_audit.py --json report.json  # 同时输出 JSON 结果
  python tools/a11y_audit.py --strict            # 有 error 级问题时退出码为 1（CI 门禁）

退出码：0 = 通过；1 = 存在 error 级问题（--strict）；2 = 参数/路径错误。

说明：本工具只做静态分析，不能替代 TalkBack / Accessibility Scanner / 真机
键盘导航的人工实测。人工实测项见 docs/测试记录.md。
"""

import argparse
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

ANDROID_NS = "http://schemas.android.com/apk/res/android"
A = "{%s}" % ANDROID_NS
TOOLS_NS = "http://schemas.android.com/tools"
T = "{%s}" % TOOLS_NS

# 图片类控件：必须有 contentDescription 或显式声明为装饰性元素
IMAGE_TAGS = {
    "ImageView", "ImageButton",
    "androidx.appcompat.widget.AppCompatImageView",
    "androidx.appcompat.widget.AppCompatImageButton",
    "com.google.android.material.imageview.ShapeableImageView",
    "com.google.android.material.floatingactionbutton.FloatingActionButton",
    "com.google.android.material.button.MaterialButton",  # 仅当无文本时才由 A11Y-004 判定
    "ImageSwitcher", "ImageView", "ZoomButton",
}

# 自带可朗读文本（text/hint）能力的控件
TEXT_TAGS = {
    "TextView", "Button", "EditText", "CheckBox", "CheckedTextView",
    "RadioButton", "Switch", "ToggleButton", "AutoCompleteTextView",
    "MultiAutoCompleteTextView", "TextInputEditText",
    "androidx.appcompat.widget.AppCompatTextView",
    "androidx.appcompat.widget.AppCompatButton",
    "androidx.appcompat.widget.AppCompatEditText",
    "androidx.appcompat.widget.AppCompatCheckBox",
    "androidx.appcompat.widget.AppCompatRadioButton",
    "androidx.appcompat.widget.SwitchCompat",
    "com.google.android.material.button.MaterialButton",
    "com.google.android.material.textfield.TextInputEditText",
    "com.google.android.material.checkbox.MaterialCheckBox",
    "com.google.android.material.radiobutton.MaterialRadioButton",
    "com.google.android.material.switchmaterial.SwitchMaterial",
    "com.google.android.material.chip.Chip",
    "com.google.android.material.textview.MaterialTextView",
}

# 平台默认可聚焦的交互控件（无需手动声明 focusable）
FOCUSABLE_BY_DEFAULT = {
    "Button", "EditText", "CheckBox", "RadioButton", "Switch", "ToggleButton",
    "Spinner", "SeekBar", "RatingBar", "AutoCompleteTextView",
    "TextInputEditText", "CheckedTextView",
    "androidx.appcompat.widget.AppCompatButton",
    "androidx.appcompat.widget.AppCompatEditText",
    "androidx.appcompat.widget.AppCompatCheckBox",
    "androidx.appcompat.widget.AppCompatRadioButton",
    "androidx.appcompat.widget.SwitchCompat",
    "com.google.android.material.button.MaterialButton",
    "com.google.android.material.textfield.TextInputEditText",
    "com.google.android.material.checkbox.MaterialCheckBox",
    "com.google.android.material.radiobutton.MaterialRadioButton",
    "com.google.android.material.switchmaterial.SwitchMaterial",
    "com.google.android.material.chip.Chip",
}

EDIT_TAGS = {
    "EditText", "AutoCompleteTextView", "MultiAutoCompleteTextView",
    "TextInputEditText", "androidx.appcompat.widget.AppCompatEditText",
    "androidx.appcompat.widget.AppCompatAutoCompleteTextView",
    "com.google.android.material.textfield.TextInputEditText",
}

CLICKABLE_TAGS = IMAGE_TAGS | TEXT_TAGS | {
    "View", "LinearLayout", "FrameLayout", "RelativeLayout", "ConstraintLayout",
    "androidx.constraintlayout.widget.ConstraintLayout",
    "androidx.cardview.widget.CardView",
    "com.google.android.material.card.MaterialCardView",
    "ViewGroup", "RadioGroup", "Toolbar",
    "androidx.appcompat.widget.Toolbar",
    "com.google.android.material.appbar.MaterialToolbar",
    "RecyclerView", "androidx.recyclerview.widget.RecyclerView",
    "com.google.android.material.tabs.TabLayout",
}


def _shorten_tags(collection):
    """把集合中的全限定类名统一为短类名，与 local_tag() 的返回值保持一致。"""
    return {t.split(".")[-1] if "." in t else t for t in collection}


IMAGE_TAGS = _shorten_tags(IMAGE_TAGS)
TEXT_TAGS = _shorten_tags(TEXT_TAGS)
FOCUSABLE_BY_DEFAULT = _shorten_tags(FOCUSABLE_BY_DEFAULT)
EDIT_TAGS = _shorten_tags(EDIT_TAGS)
CLICKABLE_TAGS = _shorten_tags(CLICKABLE_TAGS)

DECORATIVE_HINT = re.compile(
    r"(divider|separator|shadow|bg_|background|decor|illustration|placeholder|"
    r"dot|badge|glow|orb|highlight|overlay|gradient|line_|ic_launcher|"
    r"hero|banner_bg|track|fill|border|corner|mask|blur)", re.I)

# 由依赖库（Material Components / AppCompat）提供、不在本项目 res 下的资源。
# 静态审计无法解析依赖库中的资源表，故列入白名单避免误报。
EXTERNAL_RESOURCES = {
    "string": {
        "appbar_scrolling_view_behavior",
        "bottom_sheet_slide_duration",
        "character_counter_content_description",
        "password_toggle_content_description",
        "clear_text_end_icon_content_description",
        "error_icon_content_description",
        "exposed_dropdown_menu_content_description",
        "mtrl_exceed_max_badge_number_suffix",
        "search_menu_title",
        "status_bar_notification_info_overflow",
    },
    "dimen": {"mtrl_min_touch_target_size"},
    "style": {"Widget.MaterialComponents.TextInputLayout.FilledBox"},
}

REF_RE = re.compile(r"@(?!android:)(string|drawable|color|mipmap|dimen|style|xml|array|bool|integer|layout|id)/([A-Za-z0-9_]+)")
DP_RE = re.compile(r"^(\d+(?:\.\d+)?)dp$")

SEVERITY = {
    "A11Y-001": "error", "A11Y-002": "warning", "A11Y-003": "error",
    "A11Y-004": "error", "A11Y-005": "warning", "A11Y-006": "warning",
    "A11Y-007": "error", "A11Y-008": "error", "A11Y-009": "info",
    "A11Y-010": "error",
}
TITLE = {
    "A11Y-001": "图片控件缺少无障碍标签",
    "A11Y-002": "contentDescription 硬编码",
    "A11Y-003": "输入框缺少 hint / labelFor",
    "A11Y-004": "可点击控件无可朗读内容",
    "A11Y-005": "触摸目标小于 48dp",
    "A11Y-006": "文本尺寸使用 dp（不支持字体缩放）",
    "A11Y-007": "引用的资源不存在",
    "A11Y-008": "可点击控件未声明可聚焦",
    "A11Y-009": "表单缺少 nextFocusForward",
    "A11Y-010": "Java 绑定点击但布局未声明可聚焦",
}


def local_tag(tag):
    return tag.split(".")[-1] if "." in tag else tag


def attr(el, name):
    return el.get(A + name)


def id_value(el):
    v = attr(el, "id")
    if not v:
        return None
    return v.split("/")[-1]


def is_clickable(el):
    return (attr(el, "clickable") == "true"
            or attr(el, "onClick") is not None
            or attr(el, "longClickable") == "true")


def has_readable_text(el, tag):
    if attr(el, "text") is not None:
        return True
    if tag in EDIT_TAGS and attr(el, "hint") is not None:
        return True
    if attr(el, "contentDescription") is not None:
        return True
    return False


def collect_resources(res_dir):
    """收集项目内实际存在的资源名。"""
    found = defaultdict(set)
    values_dir = os.path.join(res_dir, "values")
    for root, dirs, files in os.walk(res_dir):
        for f in files:
            stem, ext = os.path.splitext(f)
            if ext not in (".xml", ".png", ".jpg", ".webp", ".9.png"):
                continue
            folder = os.path.basename(root)
            kind = folder.split("-")[0]
            if kind in ("drawable", "mipmap", "xml", "layout", "anim", "animator",
                        "raw", "font", "color", "transition", "menu", "navigation"):
                found[kind].add(stem)
            if root.startswith(values_dir) or folder.startswith("values"):
                if ext == ".xml":
                    try:
                        tree = ET.parse(os.path.join(root, f))
                        for child in tree.getroot():
                            n = child.get("name")
                            if not n:
                                continue
                            t = child.tag
                            kind = ("string" if t == "string" else
                                    "color" if t == "color" else
                                    "dimen" if t == "dimen" else
                                    "style" if t == "style" else
                                    "array" if "array" in t else
                                    "bool" if t in ("bool", "item") else None)
                            if kind:
                                found[kind].add(n)
                    except ET.ParseError:
                        pass
    # values 下 <item name=... type=...> 形式
    for root, dirs, files in os.walk(res_dir):
        if not os.path.basename(root).startswith("values"):
            continue
        for f in files:
            if not f.endswith(".xml"):
                continue
            try:
                tree = ET.parse(os.path.join(root, f))
                for child in tree.getroot():
                    n = child.get("name")
                    typ = child.get("type")
                    if n and typ:
                        found[typ].add(n)
            except ET.ParseError:
                pass
    return found


def line_of_id(text, ident):
    if not ident:
        return 0
    for i, line in enumerate(text.splitlines(), 1):
        if re.search(r'@(\+)?id/' + re.escape(ident) + r'(?![A-Za-z0-9_])', line):
            return i
    return 0


def collect_java_click_ids(project_root):
    """扫描 Java 源码，提取所有绑定了点击监听的 R.id.xxx（跨文件按 id 名匹配）。"""
    java_dir = os.path.join(project_root, "app", "src", "main", "java")
    ids = set()
    if not os.path.isdir(java_dir):
        return ids
    for root, dirs, files in os.walk(java_dir):
        for f in files:
            if not f.endswith(".java"):
                continue
            raw = open(os.path.join(root, f), "r", encoding="utf-8").read()
            ids.update(re.findall(
                r'findViewById\(R\.id\.([A-Za-z0-9_]+)\)\s*\.setOn(?:Click|LongClick|CheckedChange)Listener',
                raw))
            ids.update(re.findall(
                r'findViewById\(R\.id\.([A-Za-z0-9_]+)\)\s*\.\s*setOnClickListener', raw))
            vars_clicked = set(re.findall(r'(\w+)\.setOnClickListener\(', raw))
            for var, rid in re.findall(
                    r'(\w+)\s*=\s*(?:\w+\.)?findViewById\(R\.id\.([A-Za-z0-9_]+)\)', raw):
                if var in vars_clicked:
                    ids.add(rid)
    return ids


def audit_file(path, resources, issues, click_ids=frozenset()):
    rel = os.path.relpath(path).replace("\\", "/")
    try:
        raw = open(path, "r", encoding="utf-8").read()
        root = ET.fromstring(raw)
    except (ET.ParseError, UnicodeDecodeError) as e:
        issues.append(dict(rule="A11Y-000", severity="error", file=rel, line=0,
                           widget="(XML 解析失败)", detail=str(e)))
        return

    for el in root.iter():
        if not isinstance(el.tag, str):
            continue
        tag = el.tag
        short = local_tag(tag)
        ident = id_value(el)
        widget = "@+id/" + ident if ident else short
        line = line_of_id(raw, ident)

        def add(rule, detail):
            issues.append(dict(rule=rule, severity=SEVERITY[rule], file=rel,
                               line=line, widget=widget, detail=detail))

        # A11Y-007 资源引用完整性（对所有属性）
        for k, v in el.attrib.items():
            for m in REF_RE.finditer(v or ""):
                kind, name = m.group(1), m.group(2)
                if kind in ("id",):
                    continue
                if name in EXTERNAL_RESOURCES.get(kind, set()):
                    continue  # 依赖库提供的资源，静态审计不解析其资源表
                if name not in resources.get(kind, set()):
                    add("A11Y-007", "引用 @%s/%s 未找到定义" % (kind, name))

        cd = attr(el, "contentDescription")
        important = attr(el, "importantForAccessibility")
        declared_decorative = important in ("no", "noHideDescendants")

        # A11Y-010 Java 里绑定了点击监听（含 setOnClickListener / setOnCheckedChangeListener），
        # 但布局未声明可聚焦：TalkBack 之外，键盘 / 方向键 / 开关控制无法到达该控件。
        if (ident in click_ids and short not in FOCUSABLE_BY_DEFAULT
                and not declared_decorative and attr(el, "focusable") != "true"):
            add("A11Y-010", "Java 中绑定了点击监听，但布局未声明 android:focusable=\"true\"，键盘/方向键无法到达")

        # A11Y-001 图片类控件
        if short in IMAGE_TAGS and not declared_decorative:
            if cd is None:
                add("A11Y-001", "%s 既无 contentDescription，也未标记 importantForAccessibility=\"no\"（装饰性）" % short)
            elif not str(cd).startswith("@string/"):
                add("A11Y-002", "contentDescription 应引用 @string 资源，当前为硬编码：%s" % str(cd)[:60])

        # A11Y-002 任意控件上的硬编码 contentDescription
        if cd is not None and short not in IMAGE_TAGS and not str(cd).startswith("@"):
            add("A11Y-002", "contentDescription 应引用 @string 资源，当前为硬编码：%s" % str(cd)[:60])

        # A11Y-003 输入框
        if short in EDIT_TAGS:
            if attr(el, "hint") is None and attr(el, "labelFor") is None and cd is None:
                add("A11Y-003", "%s 无 hint / labelFor / contentDescription，TalkBack 无法说明输入用途" % short)

        # A11Y-004 可点击控件无可朗读内容
        if is_clickable(el) and short in CLICKABLE_TAGS:
            if not has_readable_text(el, short) and not declared_decorative:
                add("A11Y-004", "%s 可点击但无 text / contentDescription，TalkBack 只会朗读「按钮」" % short)

        # A11Y-008 自定义可点击控件未声明可聚焦
        if is_clickable(el) and short not in FOCUSABLE_BY_DEFAULT:
            if attr(el, "focusable") != "true" and not declared_decorative:
                add("A11Y-008", "%s 可点击但未设置 android:focusable=\"true\"，键盘/方向键无法聚焦" % short)

        # A11Y-005 触摸目标 < 48dp
        # 说明：0dp 在 ConstraintLayout / LinearLayout weight 中表示「由约束或权重决定」，
        # 不代表实际尺寸，跳过；已用 minWidth/minHeight 补足到 48dp 的控件视为合规。
        if is_clickable(el) or short in FOCUSABLE_BY_DEFAULT:
            for dim_name, min_dim in (("layout_width", "minWidth"), ("layout_height", "minHeight")):
                m = DP_RE.match((attr(el, dim_name) or "").strip())
                if not m:
                    continue
                size = float(m.group(1))
                if size <= 0 or size >= 48:
                    continue
                min_m = DP_RE.match((attr(el, min_dim) or "").strip())
                if min_m and float(min_m.group(1)) >= 48:
                    continue
                add("A11Y-005", "%s=%sdp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足）" % (dim_name, m.group(1)))

        # A11Y-006 文本尺寸用 dp
        ts = attr(el, "textSize")
        if ts and DP_RE.match(ts.strip()):
            add("A11Y-006", "android:textSize=\"%s\" 不支持系统字体缩放，应改用 sp" % ts)

        # A11Y-009 表单焦点顺序（仅信息级）
        if short in EDIT_TAGS and attr(el, "nextFocusForward") is None:
            add("A11Y-009", "EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断")


def build_report(issues, layout_files, resources, strict):
    by_rule = Counter(i["rule"] for i in issues)
    by_file = Counter(i["file"] for i in issues)
    errors = sum(1 for i in issues if i["severity"] == "error")
    warnings = sum(1 for i in issues if i["severity"] == "warning")
    infos = sum(1 for i in issues if i["severity"] == "info")

    lines = []
    lines.append("# Android 无障碍静态审计报告")
    lines.append("")
    lines.append("> 由 `tools/a11y_audit.py` 自动生成，可重复执行验证。")
    lines.append("")
    lines.append("## 概览")
    lines.append("")
    lines.append("| 指标 | 数值 |")
    lines.append("| --- | --- |")
    lines.append("| 扫描布局文件 | %d |" % len(layout_files))
    lines.append("| 问题总数 | %d |" % len(issues))
    lines.append("| error（阻断） | %d |" % errors)
    lines.append("| warning | %d |" % warnings)
    lines.append("| info | %d |" % infos)
    lines.append("| 结论 | %s |" % ("通过（无 error 级问题）" if errors == 0 else "未通过（存在 error 级问题）"))
    lines.append("")
    lines.append("## 按规则统计")
    lines.append("")
    lines.append("| 规则 | 说明 | 数量 | 级别 |")
    lines.append("| --- | --- | --- | --- |")
    for rule in sorted(SEVERITY):
        lines.append("| %s | %s | %d | %s |" % (rule, TITLE[rule], by_rule.get(rule, 0), SEVERITY[rule]))
    if by_rule.get("A11Y-000"):
        lines.append("| A11Y-000 | XML 解析失败 | %d | error |" % by_rule["A11Y-000"])
    lines.append("")

    if by_file:
        lines.append("## 按文件统计")
        lines.append("")
        lines.append("| 布局文件 | 问题数 |")
        lines.append("| --- | --- |")
        for f, c in sorted(by_file.items(), key=lambda x: (-x[1], x[0])):
            lines.append("| `%s` | %d |" % (f, c))
        lines.append("")

    lines.append("## 问题明细")
    lines.append("")
    if not issues:
        lines.append("未发现任何问题。")
    else:
        lines.append("| 级别 | 规则 | 文件 | 行 | 控件 | 说明 |")
        lines.append("| --- | --- | --- | --- | --- | --- |")
        for i in sorted(issues, key=lambda x: (x["file"], x["line"], x["rule"])):
            lines.append("| %s | %s | `%s` | %s | `%s` | %s |" % (
                i["severity"], i["rule"], i["file"], i["line"] or "-",
                i["widget"], i["detail"].replace("|", "\\|")))
    lines.append("")
    lines.append("## 规则依据")
    lines.append("")
    lines.append("- A11Y-001/002/004：Android 官方「Make apps more accessible」——为所有非文本内容提供 `contentDescription`，装饰性图片设 `importantForAccessibility=\"no\"`。")
    lines.append("- A11Y-003：`EditText` 需 `hint` 或用 `labelFor` 关联可见标签。")
    lines.append("- A11Y-005/006：Material Accessibility / WCAG 2.1 —— 触摸目标至少 48dp，文字用 sp 以跟随系统字体缩放。")
    lines.append("- A11Y-008：所有可交互控件必须可由键盘 / 方向键聚焦（`android:focusable=\"true\"`）。")
    lines.append("- A11Y-009：键盘导航顺序应显式可控（`nextFocusForward` / `nextFocusDown` 等）。")
    lines.append("- A11Y-010：Java 中通过 `setOnClickListener` 绑定交互的控件，同样必须在布局中声明可聚焦（静态审计关联 Java 与 XML）。")
    lines.append("")
    lines.append("> 静态审计无法覆盖 TalkBack 实际朗读效果、焦点移动顺序的动态表现、")
    lines.append("> 弹窗与列表滚动播报、系统字体放大后的布局裁切等，需按 `docs/测试记录.md` 在真机/模拟器上人工复验。")
    lines.append("")
    return "\n".join(lines), errors


def main():
    ap = argparse.ArgumentParser(description="Android 无障碍静态审计工具")
    ap.add_argument("--root", default=None, help="项目根目录（默认：脚本所在目录的上一级）")
    ap.add_argument("--out", default=None, help="Markdown 报告输出路径")
    ap.add_argument("--json", default=None, help="JSON 结果输出路径")
    ap.add_argument("--strict", action="store_true", help="存在 error 级问题时返回退出码 1")
    ap.add_argument("--quiet", action="store_true", help="不向 stdout 打印摘要")
    args = ap.parse_args()

    here = os.path.dirname(os.path.abspath(__file__))
    root = os.path.abspath(args.root) if args.root else os.path.dirname(here)
    res_dir = os.path.join(root, "app", "src", "main", "res")
    layout_dir = os.path.join(res_dir, "layout")
    if not os.path.isdir(layout_dir):
        print("错误：找不到布局目录 %s" % layout_dir, file=sys.stderr)
        return 2

    out = args.out or os.path.join(root, "docs", "a11y-audit-report.md")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)

    layout_files = sorted(
        os.path.join(layout_dir, f) for f in os.listdir(layout_dir) if f.endswith(".xml"))
    resources = collect_resources(res_dir)
    click_ids = collect_java_click_ids(root)
    issues = []
    for f in layout_files:
        audit_file(f, resources, issues, click_ids)

    report, errors = build_report(issues, layout_files, resources, args.strict)
    with open(out, "w", encoding="utf-8") as fh:
        fh.write(report)

    if args.json:
        with open(args.json, "w", encoding="utf-8") as fh:
            json.dump(dict(layout_files=len(layout_files), issues=issues,
                           error_count=errors, total=len(issues)), fh,
                      ensure_ascii=False, indent=2)

    if not args.quiet:
        print("扫描布局 %d 个，发现问题 %d 项（error=%d）" % (
            len(layout_files), len(issues), errors))
        c = Counter(i["rule"] for i in issues)
        for rule in sorted(c):
            print("  %s %-28s %d" % (rule, TITLE.get(rule, ""), c[rule]))
        print("报告：%s" % out)

    if args.strict and errors:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
