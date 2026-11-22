# Android 无障碍静态审计报告

> 由 `tools/a11y_audit.py` 自动生成，可重复执行验证。

## 概览

| 指标 | 数值 |
| --- | --- |
| 扫描布局文件 | 28 |
| 问题总数 | 151 |
| error（阻断） | 110 |
| warning | 13 |
| info | 28 |
| 结论 | 未通过（存在 error 级问题） |

## 按规则统计

| 规则 | 说明 | 数量 | 级别 |
| --- | --- | --- | --- |
| A11Y-001 | 图片控件缺少无障碍标签 | 30 | error |
| A11Y-002 | contentDescription 硬编码 | 2 | warning |
| A11Y-003 | 输入框缺少 hint / labelFor | 0 | error |
| A11Y-004 | 可点击控件无可朗读内容 | 3 | error |
| A11Y-005 | 触摸目标小于 48dp | 11 | warning |
| A11Y-006 | 文本尺寸使用 dp（不支持字体缩放） | 0 | warning |
| A11Y-007 | 引用的资源不存在 | 0 | error |
| A11Y-008 | 可点击控件未声明可聚焦 | 1 | error |
| A11Y-009 | 表单缺少 nextFocusForward | 28 | info |
| A11Y-010 | Java 绑定点击但布局未声明可聚焦 | 76 | error |

## 按文件统计

| 布局文件 | 问题数 |
| --- | --- |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 21 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 19 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 11 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 11 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 11 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | 11 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_profile.xml` | 9 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_register.xml` | 8 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_chat.xml` | 5 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_login.xml` | 5 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_amap_place_picker.xml` | 4 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_edit_profile.xml` | 4 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_new_post.xml` | 4 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_item_detail.xml` | 3 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_settings.xml` | 3 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_change_password.xml` | 3 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_reset_password.xml` | 3 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_cart_entry.xml` | 3 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_my_listing.xml` | 3 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_order_card.xml` | 3 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_community.xml` | 2 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_community_post.xml` | 2 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_conversation.xml` | 2 |
| `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_marketplace_card.xml` | 1 |

## 问题明细

| 级别 | 规则 | 文件 | 行 | 控件 | 说明 |
| --- | --- | --- | --- | --- | --- |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_amap_place_picker.xml` | 26 | `@+id/btn_place_back` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_amap_place_picker.xml` | 63 | `@+id/et_place_keyword` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_amap_place_picker.xml` | 79 | `@+id/btn_place_search` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_amap_place_picker.xml` | 115 | `@+id/btn_place_manual` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_chat.xml` | 16 | `@+id/btn_chat_back` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_chat.xml` | 50 | `@+id/btn_chat_more` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_chat.xml` | 133 | `@+id/tv_chat_empty_state` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_chat.xml` | 165 | `@+id/et_chat_input` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_chat.xml` | 178 | `@+id/btn_send_message` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_community.xml` | 16 | `@+id/btn_community_back` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_community.xml` | 36 | `@+id/btn_new_post` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_edit_profile.xml` | 23 | `@+id/btn_edit_profile_back` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_edit_profile.xml` | 59 | `@+id/et_edit_name` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_edit_profile.xml` | 96 | `@+id/et_edit_interests` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_edit_profile.xml` | 117 | `@+id/et_edit_bio` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_item_detail.xml` | 17 | `@+id/btn_back` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_item_detail.xml` | 38 | `@+id/btn_detail_help` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_item_detail.xml` | 63 | `@+id/iv_detail_image` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_login.xml` | 66 | `@+id/et_username` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_login.xml` | 94 | `@+id/tv_forgot_password` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_login.xml` | 104 | `@+id/et_password` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_login.xml` | 157 | `@+id/btn_sso_login` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_login.xml` | 175 | `@+id/tv_register` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 46 | `@+id/nav_blur_background` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 69 | `@+id/nav_discovery` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 77 | `@+id/nav_messages` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 85 | `@+id/nav_discovery_icon` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| warning | A11Y-002 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 116 | `@+id/nav_post` | contentDescription 应引用 @string 资源，当前为硬编码：发布闲置 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 123 | `@+id/nav_messages_icon` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 175 | `@+id/nav_cart` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 193 | `@+id/nav_post_icon` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 229 | `@+id/nav_profile` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 236 | `@+id/nav_cart_icon` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_main.xml` | 293 | `@+id/nav_profile_icon` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | - | `ImageView` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 15 | `@+id/btn_back` | ImageButton 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 15 | `@+id/btn_back` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 74 | `@+id/et_order_search` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 100 | `@+id/tab_orders_all` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 113 | `@+id/tab_orders_pending` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 127 | `@+id/tab_orders_shipped` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 141 | `@+id/tab_orders_completed` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 168 | `@+id/tab_sort_recent` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 182 | `@+id/tab_sort_price_high` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_purchases.xml` | 197 | `@+id/tab_sort_price_low` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | - | `ImageView` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 15 | `@+id/btn_back` | ImageButton 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 15 | `@+id/btn_back` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 74 | `@+id/et_order_search` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 100 | `@+id/tab_orders_all` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 113 | `@+id/tab_orders_pending` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 127 | `@+id/tab_orders_shipped` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 141 | `@+id/tab_orders_completed` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 168 | `@+id/tab_sort_recent` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 182 | `@+id/tab_sort_price_high` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_my_sales.xml` | 197 | `@+id/tab_sort_price_low` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_register.xml` | 68 | `@+id/et_name` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_register.xml` | 89 | `@+id/et_register_username` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_register.xml` | 110 | `@+id/et_register_password` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_register.xml` | 169 | `@+id/tv_upload_id` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_register.xml` | 180 | `@+id/tv_sso_entry` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_register.xml` | 288 | `@+id/et_interests` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_register.xml` | 299 | `@+id/et_bio` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_register.xml` | 322 | `@+id/tv_login` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_settings.xml` | 23 | `@+id/btn_settings_back` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_settings.xml` | 67 | `@+id/row_change_password` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/activity_settings.xml` | 95 | `@+id/row_clear_image_cache` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_change_password.xml` | 11 | `@+id/et_old_password` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_change_password.xml` | 22 | `@+id/et_new_password` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_change_password.xml` | 34 | `@+id/et_confirm_password` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_new_post.xml` | 11 | `@+id/et_post_content` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_new_post.xml` | 24 | `@+id/tv_select_post_image` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_new_post.xml` | 36 | `@+id/iv_selected_post_image` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_new_post.xml` | 36 | `@+id/iv_selected_post_image` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_reset_password.xml` | 11 | `@+id/et_reset_username` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_reset_password.xml` | 21 | `@+id/et_reset_password` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/dialog_reset_password.xml` | 33 | `@+id/et_reset_confirm_password` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 33 | `@+id/btn_scan` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-004 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 45 | `@+id/layout_search_box` | LinearLayout 可点击但无 text / contentDescription，TalkBack 只会朗读「按钮」 |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 45 | `@+id/layout_search_box` | layout_height=44dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |
| error | A11Y-008 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 45 | `@+id/layout_search_box` | LinearLayout 可点击但未设置 android:focusable="true"，键盘/方向键无法聚焦 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 45 | `@+id/layout_search_box` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 59 | `@+id/iv_search_icon` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 59 | `@+id/iv_search_icon` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 66 | `@+id/et_search` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 85 | `@+id/iv_search_camera` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 85 | `@+id/iv_search_camera` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 114 | `@+id/btn_local_search_toggle` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 126 | `@+id/btn_ai_search_toggle` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 163 | `@+id/tab_follow` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 187 | `@+id/tv_tab_recommend` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 197 | `@+id/tv_tab_latest` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 568 | `@+id/btn_ai_recommend_toggle` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 580 | `@+id/btn_local_recommend_toggle` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 690 | `@+id/tv_empty_state` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_discovery.xml` | 703 | `@+id/btn_empty_reset_filters` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | - | `ImageView` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | - | `ImageView` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | 40 | `@+id/tv_header_unread_badge` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | 58 | `@+id/layout_unread_summary` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | 121 | `@+id/tv_summary_toggle_unread` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | 154 | `@+id/et_message_search` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | 172 | `@+id/tab_all` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | 182 | `@+id/tab_buying` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | 192 | `@+id/tab_selling` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | 238 | `@+id/fab_new_message` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_message.xml` | 238 | `@+id/fab_new_message` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | - | `ImageView` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | - | `ImageView` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 85 | `@+id/layout_upload_photos` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 124 | `@+id/iv_preview_1` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 130 | `@+id/btn_remove_preview_1` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 156 | `@+id/iv_preview_2` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 162 | `@+id/btn_remove_preview_2` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 188 | `@+id/iv_preview_3` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 205 | `@+id/btn_remove_preview_3` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 238 | `@+id/et_post_title` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 259 | `@+id/et_post_description` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 279 | `@+id/btn_ai_post_assist` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 331 | `@+id/sp_category` | layout_height=32dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 370 | `@+id/et_post_price` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 383 | `@+id/btn_ai_price_assist` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 473 | `@+id/et_post_condition` | layout_height=36dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 473 | `@+id/et_post_condition` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 516 | `@+id/et_post_location` | layout_height=36dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |
| info | A11Y-009 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 516 | `@+id/et_post_location` | EditText 未指定 nextFocusForward，键盘 Tab 顺序依赖布局推断 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 529 | `@+id/btn_pick_location_amap` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_post.xml` | 666 | `@+id/btn_close_pro_tip` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_profile.xml` | 45 | `@+id/btn_profile_settings` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| warning | A11Y-002 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_profile.xml` | 66 | `@+id/layout_profile_avatar` | contentDescription 应引用 @string 资源，当前为硬编码：更换头像 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_profile.xml` | 74 | `@+id/iv_profile_avatar` | ShapeableImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_profile.xml` | 131 | `@+id/btn_edit_profile` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-004 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_profile.xml` | 152 | `@+id/card_purchases` | LinearLayout 可点击但无 text / contentDescription，TalkBack 只会朗读「按钮」 |
| error | A11Y-004 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_profile.xml` | 198 | `@+id/card_sales` | LinearLayout 可点击但无 text / contentDescription，TalkBack 只会朗读「按钮」 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_profile.xml` | 362 | `@+id/row_account_listings` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_profile.xml` | 394 | `@+id/row_account_community` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-010 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/fragment_profile.xml` | 426 | `@+id/row_account_settings` | Java 中绑定了点击监听，但布局未声明 android:focusable="true"，键盘/方向键无法到达 |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_cart_entry.xml` | 20 | `@+id/iv_cart_item_image` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_cart_entry.xml` | 89 | `@+id/btn_cart_item_contact` | layout_height=36dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_cart_entry.xml` | 102 | `@+id/btn_cart_item_remove` | layout_height=36dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_community_post.xml` | 69 | `@+id/iv_post_image` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_community_post.xml` | 77 | `@+id/btn_delete_post` | layout_height=36dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_conversation.xml` | 18 | `@+id/iv_conversation_avatar` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_conversation.xml` | 119 | `@+id/iv_conversation_item_image` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_marketplace_card.xml` | 18 | `@+id/iv_item_image` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_my_listing.xml` | 18 | `@+id/iv_my_item_image` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_my_listing.xml` | 65 | `@+id/btn_edit_item` | layout_height=32dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_my_listing.xml` | 79 | `@+id/btn_delete_item` | layout_height=32dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |
| error | A11Y-001 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_order_card.xml` | 23 | `@+id/iv_order_thumb` | ImageView 既无 contentDescription，也未标记 importantForAccessibility="no"（装饰性） |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_order_card.xml` | 107 | `@+id/btn_order_secondary` | layout_height=40dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |
| warning | A11Y-005 | `../.workbuddy/repos/Android-APP/app/src/main/res/layout/item_order_card.xml` | 120 | `@+id/btn_order_primary` | layout_height=40dp 小于 48dp 最小触摸目标（可用 minWidth/minHeight 补足） |

## 规则依据

- A11Y-001/002/004：Android 官方「Make apps more accessible」——为所有非文本内容提供 `contentDescription`，装饰性图片设 `importantForAccessibility="no"`。
- A11Y-003：`EditText` 需 `hint` 或用 `labelFor` 关联可见标签。
- A11Y-005/006：Material Accessibility / WCAG 2.1 —— 触摸目标至少 48dp，文字用 sp 以跟随系统字体缩放。
- A11Y-008：所有可交互控件必须可由键盘 / 方向键聚焦（`android:focusable="true"`）。
- A11Y-009：键盘导航顺序应显式可控（`nextFocusForward` / `nextFocusDown` 等）。
- A11Y-010：Java 中通过 `setOnClickListener` 绑定交互的控件，同样必须在布局中声明可聚焦（静态审计关联 Java 与 XML）。

> 静态审计无法覆盖 TalkBack 实际朗读效果、焦点移动顺序的动态表现、
> 弹窗与列表滚动播报、系统字体放大后的布局裁切等，需按 `docs/测试记录.md` 在真机/模拟器上人工复验。
