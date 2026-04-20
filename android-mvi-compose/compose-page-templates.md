# Compose 页面模板

本文件是 android-mvi-compose 技能的附属参考文件，包含完整的 Compose 页面模板和屏幕适配速查表。

---

## 一、屏幕适配速查表

| 场景 | 方案 |
|------|------|
| 手机竖屏 | 直接用设计稿 dp/sp 标注值 |
| 宽度 | 优先 `fillMaxWidth()` + 限制 `widthIn(max=xxx.dp)` + 居中排列 |
| 图片/卡片比例 | 用 `aspectRatio` 保持长宽比 |
| 列表网格 | 用 `GridCells.Adaptive` 自适应列数 |
| 大屏/平板 | 响应式适配 + `widthIn(max=600.dp).align(Alignment.TopCenter)` |

> **完整的屏幕适配规范和 ResponsiveScaffold 实现见 [compose-components.md](compose-components.md)**

---

## 二、页面模板（使用 ResponsiveScaffold）

```kotlin
/**
 * 用户列表页面
 * 集成了响应式布局 ResponsiveScaffold，确保屏幕适配
 *
 * 依赖前提：
 * 1. ViewModel 基类与状态模型来自 MviInfrastructure.kt
 * 2. LoadingScreen / ErrorScreen / EmptyScreen / LoadingDialog 来自 ComposeStatusComponents.kt
 * 3. ResponsiveScaffold / AppTopBar 来自 compose-components.md
 * ⚠️ 包名需替换为项目实际包名
 */
@Composable
fun UserListScreen(
    viewModel: UserListViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    // 1. 监听状态
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pageData = state.data

    // 2. 监听一次性 Effect
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserListEffect.NavigateToDetail -> onNavigateToDetail(effect.userId)
                is UserListEffect.NavigateBack -> onNavigateBack()
                is UserListEffect.ScrollToTop -> {
                    // 在这里处理滚动到顶部逻辑
                }
            }
        }
    }

    // 3. 首次进入页面时加载数据
    LaunchedEffect(Unit) {
        viewModel.handleIntent(UserListIntent.LoadData)
    }

    ResponsiveScaffold(
        topBar = {
            AppTopBar(
                title = "用户列表",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val status = state.status) {
                is MviPageStatus.Content,
                is MviPageStatus.DialogLoading -> UserListContent(
                    pageData = pageData,
                    onIntent = viewModel::handleIntent
                )
                is MviPageStatus.FullScreenLoading -> LoadingScreen(text = status.text)
                is MviPageStatus.Error -> ErrorScreen(
                    msg = status.message,
                    iconRes = status.icon ?: R.drawable.ic_error_default,
                    onRetry = { viewModel.handleIntent(UserListIntent.LoadData) }
                )
                is MviPageStatus.Empty -> EmptyScreen(
                    msg = status.message,
                    iconRes = status.icon ?: R.drawable.ic_empty_default,
                    onRetry = { viewModel.handleIntent(UserListIntent.LoadData) }
                )
            }

            // DialogLoading 需要在内容层之上额外叠加弹窗
            if (state.status is MviPageStatus.DialogLoading) {
                LoadingDialog(msg = (state.status as MviPageStatus.DialogLoading).text)
            }
        }
    }
}
```

---

## 三、Scaffold 使用规范

**原则：Scaffold 组件仅在页面入口级组件中使用，禁止嵌套使用。**

### 原因
Scaffold 提供的 `paddingValues` 参数已包含状态栏、导航栏等安全区域的内边距。如果多层组件都使用 Scaffold 并应用该 padding，会导致边距重复叠加，造成界面布局异常。

### 正确用法
```kotlin
@Composable
fun UserListScreen() {
    // ✅ 只在入口组件使用 Scaffold
    Scaffold(
        topBar = { AppTopBar(title = "用户列表") }
    ) { paddingValues ->
        // 将 padding 传递给内容区域
        UserListContent(
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun UserListContent(modifier: Modifier = Modifier) {
    // ✅ 子组件不再使用 Scaffold，直接使用传入的 modifier
    LazyColumn(modifier = modifier) {
        // ...
    }
}
```

### 错误示例
```kotlin
@Composable
fun UserListScreen() {
    Scaffold { padding ->
        UserListContent(Modifier.padding(padding))
    }
}

@Composable
fun UserListContent(modifier: Modifier) {
    // ❌ 错误：子组件又嵌套 Scaffold
    Scaffold(modifier = modifier) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) { }
    }
}
```
