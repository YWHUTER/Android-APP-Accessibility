#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
揭榜任务二「视障友好适配」综合静态校验（可复现验证）。

在没有 Android SDK / 模拟器的环境下，尽最大可能验证改造的正确性：
  1. 全部 XML（布局 / values / drawable / Manifest）可解析；
  2. 布局中的 @string / @drawable / @color 等资源引用均存在；
  3. Java 中 R.string / R.id / R.drawable / R.layout / R.color 等引用均存在；
  4. Java 中调用的 A11y.xxx 方法均在工具类中定义（拼写错误检查）；
  5. 每个使用 A11y 的 Java 文件都已 import；
  6. 每个 Java 文件的括号 / 引号 / 注释结构平衡（粗语法检查）；
  7. 统计适配规模（Toast 播报、语义标签、焦点声明等）。

用法：python tools/verify_a11y.py [--out docs/verification-report.md] [--strict]
退出码：0 = 全部通过；1 = 存在问题（--strict）；2 = 路径错误。
"""

import argparse
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
LAYOUT = os.path.join(RES, "layout")
JAVA = os.path.join(ROOT, "app", "src", "main", "java")
MANIFEST = os.path.join(ROOT, "app", "src", "main", "AndroidManifest.xml")
A11Y_FILE = os.path.join(JAVA, "com", "example", "homework", "util", "A11y.java")

VALUE_TAGS = {
    "string": "string", "color": "color", "dimen": "dimen", "style": "style",
    "bool": "bool", "integer": "integer", "string-array": "array",
    "integer-array": "array", "array": "array", "plurals": "plurals",
    "attr": "attr", "declare-styleable": "styleable",
}
# 资源名可包含点（如 @style/Theme.Homework）
REF_RE = re.compile(r"@(?!android:)(string|drawable|color|mipmap|dimen|style|xml|array|bool|integer|layout|id)/([A-Za-z0-9_.]+)")
# 负向后顾排除 android.R.xxx / androidx...R.xxx 中的 R 前缀，只匹配本项目 R
R_REF_RE = re.compile(r"(?<![\w.])R\.([a-z]+)\.([A-Za-z0-9_]+)")
A11Y_CALL_RE = re.compile(r"\bA11y\.([A-Za-z0-9_]+)\s*\(")

# 依赖库（Material Components / AppCompat）提供、不在本项目 res 下的资源
EXTERNAL_RESOURCES = {
    "string": {"appbar_scrolling_view_behavior"},
}


def collect_definitions():
    defs = defaultdict(set)
    for root, dirs, files in os.walk(RES):
        folder = os.path.basename(root)
        kind = folder.split("-")[0]
        for f in files:
            stem, ext = os.path.splitext(f)
            if kind in ("drawable", "mipmap", "xml", "layout", "anim", "animator",
                        "raw", "font", "color", "menu", "navigation", "transition"):
                defs[kind].add(stem)
            if folder.startswith("values") and ext == ".xml":
                try:
                    tree = ET.parse(os.path.join(root, f))
                    for child in tree.getroot():
                        name = child.get("name")
                        if not name:
                            continue
                        t = VALUE_TAGS.get(child.tag)
                        if t:
                            defs[t].add(name)
                        typ = child.get("type")   # <item type="id" name="..."/>
                        if typ:
                            defs[typ].add(name)
                except ET.ParseError:
                    pass
    # @+id/ 定义的 id 也会生成 R.id
    for f in os.listdir(LAYOUT):
        if f.endswith(".xml"):
            raw = open(os.path.join(LAYOUT, f), encoding="utf-8").read()
            defs["id"].update(re.findall(r"@\+id/([A-Za-z0-9_]+)", raw))
    return defs


def xml_files():
    out = [MANIFEST]
    for root, dirs, files in os.walk(RES):
        for f in files:
            if f.endswith(".xml"):
                out.append(os.path.join(root, f))
    return sorted(out)


def java_files():
    out = []
    for root, dirs, files in os.walk(JAVA):
        for f in files:
            if f.endswith(".java"):
                out.append(os.path.join(root, f))
    return sorted(out)


def brace_balance(src):
    """忽略字符串 / 字符字面量 / 注释后的括号平衡检查。"""
    stack = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == "/" and i + 1 < n:
            if src[i + 1] == "/":
                j = src.find("\n", i)
                i = j if j != -1 else n
                continue
            if src[i + 1] == "*":
                j = src.find("*/", i + 2)
                i = (j + 2) if j != -1 else n
                continue
        if c in ('"', "'"):
            quote = c
            i += 1
            while i < n:
                if src[i] == "\\":
                    i += 2
                    continue
                if src[i] == quote:
                    i += 1
                    break
                i += 1
            continue
        if c in "({[":
            stack.append(c)
        elif c in ")}]":
            pair = {")": "(", "}": "{", "]": "["}[c]
            if stack and stack[-1] == pair:
                stack.pop()
            else:
                return False
        i += 1
    return not stack


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=None)
    ap.add_argument("--strict", action="store_true")
    args = ap.parse_args()
    if not os.path.isdir(RES):
        print("错误：找不到 res 目录", file=sys.stderr)
        return 2

    out_path = args.out or os.path.join(ROOT, "docs", "verification-report.md")
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)

    problems = []
    defs = collect_definitions()

    # 1. XML 解析
    xfiles = xml_files()
    for p in xfiles:
        try:
            ET.parse(p)
        except (ET.ParseError, UnicodeDecodeError) as e:
            problems.append(("XML 解析失败", os.path.relpath(p, ROOT).replace("\\", "/"), str(e)))

    # 2/3. 资源引用完整性（布局 + Java）
    for p in xfiles:
        if not p.endswith(".xml"):
            continue
        raw = open(p, encoding="utf-8").read()
        rel = os.path.relpath(p, ROOT).replace("\\", "/")
        for kind, name in REF_RE.findall(raw):
            if kind == "id" or name in EXTERNAL_RESOURCES.get(kind, set()):
                continue
            if name not in defs.get(kind, set()):
                problems.append(("资源引用缺失", rel, "@%s/%s" % (kind, name)))

    for p in java_files():
        raw = open(p, encoding="utf-8").read()
        rel = os.path.relpath(p, ROOT).replace("\\", "/")
        for kind, name in R_REF_RE.findall(raw):
            if name not in defs.get(kind, set()):
                problems.append(("Java 资源引用缺失", rel, "R.%s.%s" % (kind, name)))

    # 4. A11y 方法名检查
    a11y_src = open(A11Y_FILE, encoding="utf-8").read()
    defined = set(re.findall(r"public\s+static\s+\S+\s+(\w+)\s*\(", a11y_src))
    used = Counter()
    for p in java_files():
        raw = open(p, encoding="utf-8").read()
        rel = os.path.relpath(p, ROOT).replace("\\", "/")
        methods = A11Y_CALL_RE.findall(raw)
        used.update(methods)
        for m in methods:
            if m not in defined:
                problems.append(("A11y 方法未定义", rel, "A11y.%s()" % m))
        # 5. import 检查
        if "A11y." in raw and "import com.example.homework.util.A11y;" not in raw \
                and not rel.endswith("util/A11y.java"):
            problems.append(("缺少 A11y import", rel, ""))
        # 6. 括号平衡
        if not brace_balance(raw):
            problems.append(("括号不平衡", rel, ""))

    # 6b. 可选：使用 javalang 做 Java AST 语法解析（需要 pip install javalang）
    ast_enabled, ast_failed, ast_total = False, 0, 0
    try:
        import javalang
        ast_enabled = True
        for p in java_files():
            rel = os.path.relpath(p, ROOT).replace("\\", "/")
            ast_total += 1
            try:
                javalang.parse.parse(open(p, encoding="utf-8").read())
            except Exception as e:
                ast_failed += 1
                problems.append(("Java 语法解析失败", rel, str(e)[:140]))
    except ImportError:
        pass

    # 统计适配规模
    layout_stats = Counter()
    for f in os.listdir(LAYOUT):
        if f.endswith(".xml"):
            t = open(os.path.join(LAYOUT, f), encoding="utf-8").read()
            layout_stats["contentDescription"] += len(re.findall(r'android:contentDescription=', t))
            layout_stats["importantForAccessibility_no"] += len(
                re.findall(r'android:importantForAccessibility="no"', t))
            layout_stats["focusable_true"] += len(re.findall(r'android:focusable="true"', t))
            layout_stats["nextFocusForward"] += len(re.findall(r'android:nextFocusForward=', t))
            layout_stats["accessibilityHeading"] += len(re.findall(r'android:accessibilityHeading=', t))
            layout_stats["accessibilityLiveRegion"] += len(re.findall(r'android:accessibilityLiveRegion=', t))
    toast_calls = 0
    for p in java_files():
        toast_calls += len(re.findall(r"\bA11y\.toast\s*\(", open(p, encoding="utf-8").read()))

    lines = [
        "# 视障友好适配 · 综合静态校验报告",
        "",
        "> 由 `tools/verify_a11y.py` 自动生成，可在任意装有 Python 3 的机器上重复执行。",
        "",
        "## 结论",
        "",
        "| 指标 | 数值 |",
        "| --- | --- |",
        "| XML 文件 | %d |" % len(xfiles),
        "| Java 文件 | %d |" % len(java_files()),
        "| 布局文件 | %d |" % len([f for f in os.listdir(LAYOUT) if f.endswith(".xml")]),
        "| 问题数 | %d |" % len(problems),
        "| Java AST 语法解析 | %s |" % (
            "已启用（javalang），%d 个文件全部通过" % ast_total
            if ast_enabled and ast_failed == 0 else
            "已启用，%d/%d 失败" % (ast_failed, ast_total) if ast_enabled else
            "未启用（未安装 javalang，可 pip install javalang 后重跑）"),
        "| 结论 | %s |" % ("全部通过" if not problems else "存在问题，需修复"),
        "",
        "## 适配规模统计",
        "",
        "| 项目 | 数量 |",
        "| --- | --- |",
    ]
    for k, v in sorted(layout_stats.items()):
        lines.append("| 布局属性 `%s` | %d |" % (k, v))
    lines.append("| `A11y.toast`（Toast + 无障碍播报） | %d |" % toast_calls)
    lines.append("| A11y 工具类公开方法 | %d |" % len(defined))
    lines.append("")
    lines.append("## A11y 方法调用分布")
    lines.append("")
    lines.append("| 方法 | 调用次数 |")
    lines.append("| --- | --- |")
    for m, c in sorted(used.items(), key=lambda x: (-x[1], x[0])):
        lines.append("| `A11y.%s()` | %d |" % (m, c))
    lines.append("")
    lines.append("## 问题明细")
    lines.append("")
    if not problems:
        lines.append("未发现问题。")
    else:
        lines.append("| 类别 | 文件 | 详情 |")
        lines.append("| --- | --- | --- |")
        for kind, rel, detail in problems:
            lines.append("| %s | `%s` | %s |" % (kind, rel, detail or "-"))
    lines.append("")
    lines.append("## 校验范围说明")
    lines.append("")
    lines.append("- 已覆盖：XML 合法性、资源引用完整性、A11y 方法名与 import、")
    lines.append("  Java 括号 / 字符串 / 注释结构平衡、Java AST 语法解析（javalang）、适配规模统计。")
    lines.append("- 未覆盖：Java 类型检查与 Android SDK 编译（本机未安装 Android SDK）、")
    lines.append("  TalkBack 实际朗读、键盘焦点动态顺序、系统字体放大后的布局表现。")
    lines.append("  这些项目需在 Android Studio + 模拟器 / 真机上按 `docs/测试记录.md` 人工复验。")
    lines.append("")
    with open(out_path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))

    print("XML %d 个，Java %d 个，问题 %d 项" % (len(xfiles), len(java_files()), len(problems)))
    print("布局改造：%s" % ", ".join("%s=%d" % kv for kv in sorted(layout_stats.items())))
    print("A11y.toast 调用 %d 处，A11y 方法 %d 个" % (toast_calls, len(defined)))
    for kind, rel, detail in problems[:30]:
        print("  [%s] %s %s" % (kind, rel, detail))
    if len(problems) > 30:
        print("  ... 其余 %d 项见报告" % (len(problems) - 30))
    print("报告：%s" % out_path)

    if args.strict and problems:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
