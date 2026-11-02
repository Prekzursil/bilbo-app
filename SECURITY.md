# Security Policy

## Reporting a vulnerability

Please use [GitHub private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-reviewing/privately-reporting-a-security-vulnerability)
on this repository (**Security → Report a vulnerability**). Do not open a
public issue for anything security-sensitive.

Include:

- the component involved (Android app, iOS app, KMP shared module, or Supabase functions)
- steps to reproduce or a proof of concept
- the impact you see

You should get a first response within a week. Please do not disclose the
issue publicly before a fix is released.

## Scope

The code in this repository is in scope, including the Supabase functions
under `supabase/functions/` and the Gradle build. Vulnerabilities in
third-party dependencies are reported the same way — Dependabot alerts for
them are triaged here too, and fixed by version bumps rather than
suppression.

## Handling

- Reports are fixed on a private branch and released with an advisory when user-facing.
- Dependabot security updates are enabled and grouped so one PR can clear every open advisory.
- CodeQL (security-and-quality) and the lean quality gate (gitleaks, opengrep, osv-scanner) run on every pull request; a PR that introduces a finding of any severity is blocked, not muted.
