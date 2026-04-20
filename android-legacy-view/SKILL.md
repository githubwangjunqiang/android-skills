---
name: android-legacy-view
description: 传统 View 与互操作。Activity 启动模板、状态页容器、View-Compose 互操作。
---

# Android 传统 View 与互操作规范

本文件包含传统 View（XML 布局、Activity）相关的组件和规范。

## 附属参考文件

| 文件 | 内容 |
|------|------|
| [ActivityTemplate.kt](ActivityTemplate.kt) | Activity 启动模板完整代码 |
| [StatusViewLayout.kt](StatusViewLayout.kt) | 传统 View 状态页容器完整代码 |

---

## 一、Activity 启动模板规范

推荐在 `companion object` 中提供统一的 `startActivity()` 方法，便于封装参数和减少重复 Intent 构造代码。

### 1.1 规范要点

1. **新建 Activity 时推荐**同步创建 `startActivity()` 方法
2. **启动 Activity 时**优先使用此方法
3. **Intent Flags 默认模板**：`NEW_TASK | CLEAR_TOP | SINGLE_TOP`，具体是否保留按页面场景决定
4. **参数扩展**：通过添加方法参数和 `putExtra` 实现

> **完整模板见 [ActivityTemplate.kt](ActivityTemplate.kt)**

### 1.2 快速示例

```kotlin
class UserDetailActivity : BaseVMActivity<UserDetailVm>() {
    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"

        fun startActivity(context: Context, userId: String) {
            context.startActivity(Intent(context, UserDetailActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_USER_ID, userId)
            })
        }
    }
}
```

---

## 二、StatusViewLayout 状态页容器

当使用传统 View（非 Compose）布局时，使用此容器管理 Loading / Error / Empty / Content 状态切换。

### 2.1 XML 用法

```xml
<StatusViewLayout android:id="@+id/statusView" ...>
    <!-- 正常内容放这里 -->
    <RecyclerView ... />
</StatusViewLayout>
```

### 2.2 代码用法

```kotlin
statusView.showLoading()
statusView.showContent()
statusView.showEmpty(getString(R.string.empty_data)) { retryLoad() }
statusView.showError(getString(R.string.load_failed)) { retryLoad() }
```

> **完整实现见 [StatusViewLayout.kt](StatusViewLayout.kt)**

---

## 三、View-Compose 互操作

### 3.1 Compose 中嵌入传统 View

```kotlin
AndroidView(
    factory = { context -> PlayerView(context) },
    modifier = Modifier.fillMaxSize(),
    update = { playerView -> playerView.player = exoPlayer }
)
```

### 3.2 传统 Activity / Fragment 中嵌入 Compose

```kotlin
// Activity
setContent { MaterialTheme { XxxScreen() } }

// Fragment
override fun onCreateView(...) = ComposeView(requireContext()).apply {
    setContent { MaterialTheme { XxxScreen() } }
}
```
