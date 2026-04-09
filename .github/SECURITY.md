# Security Policy

Bilbo is a digital wellness app that handles personal screen-time data, emotional check-ins, and social accountability relationships. We take the security of our users' data seriously and appreciate the responsible disclosure of vulnerabilities.

---

## Supported Versions

Only the latest major version of Bilbo receives security updates. Please ensure you are running the most recent release before reporting a vulnerability.

| Version | Supported |
|---------|-----------|
| Latest major (1.x) | ✅ Yes |
| Older major versions | ❌ No — please upgrade |

---

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.** Public disclosure before a patch is released puts all users at risk.

### How to Report

Send an email to **prekzursil1993@gmail.com** with:

1. **Subject line:** `[SECURITY] <brief description>`
2. **Description:** A clear explanation of the vulnerability and its potential impact.
3. **Reproduction steps:** Step-by-step instructions to reproduce the issue (proof-of-concept code or scripts are welcome).
4. **Affected component:** Which part of the codebase is affected (Android app, iOS app, Supabase Edge Functions, database schema, CI/CD).
5. **Affected version(s):** The version or commit SHA where you found the issue.
6. **Suggested fix (optional):** If you have ideas on how to fix the issue, please include them.

If the report contains sensitive details (e.g., a working exploit), you may encrypt it using a PGP key. See the [PGP Key](#pgp-key) section below.

---

## Response Timeline

| Milestone | Target |
|-----------|--------|
| **Acknowledgment** | Within **72 hours** of receiving your report |
| **Initial assessment** | Within **7 days** — we will confirm whether the report is valid and assign a severity level |
| **Patch or mitigation** | Within **30 days** for critical/high issues; **90 days** for medium/low |
| **Public disclosure** | After the fix is released, coordinated with the reporter |

We follow a **90-day responsible disclosure timeline**. If 90 days elapse without a fix, you are free to disclose the vulnerability publicly. We will always aim to resolve issues well within this window and may request an extension in exceptional circumstances.

---

## Responsible Disclosure Guidelines

We ask that you:

- **Do not** access, modify, or delete other users' data during your research.
- **Do not** perform denial-of-service (DoS) attacks.
- **Do not** spam users or send mass notifications.
- **Do not** publicly disclose the vulnerability before a fix is released.
- **Do** limit your testing to your own accounts and test environments.
- **Do** provide enough detail for us to reproduce and fix the issue.

In return, we commit to:

- Acknowledge your report within 72 hours.
- Keep you informed of our progress throughout the investigation.
- Credit you in the release notes and/or a security advisory (unless you prefer to remain anonymous).
- Not pursue legal action against researchers who follow these guidelines in good faith.

---

## What Qualifies as a Security Issue

The following are examples of issues that qualify for responsible disclosure:

- **Authentication bypass** — unauthenticated access to user data or protected API endpoints.
- **Authorization flaws** — accessing, modifying, or deleting another user's data (broken Row Level Security policies, missing auth checks in Edge Functions).
- **Sensitive data exposure** — personal screen-time data, emotional check-ins, or social graph data leaked to unauthorized parties.
- **Injection vulnerabilities** — SQL injection, code injection, or command injection in Edge Functions or build scripts.
- **Privilege escalation** — gaining elevated permissions beyond those granted at sign-up.
- **Insecure data storage** — unencrypted storage of sensitive data on device (Supabase tokens, emotional data in unprotected SQLite, etc.).
- **Token/secret leakage** — API keys or access tokens exposed in logs, crash reports, or HTTP responses.
- **Push notification abuse** — ability to send arbitrary push notifications to users without authorization.
- **Cryptographic weaknesses** — use of weak or broken cryptographic algorithms for data at rest or in transit.
- **Dependency vulnerabilities** — critical CVEs in transitive dependencies with a demonstrated impact path (not just a theoretical one).

---

## What Does NOT Qualify

The following are **not** security vulnerabilities and should be reported as regular issues instead:

- **Feature requests** — improvements or new functionality that do not involve a security risk.
- **General bugs** — crashes, incorrect calculations, UI glitches, or unexpected behavior that does not expose user data or grant unauthorized access.
- **Self-XSS** — attacks that require the victim to paste malicious code into their own browser/device.
- **Social engineering** — attacks that require tricking a user into taking unusual actions outside of normal app usage.
- **Physical device attacks** — vulnerabilities that require physical access to an unlocked device.
- **Theoretical vulnerabilities** — issues without a demonstrated, practical attack path.
- **Rate-limiting on non-sensitive endpoints** — endpoints that do not involve sensitive data and have no meaningful impact if rate-limited.
- **Missing security headers** on non-browser endpoints (e.g., Edge Function JSON APIs that are not accessed from a web browser).

---

## PGP Key

A PGP key for encrypting sensitive security reports is not yet published. If you have a highly sensitive finding and would like to use end-to-end encryption, please first contact **prekzursil1993@gmail.com** with a brief, non-sensitive summary and request a PGP public key exchange. We will reply with a key or an alternative secure channel.

---

## Security Update Process

When a security vulnerability is confirmed and a fix is developed:

1. **Private patch** — The fix is developed in a private branch or fork, never in the public `main` branch until disclosure.
2. **Testing** — The patch is tested against the reported reproduction case and the full CI suite.
3. **Release** — A new version is released as soon as the patch is ready. For critical issues, an out-of-band release is published immediately.
4. **Advisory** — A GitHub Security Advisory is published with:
   - CVE identifier (requested from GitHub if applicable)
   - Affected versions
   - Description of the vulnerability and its impact
   - Credit to the reporter (unless anonymity is requested)
5. **Notification** — Users with auto-update disabled are notified via GitHub Release notes and (where feasible) in-app prompts.
6. **Post-mortem** — For critical vulnerabilities, we conduct an internal post-mortem to prevent similar issues in the future.

---

## Scope

This security policy covers:

| Component | In Scope |
|-----------|----------|
| Android app (`/androidApp`) | ✅ Yes |
| iOS app (`/iosApp`) | ✅ Yes |
| Shared KMP module (`/shared`) | ✅ Yes |
| Supabase Edge Functions (`/supabase/functions`) | ✅ Yes |
| PostgreSQL schema & RLS policies (`/supabase/migrations`) | ✅ Yes |
| GitHub Actions workflows (`/.github/workflows`) | ✅ Yes |
| Third-party services (Supabase platform, Anthropic, PostHog) | ❌ No — report to the respective vendor |

---

*Last updated: April 2026*
