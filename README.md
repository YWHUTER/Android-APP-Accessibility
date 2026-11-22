# 揭榜任务二 · 视障友好适配（Android 移动项目）

> 基于本人《软件工程》课程项目 **「校园智慧二手平台」**（Android 原生 Java，
> `com.example.homework`）做的 **无障碍适配改造**。
> **只做适配，不新增任何业务功能**——业务分支、页面、数据模型、网络与数据库逻辑均未改动。

原项目仓库：`YWHUTER/Android-APP`（私有课程项目）
本成果仓库：`YWHUTER/Android-APP-Accessibility`

---

## 一、任务依据

依据任务书「揭榜任务二：视障友好适配任务书」中 **（2）Android 移动项目** 分支，
按 Android 官方无障碍规范开展适配：

1. 为界面控件设置合理的语义信息（`contentDescription` 等），确保 TalkBack 可正确朗读；
2. 保证界面可通过键盘、方向键、无障碍导航完整操作，焦点逻辑清晰；
3. 保证屏幕切换、弹窗、列表等常用界面在 TalkBack 下可正常使用；
4. 确保原有功能在无障碍模式下可用、完整、不中断。

参考规范：Android Developers *Make apps more accessible*、`AccessibilityNodeInfo`、
Material Accessibility（触摸目标 ≥ 48dp）、WCAG 2.1 AA（文本对比度 ≥ 4.5:1）。

## 二、做了什么（摘要）

| 适配项 | 规模 |
| --- | --- |
| 扫描 / 改造布局文件 | 28 个 |
| 新增 / 修正 `contentDescription` | 54 处（全部引用 `@string` 资源，集中管理） |
| 标记装饰性元素 `importantForAccessibility="no"` | 23 处 |
| 补充键盘可聚焦 `android:focusable="true"` | 98 处 |
| 显式键盘导航 `nextFocusForward` | 28 个输入框 |
| 触摸目标提升到 48dp | 11 处 |
| 标题 `accessibilityHeading` / 实时区域 `accessibilityLiveRegion` | 17 / 14 处 |
| `Toast` → `A11y.toast`（视觉提示 + TalkBack 播报） | 93 处 |
| 运行时语义（导航角色、选中态、页面播报、列表动态描述） | 17 处 Java 补丁 |
| 颜色对比度优化 | `text_tertiary`、`danger` 加深 |

新增文件仅 3 类：无障碍文案资源、适配工具类 `A11y.java`、校验 / 改造脚本与文档。
详见 [`docs/变更清单.md`](docs/变更清单.md)。

## 三、如何验证（不需要 Android SDK）

```bash
# 1. 无障碍静态审计（应输出 0 问题）
python tools/a11y_audit.py --strict

# 2. 综合校验：XML / 资源引用 / A11y 方法 / Java 语法（应输出 0 问题）
pip install javalang        # 可选，用于 Java AST 语法解析
python tools/verify_a11y.py --strict

# 3. 改造前后对比报告
#    docs/a11y-audit-baseline.md  （改造前：151 项问题，110 个 error）
#    docs/a11y-audit-report.md    （改造后：0 项问题）
```

| 审计报告 | error | warning | info | 合计 |
| --- | --- | --- | --- | --- |
| 改造前（原项目） | 110 | 11 | 30 | 151 |
| 改造后（本仓库） | 0 | 0 | 0 | 0 |

> 本机未安装 Android SDK / 模拟器，故 **未执行 Gradle 编译与真机 TalkBack 实测**。
> 需在真机 / 模拟器上完成的测试用例、步骤与预期结果见
> [`docs/测试记录.md`](docs/测试记录.md)，已如实标注，未编造实测结论。

## 四、目录结构

```
app/src/main/
├── java/com/example/homework/
│   ├── util/A11y.java                 # 新增：无障碍适配工具类（无业务逻辑）
│   ├── MainActivity.java              # 导航角色/选中态、页面切换播报、角标语义
│   ├── ui/DiscoveryFragment.java      # 搜索框、分类格子、信息流 Tab 语义
│   ├── ui/*Adapter.java               # 列表图片 / 头像 / 复选框动态语义
│   └── ...                            # 其余业务代码：仅 Toast -> A11y.toast
├── res/
│   ├── values/a11y_strings.xml        # 新增：全部无障碍文案（@string）
│   ├── values/colors.xml              # 低视力对比度优化
│   └── layout/*.xml                   # 语义标签 / 焦点 / 触摸目标 / 键盘顺序
docs/
├── 适配说明.md                        # 逐条对应任务书要求的修改点清单
├── 测试记录.md                        # 测试工具、自动验证结果、真机复验用例
├── 主贴文案.md                        # 可直接复制到课程平台的主贴内容
├── 变更清单.md                        # 改造前后逐文件 diff，佐证未新增功能
├── a11y-audit-baseline.md             # 改造前静态审计基线
├── a11y-audit-report.md               # 改造后静态审计报告
└── verification-report.md             # 综合静态校验报告
tools/
├── a11y_audit.py                      # 无障碍静态审计（A11Y-001 ~ A11Y-010）
├── verify_a11y.py                     # XML / 资源 / Java 语法综合校验
├── apply_a11y_patch.py                # 一次性改造脚本（改造记录，可复现）
└── apply_a11y_java.py                 # Java 运行时适配补丁（改造记录，可复现）
```

## 五、文档索引

- [`docs/适配说明.md`](docs/适配说明.md)：修改点、规范依据、技术实现
- [`docs/测试记录.md`](docs/测试记录.md)：测试工具、验证数据、真机复验用例
- [`docs/主贴文案.md`](docs/主贴文案.md)：课程平台提交用主贴（含仓库链接）
- [`docs/变更清单.md`](docs/变更清单.md)：改造前后 diff 汇总

---

作者：姚世文 ｜ 课程：《软件工程》期末专项任务（揭榜任务二）
