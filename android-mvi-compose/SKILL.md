---
name: android-mvi-compose
description: MVI 架构与 Compose 核心规范。创建页面、编写 ViewModel 时使用。
---

# MVI 架构与 Compose 开发规范

本文件包含 MVI 核心定义模板。**完整基类实现和状态页组件见附属文件**。

---

## 一、MVI 基础设施

### 1.1 核心概念

| 组件 | 说明 |
|------|------|
| `MviPageStatus` | 页面框架状态密封接口 |
| `MviUiState<T>` | 通用状态容器（status + data） |
| `MviBaseViewModel` | ViewModel 基类 |

### 1.2 状态类型

- `Content` — 内容正常显示
- `FullScreenLoading` — 全屏加载
- `DialogLoading` — 弹窗加载
- `Error` — 错误状态
- `Empty` — 空状态

> **完整基类实现见 [MviInfrastructure.kt](MviInfrastructure.kt)**

---

## 二、点击防抖处理（强制规范）

**⚠️ 所有点击事件必须使用防抖处理**，默认 500ms 内重复点击只响应第一次。

| 组件类型 | 防抖方式 |
|----------|----------|
| Modifier（Column/Box 等） | `Modifier.throttleClick(onClick = {...})` |
| Button/IconButton | `onClick = rememberThrottleOnClick(onClick = {...})` |

> **防抖 Modifier 和函数定义见 [ComposeStatusComponents.kt](ComposeStatusComponents.kt)**

---

## 三、Intent/PageData/Effect 定义模板

### 3.1 Intent（用户意图）

```kotlin
sealed interface UserListIntent {
    data object LoadData : UserListIntent
    data object Refresh : UserListIntent
    data class Search(val query: String) : UserListIntent
    data class Delete(val userId: String) : UserListIntent
}
```

### 3.2 PageData（页面专属数据）

```kotlin
data class UserListData(
    val isRefreshing: Boolean = false,
    val userList: List<UserData> = emptyList(),
    val searchQuery: String = ""
) {
    val isEmpty: Boolean get() = userList.isEmpty()
}
```

### 3.3 Effect（一次性副作用）

```kotlin
sealed interface UserListEffect {
    data class NavigateToDetail(val userId: String) : UserListEffect
    data object NavigateBack : UserListEffect
    data object ScrollToTop : UserListEffect
}
// 注意：Toast 直接在 ViewModel 调用 "msg".show()
```

---

## 四、ViewModel 模板

```kotlin
class UserListViewModel : MviBaseViewModel<UserListIntent, UserListData, UserListEffect>(UserListData()) {

    override fun handleIntent(intent: UserListIntent) {
        when (intent) {
            is UserListIntent.LoadData -> loadData(isRefresh = false)
            is UserListIntent.Refresh -> loadData(isRefresh = true)
            is UserListIntent.Search -> searchUser(intent.query)
            is UserListIntent.Delete -> deleteUser(intent.userId)
        }
    }

    private fun loadData(isRefresh: Boolean) {
        launchTryViewModelScope {
            if (isRefresh) {
                setState { it.copy(data = it.data.copy(isRefreshing = true)) }
            } else {
                showFullScreenLoading("正在加载...")
            }
            // 网络请求...
        }
    }
}
```

---

## 五、Compose 状态页组件

| 组件 | 用途 |
|------|------|
| `LoadingScreen` | 全屏加载页 |
| `EmptyScreen` | 空数据页（支持重试） |
| `ErrorScreen` | 错误页（支持重试） |
| `LoadingDialog` | 弹窗加载 |

> **完整组件实现见 [ComposeStatusComponents.kt](ComposeStatusComponents.kt)**

---

## 六、页面状态渲染示例

> **⚠️ 示例中省略了防抖处理，实际使用时必须添加**
> 参见 [compose-page-templates.md](compose-page-templates.md) 的完整模板

```kotlin
@Composable
fun UserListPage(viewModel: UserListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ⚠️ 实际使用时需添加防抖：rememberThrottleOnClick
    val throttledRetry = rememberThrottleOnClick(onClick = { viewModel.handleIntent(UserListIntent.LoadData) })

    when (uiState.status) {
        MviPageStatus.Content -> UserListContent(uiState.data)
        MviPageStatus.FullScreenLoading -> LoadingScreen()
        MviPageStatus.DialogLoading -> LoadingDialog(uiState.status.text)
        is MviPageStatus.Error -> ErrorScreen(
            msg = uiState.status.message,
            iconRes = R.drawable.ic_error_default,
            onRetry = throttledRetry
        )
        is MviPageStatus.Empty -> EmptyScreen(
            msg = uiState.status.message,
            iconRes = R.drawable.ic_empty_default,
            onRetry = throttledRetry
        )
    }
}
```

---

## 七、附属文件导航

| 文件 | 内容 | 使用场景 |
|------|------|----------|
| [compose-page-templates.md](compose-page-templates.md) | 页面模板 + Scaffold 规范 | 创建新 Compose 页面时参考 |
| [compose-components.md](compose-components.md) | ResponsiveScaffold + 通用 UI 组件 | 屏幕适配、使用 TopBar/弹窗等组件 |
| [compose-guidelines.md](compose-guidelines.md) | Navigation + 性能优化 + 数据流 | 开发规范和性能优化 |
| [MviInfrastructure.kt](MviInfrastructure.kt) | MVI 基类实现 | 复制 ViewModel 基类代码 |
| [ComposeStatusComponents.kt](ComposeStatusComponents.kt) | 状态页组件 + 防抖点击 Modifier | 复制 Loading/Empty/Error 组件和防抖处理 |

**创建 Compose 页面的推荐流程**：
1. 先看 `compose-page-templates.md` 复制页面模板
2. 需要屏幕适配时查看 `compose-components.md` 的 ResponsiveScaffold
3. 遵守 `compose-guidelines.md` 的性能优化和 Navigation 规范