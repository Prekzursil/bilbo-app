# Bilbo v2.0.0 Active Plan

> Source of truth: `docs/superpowers/plans/2026-04-29-bilbo-quality-zero-v2-implementation.md`
> Spec: `docs/superpowers/specs/2026-04-29-bilbo-quality-zero-v2-design.md`
> Ralph-loop driver: `RALPH_LOOP.md`

The ralph-loop reads this file to pick the next work unit. Mark `[x]` only when the WU's PR is merged on `main` AND all 12 gates remain green.

## Track A — Functional Completion

- [ ] **WU-A1** — SQLDelight driver factories (Android + iOS)
- [ ] **WU-A2** — Real repository implementations replacing in-memory stubs (depends on A1)
- [ ] **WU-A3** — Supabase client initialization with CI-injected credentials
- [ ] **WU-A4** — Edge-function client wrappers in shared module (depends on A3)
- [ ] **WU-A5** — iOS AuthManager.hasActiveSession() ObjC export (depends on A3)
- [ ] **WU-A6** — Real Settings sub-screens (Android + iOS)
- [ ] **WU-A7** — iOS DeviceActivityMonitor + ShieldConfiguration + ShieldAction extensions wired
- [ ] **WU-A8** — Realtime subscriptions for buddies / circles / challenges (depends on A3, A4)
- [ ] **WU-A9** — Push notifications: FCM (Android) + real APNs (depends on A4)
- [ ] **WU-A10** — Tier-3 weekly narrative wiring (CloudInsightClient + UI) (depends on A4)
- [ ] **WU-A11** — Replace remaining "coming soon" placeholders (depends on A6, A10)

## Track B — Quality-Zero Gate Adoption

- [ ] **WU-B1** — Co-evolution PR in `Prekzursil/quality-zero-platform` (kotlin-multiplatform stack + bilbo-app repo profile)
- [ ] **WU-B2** — Add 5 reusable-caller workflows to bilbo (depends on B1)
- [ ] **WU-B3** — Strip every `continue-on-error: true` from existing workflows
- [ ] **WU-B4** — Quality config files (`.coverage-thresholds.json`, `.semgrep.yml`, `.codacy.yaml`, `.deepsource.toml`, `.gitleaks.toml`)
- [ ] **WU-B5** — `sonar-project.properties` v2 with all four coverage paths
- [ ] **WU-B6** — Shrink `shared/build.gradle.kts` Kover exclusions to ≤5; backfill commonTest (depends on A2, A4, A5, A8, A10)
- [ ] **WU-B7** — Add Kover to `androidApp` and write missing Android tests to 100% (depends on A2, A6)
- [ ] **WU-B8** — Add iOS test target + XCTest suite to 100% via `xccov` (depends on A5, A6, A7)
- [ ] **WU-B9** — Deno tests for every supabase function to 100% line (depends on A9 for push-notification rewrite)
- [ ] **WU-B10** — Local `scripts/verify` mirroring event-link's gate (depends on B6, B7, B8, B9)

## Track C — Release Polish

- [ ] **WU-C1** — Real Android signing (release keystore via CI secrets)
- [ ] **WU-C2** — iOS signing via fastlane match (depends on A7)
- [ ] **WU-C3** — Version bump to 2.0.0
- [ ] **WU-C4** — README + badges + signed-install instructions (depends on B2, C1, C2)
- [ ] **WU-C5** — CHANGELOG, RELEASE_NOTES, QUALITY_GATES docs (depends on all A* and B*)
- [ ] **WU-C6** — Release attach-artifacts workflow update (signed APKs + IPA + dSYM + SHA256SUMS) (depends on C1, C2)
- [ ] **WU-C7** — Tag v2.0.0 + final dry-run on main (depends on EVERYTHING above; FINAL WU)

## Stop Condition

When ALL 28 work units are `[x]` AND the four-condition Stop Condition Verification Suite in `RALPH_LOOP.md` returns success, the loop emits `<promise>BILBO_V2_DONE</promise>` and stops.
