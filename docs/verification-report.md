# 视障友好适配 · 综合静态校验报告

> 由 `tools/verify_a11y.py` 自动生成，可在任意装有 Python 3 的机器上重复执行。

## 结论

| 指标 | 数值 |
| --- | --- |
| XML 文件 | 121 |
| Java 文件 | 54 |
| 布局文件 | 28 |
| 问题数 | 0 |
| Java AST 语法解析 | 已启用（javalang），54 个文件全部通过 |
| 结论 | 全部通过 |

## 适配规模统计

| 项目 | 数量 |
| --- | --- |
| 布局属性 `accessibilityHeading` | 17 |
| 布局属性 `accessibilityLiveRegion` | 14 |
| 布局属性 `contentDescription` | 54 |
| 布局属性 `focusable_true` | 98 |
| 布局属性 `importantForAccessibility_no` | 23 |
| 布局属性 `nextFocusForward` | 28 |
| `A11y.toast`（Toast + 无障碍播报） | 93 |
| A11y 工具类公开方法 | 10 |

## A11y 方法调用分布

| 方法 | 调用次数 |
| --- | --- |
| `A11y.toast()` | 93 |
| `A11y.asButton()` | 10 |
| `A11y.itemImage()` | 5 |
| `A11y.label()` | 3 |
| `A11y.decorative()` | 2 |
| `A11y.announce()` | 1 |

## 问题明细

未发现问题。

## 校验范围说明

- 已覆盖：XML 合法性、资源引用完整性、A11y 方法名与 import、
  Java 括号 / 字符串 / 注释结构平衡、Java AST 语法解析（javalang）、适配规模统计。
- 未覆盖：Java 类型检查与 Android SDK 编译（本机未安装 Android SDK）、
  TalkBack 实际朗读、键盘焦点动态顺序、系统字体放大后的布局表现。
  这些项目需在 Android Studio + 模拟器 / 真机上按 `docs/测试记录.md` 人工复验。
