---
name: android-project-overview
description: Android 项目总纲。技术栈、编码规范、包结构、MVI 概览。新建项目或了解架构时使用。
---

# Android 项目开发技能规范

本文件为 AI 编程工具提供 Android 项目开发规范和技能指导。
所有生成的代码必须严格遵守本文件及 `skills/` 目录下子文件中的规范。

---

## 一、技术栈

- **语言**: Kotlin（非必要不使用 Java）
- **UI 框架**: Jetpack Compose（Material 3）
- **架构模式**: MVI（Model-View-Intent）
- **异步处理**: Kotlin Coroutines + Flow
- **网络库**: OkHttp3 自定义封装（**禁止 Retrofit**）
- **图片加载**: Glide（封装调用）
- **本地缓存**: MMKV + Room
- **JSON 解析**: Gson
- **依赖注入**: 手动单例管理

### 技术选型理由

| 选择 | 理由 |
|------|------|
| OkHttp 而非 Retrofit | 项目已有统一封装；Retrofit 注解增加学习成本 |
| 手动单例而非 Hilt/Koin | 项目规模适中，DI 学习成本高于收益 |
| MMKV 而非 SharedPreferences | SP `commit()` 导致 ANR；MMKV 基于 mmap，无 ANR 风险 |
| MVI 而非 MVVM | 单向数据流更易追踪状态变化 |

---

## 二、强制规范

### 2.1 中文注释要求

**所有代码必须添加中文注释**：
- **类**：KDoc 注释说明用途
- **公共方法**：KDoc 包含 `@param`、`@return`
- **关键逻辑**：行内注释解释意图
- **常量**：注释说明用途
- **sealed class 子类**：每个子类注释含义

### 2.2 包结构规范

```
com.xxx.app/
├── App.kt                  // Application 入口
├── base/                   // 基础架构层（跨项目复用）
│   ├── baseui/             // ViewModel 基类、UIStatus
│   ├── http/               // 网络层封装
│   ├── utils/              // 通用工具类
│   ├── storage/            // MMKV、Room 封装
│   └── view/               // 自定义 View
├── manager/                // 全局管理器
├── ui/                     // Compose UI 层
│   ├── theme/              // 主题、颜色
│   ├── components/         // 通用 Compose 组件
│   └── navigation/         // 导航路由
├── feature/                // 业务功能模块
│   └── home/               // 首页（Screen + ViewModel + Repository）
└── model/                  // 全局数据模型
    ├── response/           // API 响应基类
    └── entity/             // Room 实体
```

**关键原则**：
- `base/` 跨项目可复用
- `feature/` 按业务分包，模块内聚
- 业务模型跟随 feature，响应基类放 `model/`

### 2.3 代码风格

- **缩进**: 4 空格
- **行宽**: 120 字符
- **命名**: 类名 PascalCase，函数/变量 camelCase，常量 UPPER_SNAKE_CASE
- **导入**: 显式导入，禁止通配符
- **SDK 兼容**: 新 API 用 `Build.VERSION.SDK_INT >= XXX` 守卫（Min SDK 23）

### 2.4 点击事件防抖规范（强制）

**所有点击事件必须做防抖处理**，避免用户快速点击导致重复操作。

| 场景 | 防抖间隔 | 用法 |
|------|----------|------|
| 普通按钮（提交、保存） | 500ms | 默认间隔 |
| 轻量操作（点赞、收藏） | 200ms | 快响应 |
| 重操作（支付、下载） | 800ms | 慢响应 |

**传统 View 用法**：
```kotlin
// ✅ 正确：使用防抖扩展函数
btnSubmit.setOnClickListener500 { doSubmit() }
btnLike.setOnClickListener200 { toggleLike() }

// ❌ 错误：直接 setOnClickListener 可能重复触发
btnSubmit.setOnClickListener { doSubmit() }
```

**Compose 用法**：
```kotlin
// ✅ 正确：使用 throttleClick Modifier
Button(
    onClick = {},
    modifier = Modifier.throttleClick { viewModel.handleIntent(Submit) }
)

// ✅ 正确：使用 throttleClick 回调
Button(onClick = throttleClick { submit() }) { Text("提交") }

// ❌ 错误：直接 onClick 可能重复触发
Button(onClick = { submit() }) { Text("提交") }
```

> **防抖实现代码由 AI 自行提供**（OnClickDelayListener、throttleClick Modifier）

### 2.5 依赖管理

使用 Gradle Version Catalog (`libs.versions.toml`)，版本见 `references/dependencies.md`。

---

## 三、MVI 架构概览

**数据流**：View → Intent → ViewModel → State/Effect → View

| 组件 | 说明 |
|------|------|
| Intent | 用户意图（sealed interface） |
| State | 页面状态（data class） |
| Effect | 一次性事件（导航、Toast） |
| ViewModel | 处理 Intent，更新 State/Effect |

> **完整实现见 `android-mvi-compose` 技能**

---

## 四、AndroidManifest 配置

**构建环境**：JDK 21 / Compile SDK 35 / Target SDK 35 / **Min SDK 23**

> **完整配置见附属文件**：
> - [AndroidManifest.xml](AndroidManifest.xml) — 主配置模板
> - [network_security_config.xml](network_security_config.xml) — 网络安全配置
> - [file_paths.xml](file_paths.xml) — FileProvider 路径

---

## 五、buildSrc 构建工具

`buildSrc/` 提供版本管理和 APK 上传能力：
- **VersionConfig** — 版本号自增管理
- **ApkUpLoadUtils** — 蒲公英上传

> **详细说明见 `android-build-publish` 技能**

---

## 六、Skills 文件索引

| 技能 | 内容 |
|------|------|
| `android-mvi-compose` | MVI 模板、Compose 页面、状态页组件、屏幕适配 |
| `android-local-storage` | MMKV、Room、存储选型 |
| `android-utils-core` | Toast、JSON、时间、图片加载、日志、权限 |
| `android-coroutines` | 协程提供者、作用域管理、安全启动方法 |
| `android-advanced-dev` | Repository、异常处理、多语言、深色模式、测试 |
| `android-build-publish` | buildSrc、版本管理、蒲公英上传 |
| `android-legacy-view` | Activity 启动模板、传统 View 状态页 |