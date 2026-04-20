# Repository Guidelines

## Project Structure & Module Organization
This repository is a collection of Android development skills. Each top-level folder such as `android-mvi-compose/`, `android-network/`, and `android-build-publish/` is a self-contained skill package. A package usually includes a `SKILL.md` guide plus supporting Kotlin, XML, or Markdown reference files. Shared reference material lives in `references/`, and `README.md` provides the catalog overview.

## Build, Test, and Development Commands
This repo does not ship a runnable app or Gradle build. Common contributor tasks are content-focused:

- `ls */SKILL.md` — list all skill entry files.
- `find . -maxdepth 2 -type f | sort` — review package contents before editing.
- `sed -n '1,120p' android-mvi-compose/SKILL.md` — inspect a skill quickly.
- `cp -R . ~/.claude/skills/` — install the skills locally for manual verification.

When updating examples, keep snippets self-contained and copy-pasteable.

## Coding Style & Naming Conventions
Use concise Markdown with clear headings and short task-oriented instructions. Keep filenames descriptive and aligned with contents, for example `ThemeConfig.kt` or `compose-guidelines.md`. Preserve existing language choices: docs may be Chinese, while code examples should follow idiomatic Kotlin and Android XML conventions. Prefer 4-space indentation in code blocks and consistent fenced Markdown blocks for commands and snippets.

## Testing Guidelines
There is no automated test suite in this repository today. Validate changes by checking that each edited `SKILL.md` still references real local files and that sample code matches the surrounding package topic. If you add test guidance, place it near the relevant skill, as in `android-advanced-dev/testing.md`.

## Commit & Pull Request Guidelines
Current Git history uses short, imperative commit messages, e.g. `Initial commit: Add Android Skills collection`. Follow the same style: `module: summarize change` also works well, such as `android-network: refine error handling examples`. PRs should include a brief summary, affected skill directories, and sample screenshots only when formatting or rendered output changed.

## Contributor Notes
Avoid introducing cross-package dependencies unless multiple skills truly share the same reference. Prefer small, focused edits within a single skill directory.
