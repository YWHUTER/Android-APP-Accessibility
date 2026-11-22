# Android 无障碍静态审计报告

> 由 `tools/a11y_audit.py` 自动生成，可重复执行验证。

## 概览

| 指标 | 数值 |
| --- | --- |
| 扫描布局文件 | 28 |
| 问题总数 | 0 |
| error（阻断） | 0 |
| warning | 0 |
| info | 0 |
| 结论 | 通过（无 error 级问题） |

## 按规则统计

| 规则 | 说明 | 数量 | 级别 |
| --- | --- | --- | --- |
| A11Y-001 | 图片控件缺少无障碍标签 | 0 | error |
| A11Y-002 | contentDescription 硬编码 | 0 | warning |
| A11Y-003 | 输入框缺少 hint / labelFor | 0 | error |
| A11Y-004 | 可点击控件无可朗读内容 | 0 | error |
| A11Y-005 | 触摸目标小于 48dp | 0 | warning |
| A11Y-006 | 文本尺寸使用 dp（不支持字体缩放） | 0 | warning |
| A11Y-007 | 引用的资源不存在 | 0 | error |
| A11Y-008 | 可点击控件未声明可聚焦 | 0 | error |
| A11Y-009 | 表单缺少 nextFocusForward | 0 | info |
| A11Y-010 | Java 绑定点击但布局未声明可聚焦 | 0 | error |

## 问题明细

未发现任何问题。

## 规则依据

- A11Y-001/002/004：Android 官方「Make apps more accessible」——为所有非文本内容提供 `contentDescription`，装饰性图片设 `importantForAccessibility="no"`。
- A11Y-003：`EditText` 需 `hint` 或用 `labelFor` 关联可见标签。
- A11Y-005/006：Material Accessibility / WCAG 2.1 —— 触摸目标至少 48dp，文字用 sp 以跟随系统字体缩放。
- A11Y-008：所有可交互控件必须可由键盘 / 方向键聚焦（`android:focusable="true"`）。
- A11Y-009：键盘导航顺序应显式可控（`nextFocusForward` / `nextFocusDown` 等）。
- A11Y-010：Java 中通过 `setOnClickListener` 绑定交互的控件，同样必须在布局中声明可聚焦（静态审计关联 Java 与 XML）。

> 静态审计无法覆盖 TalkBack 实际朗读效果、焦点移动顺序的动态表现、
> 弹窗与列表滚动播报、系统字体放大后的布局裁切等，需按 `docs/测试记录.md` 在真机/模拟器上人工复验。
