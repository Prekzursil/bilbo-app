# Contributing to Bilbo ⚡🧠

Thank you for your interest in contributing to Bilbo! This document describes how to report bugs, suggest features, set up a development environment, and submit pull requests.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Report Bugs](#how-to-report-bugs)
- [How to Suggest Features](#how-to-suggest-features)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Branch Naming](#branch-naming)
- [Commit Message Format](#commit-message-format)
- [Pull Request Process](#pull-request-process)

---

## Code of Conduct

By participating in this project you agree to treat all contributors with respect. We have zero tolerance for harassment, discrimination, or toxic behavior of any kind.

---

## How to Report Bugs

1. **Search existing issues first** — your bug may already be reported. If so, add a 👍 reaction and any additional context rather than opening a duplicate.

2. **Open a new issue** using the **Bug Report** template and include:
   - A clear, descriptive title
   - Steps to reproduce (numbered, minimal reproduction preferred)
   - Expected behavior vs. actual behavior
   - Device model, OS version, Bilbo version / build flavor
   - Relevant logs (from Android Logcat or iOS Console), stack traces, or screenshots
   - Whether the issue is reproducible on both platforms or only one

3. **Label your issue** with `bug` plus any relevant component label (`android`, `ios`, `backend`, `ai`).

---

## How to Suggest Features

1. **Search existing issues and discussions** to avoid duplicates.

2. **Open a new issue** using the **Feature Request** template and include:
   - The problem you are trying to solve (user story format preferred: *"As a [type of user], I want to [do something] so that [benefit]."*)
   - Your proposed solution
   - Alternatives you have considered
   - Any relevant mockups or references

3. **Label your issue** with `enhancement` plus the relevant component label.

Large architectural changes should be proposed as an **RFC (Request for Comments)** discussion before implementation begins.

---

## Development Setup

### Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| JDK | 17+ | [Adoptium](https://adoptium.net) |
| Android Studio | Ladybug 2024.2.1+ | [developer.android.com](https://developer.android.com/studio) |
| Kotlin Multiplatform Plugin | 2.1.0 | Android Studio → Plugins |
| Xcode | 15+ (macOS only) | Mac App Store |
| Supabase CLI | Latest | `brew install supabase/tap/supabase` |
| Deno | 1.40+ (Edge Fn dev) | [deno.land](https://deno.land) |

### First-time Setup

```bash
# 1. Fork and clone
git clone https://github.com/<your-username>/bilbo-app.git
cd bilbo-app

# 2. Add upstream remote
git remote add upstream https://github.com/Prekzursil/bilbo-app.git

# 3. Copy config template
cp androidApp/src/main/assets/config.template.json \
   androidApp/src/main/assets/config.json
# Edit config.json with your local Supabase keys (see below)

# 4. Start local Supabase stack
supabase start
supabase db reset   # applies all migrations

# 5. Verify the build
./gradlew :shared:build
./gradlew :androidApp:assembleGithubDebug
```

### Running the Full Stack Locally

```bash
# Terminal 1 — Supabase local stack (Docker required)
supabase start

# Terminal 2 — Edge Functions with hot reload
supabase functions serve --env-file supabase/.env.local

# Terminal 3 — Android app (connects to local Supabase)
./gradlew :androidApp:installGithubDebug
```

Set `SUPABASE_URL=http://10.0.2.2:54321` in `config.json` to route Android Emulator traffic to the local stack.

---

## Coding Standards

### Kotlin

- Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- All shared code lives in `shared/src/commonMain/`. Platform-specific code belongs in `androidMain/` or `iosMain/`.
- Use **Kotlinx Coroutines** for async work. Avoid `GlobalScope`. Prefer `viewModelScope` or injected `CoroutineScope`.
- Prefer immutable data (`val`, `data class` with `copy()`). Avoid mutable shared state.
- Use **sealed classes** for UI state (`UiState.Loading`, `UiState.Success`, `UiState.Error`).
- Annotate all `@Composable` functions with `@Preview` for visual verification.

### Detekt

Detekt static analysis is enforced in CI. Run it locally before pushing:

```bash
./gradlew detekt
```

The configuration lives in `config/detekt/detekt.yml`. Do not suppress warnings with `@Suppress` unless there is a documented reason in a comment on the same line.

### Jetpack Compose Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Screen composables | `<Name>Screen` | `IntentGatekeeperScreen` |
| Reusable UI components | `<Name>` (noun/noun phrase) | `FocusPointBadge`, `EmotionSlider` |
| Composable previews | `<Name>Preview` | `FocusPointBadgePreview` |
| ViewModel | `<Name>ViewModel` | `IntentGatekeeperViewModel` |
| UI State | `<Name>UiState` | `CheckInUiState` |
| UI Events | `<Name>UiEvent` | `CheckInUiEvent` |

### Swift / SwiftUI (iOS)

- Follow [Swift API Design Guidelines](https://www.swift.org/documentation/api-design-guidelines/).
- Use `@Observable` (Swift 5.9+) for view models; avoid `ObservableObject` in new code.
- Run SwiftLint (`swiftlint lint`) before pushing iOS-touching changes.
- Keep SwiftUI views small — extract sub-views aggressively.

### Supabase Edge Functions (Deno/TypeScript)

- Use `Deno.serve()` entry point.
- Always validate the `Authorization` header and create the Supabase client from the request JWT.
- Return standard HTTP status codes (`200`, `201`, `400`, `401`, `403`, `409`, `429`, `500`).
- Include CORS headers on every response.
- No secrets in source code — use `Deno.env.get()` and Supabase Vault.

### General

- **No `println` / `console.log` in production paths** — use structured logging (Timber on Android, `os_log` on iOS, structured JSON on Edge Functions).
- **No hardcoded strings** in UI — use Android string resources and iOS `LocalizedStringKey`.
- Every new public API in `shared/` must have KDoc documentation.
- Test coverage target: **≥ 80%** for `shared/` business logic.

---

## Branch Naming

Use the following prefixes:

| Prefix | Use for |
|--------|---------|
| `feature/` | New features or enhancements |
| `fix/` | Bug fixes |
| `docs/` | Documentation-only changes |
| `refactor/` | Code restructuring without behavior change |
| `test/` | Adding or improving tests |
| `chore/` | Build scripts, dependency updates, tooling |
| `ci/` | CI/CD pipeline changes |

**Format:** `<prefix>/<short-kebab-description>`

**Examples:**
```
feature/intent-gatekeeper-overlay
fix/ios-familycontrols-crash-on-16-3
docs/edge-function-readme
refactor/usage-repository-flows
chore/bump-ktor-3-1-0
```

Branch names should be lowercase, use hyphens (not underscores), and be ≤ 60 characters.

---

## Commit Message Format

Bilbo uses **[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)**.

### Structure

```
<type>(<scope>): <short summary>

[optional body]

[optional footer(s)]
```

### Types

| Type | Use for |
|------|---------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only |
| `style` | Formatting (no logic change) |
| `refactor` | Code restructuring |
| `test` | Adding or fixing tests |
| `chore` | Build process, dependency updates |
| `ci` | CI/CD configuration |
| `perf` | Performance improvement |
| `revert` | Reverts a previous commit |

### Scopes (optional but encouraged)

`shared`, `android`, `ios`, `supabase`, `functions`, `ci`, `docs`, `gradle`

### Examples

```
feat(android): add WindowManager overlay for intent gatekeeper
fix(shared): correct dopamine budget weekly reset logic
docs(supabase): add README for create-invite edge function
chore(gradle): bump Kotlin to 2.1.0
feat(functions)!: redesign ai-weekly-insight rate limiting

BREAKING CHANGE: rate_limit_tokens column removed from user_preferences
```

- Use the **imperative mood** in the summary ("add", not "added" or "adds").
- Keep the summary line ≤ 72 characters.
- Mark breaking changes with `!` after the type/scope and a `BREAKING CHANGE:` footer.

---

## Pull Request Process

1. **Sync with upstream** before starting work:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Keep PRs focused** — one logical change per PR. Avoid mixing feature work with unrelated refactors.

3. **Fill in the PR template** completely:
   - Link the related issue (`Closes #123`)
   - Describe what changed and why
   - Include screenshots or screen recordings for UI changes
   - List any manual testing performed

4. **Ensure CI passes** before requesting review:
   - `./gradlew detekt` — no new violations
   - `./gradlew :shared:allTests` — all tests pass
   - `./gradlew :androidApp:assembleGithubDebug` — clean build
   - SwiftLint passes for iOS changes

5. **Request review** from at least one maintainer. For significant changes, request two reviewers.

6. **Address review comments** with new commits (do not force-push during review). Once approved, the PR will be squash-merged by a maintainer.

7. **Do not merge your own PR** unless you are a maintainer and the change is trivial (e.g., typo fix).

### PR Checklist (copy into your PR description)

```markdown
- [ ] I have read CONTRIBUTING.md
- [ ] My branch follows the naming convention
- [ ] My commits follow Conventional Commits format
- [ ] I have added/updated tests for my changes
- [ ] `./gradlew detekt` passes with no new violations
- [ ] `./gradlew :shared:allTests` passes
- [ ] CI is green
- [ ] I have updated documentation where needed
- [ ] Screenshots/recordings attached (for UI changes)
```

---

Thank you for helping make Bilbo better! 🙏
