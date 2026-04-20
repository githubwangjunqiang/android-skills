---
name: android-project-overview
description: Android 项目总纲。定义默认技术路线、强制规范、统一命名基准与 skill 路由。进入项目时优先阅读。
---

# Android 项目总纲

本文件是整个 Android skills 体系的统一入口。  
当其他 skill 与本文件冲突时，以本文件为最高优先级。

---

## 一、默认技术路线

本项目默认采用以下技术路线：

- 语言：Kotlin
- UI：Jetpack Compose 优先，传统 View 仅用于存量页面或互操作场景
- 架构：MVI
- 异步：Coroutines + Flow
- 本地存储：MMKV / Room
- 网络：项目封装的 OkHttp 调用链
- 图片加载：项目统一图片加载封装
- 依赖管理与工程脚本：按项目现有结构接入

以上为默认路线。若项目已有成熟存量实现，应优先兼容现有实现，而不是强行重构。

---

## 二、强制规范

以下规则为生成代码时必须优先遵守的强制规范：

### 2.1 统一架构规范
- 新页面默认按 **Compose + MVI** 组织
- 优先复用现有基类、状态模型、工具函数
- 不重复创建与现有体系冲突的基础设施

### 2.2 代码中文注释规范
所有生成代码必须包含中文注释，至少满足以下要求：

- 类：使用 KDoc 说明用途
- 公共方法：使用 KDoc 说明作用、参数、返回值
- 关键逻辑：使用行内中文注释解释意图
- 常量：说明业务含义
- sealed class / sealed interface 的子类型：说明状态或事件含义

禁止只写无意义注释，例如：
- `// 初始化`
- `// 点击事件`
- `// 请求接口`

注释应说明“为什么这样做”或“该结构承担什么职责”。

### 2.3 用户可见文案规范
- 用户可见文本优先资源化
- 示例中的硬编码文案仅用于演示结构，落地时应迁移到 `strings.xml`

### 2.4 命名与风格规范
- 类名：PascalCase
- 函数 / 变量：camelCase
- 常量：UPPER_SNAKE_CASE
- 缩进：4 空格
- 导入：显式导入，避免通配符导入

---

## 三、统一命名与实现基准

生成代码时默认遵循以下基准：

- ViewModel 基类：`MviBaseViewModel`
- 页面状态容器：`MviUiState<T>`
- 页面状态类型：`MviPageStatus`
- 状态更新入口：`setState { ... }`
- 页面状态处理方式：以 `android-mvi-compose/MviInfrastructure.kt` 当前定义为准

除非项目内已有明确存量实现，否则禁止混用以下旧命名：

- `BaseViewModel`
- `UIStatus`
- `showLoadingView()`
- `showErrorLayout()`

如果多个 skill 中出现旧写法与新写法冲突，优先采用本节定义的统一基准。

---

## 四、最高优先级依赖来源

以下文件是对应能力的优先参考来源：

1. 页面状态管理：`android-mvi-compose/MviInfrastructure.kt`
2. Compose 状态页组件：`android-mvi-compose/ComposeStatusComponents.kt`
3. ViewModel 协程扩展：`android-utils-core/data-flow-tools.md`
4. 非生命周期组件协程：`android-coroutines/SKILL.md`
5. 本地存储：`android-local-storage/SKILL.md`
6. 工具类扩展：`android-utils-core/SKILL.md`

如果示例代码与这些文件的当前定义不一致，以这些优先来源为准。

---

## 五、Skill 路由

按任务选择 skill：

- 新建 Compose 页面 / ViewModel / 状态页：`android-mvi-compose`
- 协程、线程切换、非生命周期任务：`android-coroutines`
- ViewModel 协程扩展、Flow 工具：`android-utils-core`
- 本地存储（MMKV / Room）：`android-local-storage`
- 网络请求接入：`android-network`
- Repository / 崩溃处理 / 国际化 / 深色模式 / 测试：`android-advanced-dev`
- 传统 View / Activity 模板 / 互操作：`android-legacy-view`
- 构建与发布：`android-build-publish`

---

## 六、规则分级说明

### 6.1 强制
必须遵守的项目规范：
- Compose + MVI 为默认新页面方案
- 使用统一状态模型与统一命名基准
- 生成代码必须补充有效中文注释

### 6.2 推荐
默认应优先遵守：
- 优先复用已有工具函数和模板
- 用户可见文案资源化
- 公共能力沉淀到对应 skill，而不是散落在业务代码中

### 6.3 项目专用
以下内容默认视为项目策略，不应无条件推广到所有 Android 项目：
- 禁用 Retrofit
- 指定发布平台或打包流程
- 特定包结构
- 特定构建 task 命名
- 特定日志、上传、发版体系

---

## 七、使用原则

1. 先看总纲，再进入对应子 skill
2. 总纲负责方向、优先级和裁决，不重复展开细节实现
3. 子 skill 负责模板、示例和专项规范
4. 若多个 skill 示例不一致，优先采用“统一命名与实现基准”
5. 若示例明显属于旧体系，应按当前主架构修正后再使用

---

## 八、边界说明

本文件不提供以下内容的完整实现，具体请查看对应 skill：

- 完整页面模板
- 完整网络封装
- 完整 Room / MMKV 工具
- 完整发布脚本
- 完整 Manifest / 资源模板

总纲只负责定义方向、优先级、约束和路由关系。
