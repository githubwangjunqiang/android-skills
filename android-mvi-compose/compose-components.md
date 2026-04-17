# Compose 通用 UI 组件与屏幕适配

本文件是 android-mvi-compose 技能的附属参考文件，包含 ResponsiveScaffold 完整实现、通用 UI 组件和屏幕适配规范。

---

## 一、屏幕适配规范（375 设计稿 + WindowSizeClass）

### 1.1 核心思路

使用 Google 官方 `WindowSizeClass`（Material3）根据屏幕宽度分档，适配各种设备屏幕。

#### 为什么 375 设计稿可以直接使用 dp？

| 设备类型 | 屏幕宽度 (dp) | 与 375 差距 |
|------|:---:|:---:|
| iPhone SE / 小屏安卓 | 320~360 dp | -4% ~ -15% |
| 主流安卓（大多数） | 360~412 dp | -4% ~ +10% |
| 大屏安卓 / 折叠屏内屏 | 412~600 dp | +10% ~ +60% |

**结论**：主流手机 360~412dp 与设计稿 375dp 差距仅 ±10%，Compose Direct 布局系统可以直接使用设计稿 dp 标注值。大屏/折叠屏/平板可通过 `WindowSizeClass` 做响应式布局。

### 1.2 ResponsiveScaffold（屏幕适配封装）

对 Scaffold 的封装，处理不同屏幕下的布局：

```kotlin
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.activity.ComponentActivity

/**
 * 响应式 Scaffold 组件
 * - 手机竖屏：全屏显示，使用设计稿标注值
 * - 大屏/平板：限制内容宽度并居中显示（max = 600.dp）
 * ⚠️ 包名需替换为项目实际包名
 */
@Composable
fun ResponsiveScaffold(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val windowSizeClass = calculateWindowSizeClass(LocalContext.current as ComponentActivity)

    Scaffold(
        topBar = {
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                topBar()
            }
        },
        bottomBar = {
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                bottomBar()
            }
        },
        floatingActionButton = {
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                floatingActionButton()
            }
        }
    ) { padding ->
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> {
                // 手机竖屏：全屏内容
                content(padding)
            }
            else -> {
                // 大屏：限制内容宽度并居中
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                    ) {
                        content(PaddingValues(20.dp))
                    }
                }
            }
        }
    }
}
```

### 1.4 开发技巧

```kotlin
// ✅ 直接使用设计稿标注值
@Composable
fun UserCard() {
    Column {
        Modifier
            .fillMaxWidth()
            .height(120.dp)           // 设计稿高120dp就写120dp
            .padding(horizontal = 16.dp, vertical = 12.dp)  // 设计稿标注就直接用

        Text(
            text = "标题",
            fontSize = 16.sp,      // 设计稿16sp就用16sp
        )
    }
}

// ✅ 用 ResponsiveScaffold 作为页面根布局
@Composable
fun MainScreen() {
    ResponsiveScaffold(
        topBar = { TopBar() },
        bottomBar = { BottomBar() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),  // 处理状态栏等安全区域
            contentPadding = PaddingValues(16.dp)  // 设计稿间距
        ) {
            items(data) { item ->
                UserCard(item = item)  // 内部仍然使用设计稿尺寸
            }
        }
    }
}
```

---

## 二、通用 UI 组件

### 2.1 通用 TopBar

```kotlin
/**
 * 通用页面顶部栏
 * ⚠️ 包名需替换为项目实际包名
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        },
        actions = actions
    )
}
```

### 2.2 通用确认弹窗

```kotlin
/**
 * 通用确认弹窗
 * ⚠️ 包名需替换为项目实际包名
 */
@Composable
fun ConfirmDialog(
    title: String,
    content: String,
    confirmText: String = "确定",
    cancelText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(content) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelText) } }
    )
}
```

### 2.3 通用 BottomSheet

```kotlin
/**
 * 通用底部弹出面板
 * ⚠️ 包名需替换为项目实际包名
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                HorizontalDivider()
            }
            content()
        }
    }
}
```

### 2.4 通用搜索栏

```kotlin
/**
 * 通用搜索输入框
 * ⚠️ 包名需替换为项目实际包名
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "搜索",
    onSearch: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "清除")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke(query) })
    )
}
```

---

## 三、屏幕适配速查表

| 场景 | 方案 |
|------|------|
| 手机竖屏 | 直接用设计稿 dp/sp 标注值 |
| 宽度 | 优先 `fillMaxWidth()` + 限制 `widthIn(max=xxx.dp)` + 居中排列 |
| 图片/卡片比例 | 用 `aspectRatio` 保持长宽比 |
| 列表网格 | 用 `GridCells.Adaptive` 自适应列数 |
| 大屏/平板 | 响应式适配 + `widthIn(max=600.dp).align(Alignment.TopCenter)` |