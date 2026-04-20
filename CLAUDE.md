# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目性质

这是一个 Android 开发 Skills 规范集合，**不是可运行的 Android 应用**。每个 `android-*` 目录是一个独立的 skill 包，包含 `SKILL.md` 指南和附属的 Kotlin/Markdown 参考文件。

## Skills 结构与路由

Skills 采用分层架构，`android-project-overview` 为总纲，其他 skill 为专项规范：

| Skill | 用途 |
|-------|------|
| `android-project-overview` | 总纲（最高优先级）- 技术栈、强制规范、统一命名基准 |
| `android-mvi-compose` | MVI 基础设施、Intent/State/Effect 模板、ViewModel 模板 |
| `android-coroutines` | 协程提供者、作用域管理、线程切换 |
| `android-network` | OkHttp 扩展函数、请求规范、响应处理 |
| `android-local-storage` | MMKV 键值存储、Room 数据库 |
| `android-utils-core` | Toast、JSON、时间、DP/PX、图片加载、日志 |
| `android-advanced-dev` | Repository 模式、异常处理、多语言、深色模式、测试 |
| `android-legacy-view` | Activity 启动模板、传统 View 状态页、View-Compose 互操作 |
| `android-build-publish` | buildSrc 模块、版本管理、蒲公英上传、APK 命名 |

**总纲裁决规则**：当其他 skill 与 `android-project-overview` 冲突时，以总纲为准。

## 常用开发操作

```bash
# 查看所有 skill 入口文件
ls */SKILL.md

# 查看包结构
find . -maxdepth 2 -type f | sort

# 快速预览 skill 内容
head -50 android-mvi-compose/SKILL.md

# 安装到本地 Claude Code（验证效果）
cp -R . ~/.claude/skills/
```

## 代码风格规范

- Markdown 文档使用清晰的标题层级和任务导向指令
- 文件名采用描述性命名（如 `ThemeConfig.kt`、`compose-guidelines.md`）
- 代码示例使用 4 空格缩进
- 文档使用中文，代码示例遵循 Kotlin 惯例
- Kotlin 代码必须添加有效的中文注释（说明"为什么"而非"是什么"）

## 依赖版本参考

`references/dependencies.md` 提供依赖库选型建议和 `libs.versions.toml` 模板。

## Commit 规范

使用简短的祈使句格式：
- `android-network: refine error handling examples`
- `android-mvi-compose: update ViewModel template`

## 贡献原则

- 保持 skill 内的小型、聚焦编辑
- 避免跨包依赖，除非多个 skill 真正共享同一参考
- 更新示例时保持片段自包含且可直接复制粘贴
- 新增测试指导应放在相关 skill 附近（如 `android-advanced-dev/testing.md`）