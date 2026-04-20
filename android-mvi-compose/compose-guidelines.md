# Compose 开发规范

本文件是 android-mvi-compose 技能的附属参考文件，包含 Navigation、互操作、主题、性能优化和数据流规范。

---

## 一、Navigation（类型安全路由）

Compose Navigation 2.8+ 使用 Kotlin Serialization 定义路由：

```kotlin
// ⚠️ 包名需替换为项目实际包名

// 定义路由
@Serializable data object HomeRoute
@Serializable data class DetailRoute(val id: String)

// 配置导航
NavHost(navController = navController, startDestination = HomeRoute) {
    composable<HomeRoute> { HomeScreen(navController) }
    composable<DetailRoute> { backStackEntry ->
        DetailScreen(id = backStackEntry.toRoute<DetailRoute>().id)
    }
}

// 导航调用
navController.navigate(DetailRoute(id = "123"))
```

---

## 二、Compose 与 View 互操作

### 2.1 Compose 中嵌入传统 View

```kotlin
AndroidView(
    factory = { context -> PlayerView(context) },
    modifier = Modifier.fillMaxSize(),
    update = { playerView -> playerView.player = exoPlayer }
)
```

### 2.2 传统 Activity/Fragment 中嵌入 Compose

```kotlin
// Activity
setContent { MaterialTheme { XxxScreen() } }

// Fragment
override fun onCreateView(...) = ComposeView(requireContext()).apply {
    setContent { MaterialTheme { XxxScreen() } }
}
```

---

## 三、主题与样式

```kotlin
/**
 * 应用主题
 * ⚠️ 包名需替换为项目实际包名
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
```

---

## 四、性能优化要点

| 优化点 | 说明 | 示例 |
|--------|------|------|
| LazyColumn key | 列表更新时只重组变化的项 | `items(list, key = { it.id })` |
| remember 缓存 | 避免每次重组重新计算 | `remember(timestamp) { format(timestamp) }` |
| derivedStateOf | 减少不必要的状态更新 | `derivedStateOf { listState.firstVisibleItemIndex > 0 }` |
| drawBehind | 适用于自定义绘制背景或减少额外图层 | `Modifier.drawBehind { drawRect(color) }` |

```kotlin
// ✅ LazyColumn 必须提供 key
items(items = list, key = { it.id }) { item -> ItemView(item) }

// ✅ remember 缓存计算结果
val formatted = remember(timestamp) { timestamp.format("yyyy-MM-dd") }

// ✅ derivedStateOf 减少重组
val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

// ✅ 需要自定义绘制时可使用 drawBehind
Modifier.drawBehind { drawRect(color) }
```

---

## 五、数据流选型

| 类型 | 方向 | 用途 | 使用场景 |
|------|------|------|----------|
| 直接函数调用 | View → VM | Intent | `handleIntent()` |
| `StateFlow` | VM → View | 持续状态 | `uiState` |
| `Channel` | VM → View | 一次性事件 | `effect`（导航、滚动等） |
| `SharedFlow` | 跨模块 | 广播事件 | 全局事件总线 |

---

## 六、反模式速查

> **详细反模式见 `android-coroutines` skill 的“协程反模式与禁止事项”章节**

| ❌ 反模式 | ✅ 正确替代 |
|----------|-------------|
| `GlobalScope.launch` | `viewModelScope.launch` |
| 在简单静态背景场景滥用 `drawBehind` | 优先 `Modifier.background()`，需要自定义绘制时再用 `drawBehind` |
| LazyColumn 无 key | `items(list, key = { it.id })` |
| ViewModel 存 Context | `SavedStateHandle`、参数下沉或上层注入 |
| Repository 返回 LiveData | 返回 `suspend fun` 或 `Flow` |
