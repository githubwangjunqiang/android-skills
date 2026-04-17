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
 * ⚠️ 包名需替换为项目实际包名
 */
@Composable
fun UserListScreen(
    viewModel: UserListViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    // 1. 状态和 Effect 的监听代码
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pageData = state.data // 页面专属数据

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserListEffect.NavigateToDetail -> onNavigateToDetail(effect.userId)
                is UserListEffect.NavigateBack -> onNavigateBack()
                is UserListEffect.ScrollToTop -> { /* 滚动到顶部 */ }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(UserListIntent.LoadData)
    }

    // 2. 使用 ResponsiveScaffold 作为页面根布局，自动处理屏幕适配
    ResponsiveScaffold(
        topBar = {
            AppTopBar(
                title = "用户列表",
                onBack = onNavigateBack
            )
        }
    ) { padding -> // ResponsiveScaffold 会提供正确的内边距

        // 3. Box 用于处理 DialogLoading 的叠加
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // 使用来自 Scaffold 的 padding
        ) {
            // 4. 根据页面状态显示不同内容
            when (val status = state.status) {
                is MviPageStatus.Content,
                is MviPageStatus.DialogLoading -> UserListContent( // DialogLoading时也显示内容
                    pageData = pageData,
                    onIntent = viewModel::handleIntent
                )
                is MviPageStatus.FullScreenLoading -> LoadingScreen(text = status.text)
                is MviPageStatus.Error -> ErrorScreen(
                    msg = status.message,
                    iconRes = status.icon ?: R.drawable.ic_error_default,  // ⚠️ 需替换为项目实际资源
                    onRetry = { viewModel.handleIntent(UserListIntent.LoadData) }
                )
                is MviPageStatus.Empty -> EmptyScreen(
                    msg = status.message,
                    iconRes = status.icon ?: R.drawable.ic_empty_default,  // ⚠️ 需替换为项目实际资源
                    onRetry = { viewModel.handleIntent(UserListIntent.LoadData) }
                )
            }

            // 5. 单独处理 DialogLoading 的叠加显示
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
    Scaffold { padding ->  // 第一层 padding
        UserListContent(Modifier.padding(padding))
    }
}

@Composable
fun UserListContent(modifier: Modifier) {
    // ❌ 错误：子组件又嵌套 Scaffold
    Scaffold(modifier = modifier) { innerPadding ->  // 第二层 padding，导致边距翻倍
        LazyColumn(modifier = Modifier.padding(innerPadding)) { }
    }
}
```