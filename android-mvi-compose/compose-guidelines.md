# Compose 开发规范

本文件是 android-mvi-compose 技能的附属参考文件，包含 Navigation、互操作、主题、性能优化和数据流规范。

---

## 一、Navigation（类型安全路由）

Compose Navigation 2.8+ 使用 Kotlin Serialization 定义路由，默认推荐使用 NavHost 进行页面导航。

### 1.1 基础路由定义

```kotlin
// ⚠️ 包名需替换为项目实际包名

// 定义路由：无参数页面使用 data object
@Serializable data object HomeRoute

// 定义路由：带参数页面使用 data class
@Serializable data class DetailRoute(val id: String)
@Serializable data class UserRoute(val userId: String, val name: String)

// 配置导航图
NavHost(navController = navController, startDestination = HomeRoute) {
    composable<HomeRoute> { HomeScreen(navController) }
    composable<DetailRoute> { backStackEntry ->
        // 通过 toRoute 获取路由参数
        val route = backStackEntry.toRoute<DetailRoute>()
        DetailScreen(id = route.id)
    }
}

// 导航调用
navController.navigate(DetailRoute(id = "123"))
```

### 1.2 导航参数传递

**基本类型参数**（自动支持）：
```kotlin
// 支持的类型：String, Int, Long, Float, Double, Boolean
@Serializable data class ProductRoute(val id: String, val count: Int, val price: Double)

// 导航时传参
navController.navigate(ProductRoute(id = "p001", count = 5, price = 99.9))
```

**可选参数与默认值**：
```kotlin
@Serializable data class SearchRoute(
    val query: String,
    val page: Int = 1,           // 可选参数，有默认值
    val sort: String = "desc"    // 可选参数，有默认值
)

// 可省略可选参数
navController.navigate(SearchRoute(query = "keyword"))
navController.navigate(SearchRoute(query = "keyword", page = 2))
```

**复杂对象传递**（需实现 Parcelize 或 Serialization）：
```kotlin
@Parcelize
data class UserInfo(val id: String, val name: String, val avatar: String) : Parcelable

@Serializable data class ProfileRoute(val user: UserInfo)

// 导航时传递对象
navController.navigate(ProfileRoute(user = UserInfo("001", "张三", "url")))
```

### 1.3 导航动画

```kotlin
NavHost(
    navController = navController,
    startDestination = HomeRoute,
    // 全局默认动画
    enterTransition = { slideInHorizontally(animationSpec = tween(300)) },
    exitTransition = { slideOutHorizontally(animationSpec = tween(300)) },
    popEnterTransition = { slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it }) },
    popExitTransition = { slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it }) }
) {
    // 单个页面自定义动画
    composable<DetailRoute>(
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) }
    ) { DetailScreen() }
}
```

**常用动画效果**：
| 动画 | 说明 |
|------|------|
| `slideInHorizontally` | 从右侧滑入 |
| `slideInVertically` | 从下方滑入 |
| `fadeIn` | 淡入 |
| `scaleIn` | 缩放进入 |
| `expandIn` | 展开进入 |

### 1.4 深链接（DeepLink）

**路由定义深链接**：
```kotlin
@Serializable data class ProductRoute(val id: String)

composable<ProductRoute>(
    deepLinks = listOf(
        navDeepLink<ProductRoute>(
            basePath = "https://example.com/product"
        ),
        navDeepLink<ProductRoute>(
            basePath = "myapp://product"
        )
    )
) { ProductScreen() }

// URI 格式：https://example.com/product/{id} 或 myapp://product/{id}
// 实际访问：https://example.com/product/p001 → ProductRoute(id = "p001")
```

**AndroidManifest 配置**：
```xml
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <data android:scheme="https" android:host="example.com"/>
    </intent-filter>
</activity>
```

**测试深链接**：
```bash
# 命令行测试
adb shell am start -a android.intent.action.VIEW -d "myapp://product/p001"
```

### 1.5 嵌套导航图

```kotlin
// 定义嵌套图的路由
@Serializable data object HomeGraph

NavHost(navController, startDestination = HomeGraph) {
    // 嵌套导航图
    navigation<HomeGraph>(startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen(navController) }
        composable<DetailRoute> { DetailScreen(navController) }
        // 嵌套图内的页面共享相同的导航上下文
    }

    // 其他顶级图
    navigation<SettingsGraph>(startDestination = SettingsRoute) {
        composable<SettingsRoute> { SettingsScreen(navController) }
        composable<AboutRoute> { AboutScreen(navController) }
    }
}

// 跳转到嵌套图的起始页
navController.navigate(HomeGraph)
// 跳转到嵌套图内的特定页
navController.navigate(DetailRoute(id = "123"))
```

### 1.6 返回结果处理

**发送返回结果**：
```kotlin
// 目标页面返回时设置结果
@Composable
fun EditScreen(navController: NavHostController) {
    val previousEntry = navController.previousBackStackEntry
    Button(onClick = {
        // 通过 SavedStateHandle 存储返回结果
        previousEntry?.savedStateHandle?.set("result", "编辑完成")
        navController.popBackStack()
    }) { Text("保存并返回") }
}
```

**接收返回结果**（协程方式）：
```kotlin
@Composable
fun HomeScreen(navController: NavHostController) {
    // 监听返回结果
    LaunchedEffect(navController.currentBackStackEntry?.savedStateHandle) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow<String?>("result", null)
            ?.collect { result ->
                if (result != null) {
                    // 处理返回结果
                    "收到结果: $result".show()
                    // 清除结果避免重复接收
                    navController.currentBackStackEntry?.savedStateHandle?.remove<String>("result")
                }
            }
    }

    Button(onClick = { navController.navigate(EditRoute) }) {
        Text("去编辑")
    }
}
```

### 1.7 BottomNavigation 配合 NavHost

```kotlin
// 定义底部 Tab 路由
@Serializable data object HomeTab
@Serializable data object MessageTab
@Serializable data object ProfileTab

// 底部导航栏数据
val bottomItems = listOf(
    BottomItem(HomeTab, "首页", Icons.Default.Home),
    BottomItem(MessageTab, "消息", Icons.Default.Message),
    BottomItem(ProfileTab, "我的", Icons.Default.Person)
)

@Composable
fun MainScreen(navController: NavHostController) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { item ->
                    NavigationBarItem(
                        selected = navController.currentBackStackEntry?.toRoute<Any>() == item.route,
                        onClick = {
                            // 使用 singleTop 避免重复入栈
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeTab,
            modifier = Modifier.padding(padding)
        ) {
            composable<HomeTab> { HomeScreen() }
            composable<MessageTab> { MessageScreen() }
            composable<ProfileTab> { ProfileScreen() }
        }
    }
}
```

### 1.8 导航最佳实践与选型建议

| 场景 | 推荐方式 | 说明 |
|------|----------|------|
| Compose 页面跳转 | NavHost 路由 | 类型安全、支持参数、深链接 |
| 跨 App 跳转 | Intent | 系统级跳转、调用其他应用 |
| 跳转传统 Activity | Intent | 兼容存量传统 View 页面 |
| ViewModel 触发导航 | Effect + navController | 导航逻辑与 UI 解耦 |

**推荐模式**：
```kotlin
// ViewModel 通过 Effect 触发导航
sealed interface HomeEffect {
    data class NavigateToDetail(val id: String) : HomeEffect
    data object NavigateBack : HomeEffect
}

@Composable
fun HomeScreen(viewModel: HomeViewModel, navController: NavHostController) {
    // 监听 Effect 执行导航
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToDetail -> navController.navigate(DetailRoute(effect.id))
                is HomeEffect.NavigateBack -> navController.popBackStack()
            }
        }
    }
}
```

**避免的反模式**：
- ❌ 全局变量传参 → ✅ 使用路由参数传递
- ❌ ViewModel 直接持有 navController → ✅ 通过 Effect 让 UI 层处理导航
- ❌ 硬编码路由字符串 → ✅ 使用 `@Serializable` 类型安全定义

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

> **详细反模式见 `android-coroutines` skill 的"协程反模式与禁止事项"章节**

| ❌ 反模式 | ✅ 正确替代 |
|----------|-------------|
| `GlobalScope.launch` | `viewModelScope.launch` |
| 在简单静态背景场景滥用 `drawBehind` | 优先 `Modifier.background()`，需要自定义绘制时再用 `drawBehind` |
| LazyColumn 无 key | `items(list, key = { it.id })` |
| ViewModel 存 Context | `SavedStateHandle`、参数下沉或上层注入 |
| Repository 返回 LiveData | 返回 `suspend fun` 或 `Flow` |