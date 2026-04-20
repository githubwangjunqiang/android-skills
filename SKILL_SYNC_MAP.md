# Skills 目录映射与同步策略

本文件记录本机 Android Skills 的多端目录映射，作为后续维护的统一依据。  
当仓库中的 skill 有新增、重构或修复时，应优先以本仓库为源，再按本表同步到对应目录。

## 一、主维护源

| 角色 | 路径 | 说明 |
|------|------|------|
| Git 主仓库 | `/Users/songguo77/AndroidStudioProjects/claude-skills` | **唯一主编辑源**，所有改动先在这里完成并提交 Git |

## 二、目标目录映射

| 路径 | 对应工具 / 体系 | 角色定位 | 维护策略 |
|------|------------------|----------|----------|
| `~/.claude/skills` | Claude Code | Claude 专用 skills 目录 | 需要同步更新 |
| `~/.codex/skills` | Codex | Codex 专用 skills 目录 | 需要同步更新 |
| `~/.config/opencode/skills` | OpenCode | OpenCode 专用 skills 目录 | 需要同步更新 |
| `~/.agents/skills` | 通用 skills 管理体系 / 多 Agent 共用 | 多端共享主目录 | 需要同步更新 |

## 三、关于 `~/.agents/skills`

`~/.agents/skills` 不是某一个单独 AI 工具的私有目录，而是一套通用 skills 管理体系的共享目录。  
根据本机已有安装记录，它至少被以下体系直接或间接使用：

- Codex
- OpenCode
- Gemini CLI
- Antigravity
- Amp
- Claude Code（通过派生目录或兼容目录使用）
- OpenClaw
- Trae

因此，更新 skill 时，不应忽略 `~/.agents/skills`。

## 四、标准更新顺序

每次修改 skill 后，按以下顺序执行：

1. 在 Git 仓库中完成修改：`/Users/songguo77/AndroidStudioProjects/claude-skills`
2. 提交并同步远程仓库
3. 将以下目录视为**发布目标**并同步：
   - `~/.claude/skills`
   - `~/.codex/skills`
   - `~/.config/opencode/skills`
   - `~/.agents/skills`
4. 同步后抽样检查关键文件是否一致，例如：
   - `android-project-overview/SKILL.md`
   - `android-network/SKILL.md`
   - `android-mvi-compose/ComposeStatusComponents.kt`
   - `android-coroutines/CoroutineProvider.kt`

## 五、同步范围

默认同步以下目录：

- `android-advanced-dev`
- `android-build-publish`
- `android-coroutines`
- `android-legacy-view`
- `android-local-storage`
- `android-mvi-compose`
- `android-network`
- `android-project-overview`
- `android-utils-core`
- `references`

## 六、维护原则

- **本仓库是唯一编辑源**，不要直接在目标目录手改
- 若目标目录已有旧文件，以本仓库版本覆盖更新
- 若未来新增新的 skill 目录，应同步补充到本文件
- 若未来新增新的 AI 工具目录，也应补充到本文件后再纳入同步流程
