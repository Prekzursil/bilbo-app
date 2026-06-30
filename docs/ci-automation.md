# CI Automation (3-tier)

This repo keeps `main` green with a layered, cost-aware automation stack. Each
tier only escalates when the cheaper one cannot resolve the failure.

## Tier 1 — Dependabot auto-merge (free, native)

Workflow: [`.github/workflows/dependabot-auto-merge.yml`](../.github/workflows/dependabot-auto-merge.yml)

- Fires on Dependabot PRs and turns on GitHub's native **auto-merge** (squash).
- GitHub merges the PR **only once all required checks pass**. Branch protection
  on `main` requires the `quality` gate, so a red Dependabot PR is never merged —
  it waits until green (or a human resolves/closes it).
- Requires the repo setting **Allow auto-merge** to be enabled (it is).

## Tier 2 — GitHub Models autofix (free, in-Actions) — PRIMARY auto-fix

Workflow: [`.github/workflows/autofix-models.yml`](../.github/workflows/autofix-models.yml)

- Triggers on a **failed `Quality Gate` run on a push to `main`** (`workflow_run`
  with `conclusion == failure`).
- Uses the free **GitHub Models** inference action (`actions/ai-inference`,
  model `openai/gpt-4o-mini`) with the built-in `GITHUB_TOKEN` to draft a
  minimal unified-diff fix from the failing CI log.
- If the patch applies cleanly it opens a PR labelled **`autofix`**. The PR is
  **never auto-merged** — normal CI and a human review gate it.
- Bounded by design: only on default-branch CI failure, capped tokens, one PR
  per run, and it lowers no thresholds / disables no checks. If Models cannot
  produce an applyable patch, the run records its analysis in the job summary
  and stops (no PR), handing off to Tier 3.

## Tier 3 — Copilot coding agent (student sub) — JUDICIOUS escalation

Not wired to fire automatically — premium Copilot requests are finite.

Escalate **only when Tier 2 cannot resolve a stuck failing check**:

1. Open (or reuse) a tracking issue describing the failing check, with the link
   to the failed run and the Tier-2 job-summary analysis.
2. Assign the issue to **`@copilot`** so the Copilot coding agent opens a fix PR.
3. Review and gate that PR through normal CI like any other change.

Do **not** assign every failure to `@copilot` — only failures that survive the
free Tiers 1-2.

## Guardrails (all tiers)

- No fake-green, no threshold lowering, no `--no-verify`, no force-push to `main`.
- Only Dependabot PRs auto-merge (Tier 1); Models/Copilot fix PRs always need a
  human + CI.
