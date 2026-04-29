#!/usr/bin/env bash
# ============================================================================
# dashboard_zero_audit.sh — verify every quality gate's source-of-truth
#                          dashboard reports 0 open findings
# ============================================================================
#
# DESCRIPTION
#   Hits each vendor's API and asserts the open-finding count is 0. This is
#   the 5th condition of the ralph-loop's Stop Condition Verification Suite
#   and the implementation backbone of WU-B11 (gate self-audit).
#
#   A green CI workflow run is necessary but not sufficient — a gate's
#   `check_<vendor>_zero.py` script may swallow API errors, query the wrong
#   project, or use a stale severity filter. This script provides an
#   independent cross-check.
#
# USAGE
#   bash scripts/quality/dashboard_zero_audit.sh
#   bash scripts/quality/dashboard_zero_audit.sh --json     # machine-readable
#   bash scripts/quality/dashboard_zero_audit.sh --gate sonar  # one gate
#
# EXIT CODES
#   0 — every probed dashboard reports 0 open findings
#   1 — at least one dashboard reports >0 open findings
#   2 — a dashboard probe failed (auth, network, etc.) — treat as red
#
# ENVIRONMENT (optional; missing tokens skip that gate but the script still
# fails if any non-skipped gate has findings)
#   SONAR_TOKEN, CODACY_API_TOKEN, DEEPSCAN_API_TOKEN, DEEPSOURCE_DSN,
#   SENTRY_AUTH_TOKEN, SENTRY_ORG, SENTRY_PROJECT, GITHUB_TOKEN (for gh)
#
# OUTPUT
#   stdout: a markdown table summarising each gate's count.
#   .beads/last-dashboard-check.json: machine-readable snapshot for the
#     ralph-loop's PR-body inclusion.
#
# DEPENDENCIES
#   curl, jq, gh
#
# ============================================================================

set -euo pipefail

REPO="${REPO:-Prekzursil/bilbo-app}"
ARTIFACT_DIR="${ARTIFACT_DIR:-.beads}"
ARTIFACT_FILE="${ARTIFACT_DIR}/last-dashboard-check.json"
mkdir -p "$ARTIFACT_DIR"

JSON_MODE=false
ONLY_GATE=""
for arg in "$@"; do
    case "$arg" in
        --json) JSON_MODE=true ;;
        --gate) shift; ONLY_GATE="${1:-}" ;;
        --gate=*) ONLY_GATE="${arg#--gate=}" ;;
    esac
done

declare -A RESULTS  # gate -> "count|status"  (status: ok | issues | skipped | error)
OVERALL_EXIT=0

require() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "::error::missing required tool: $1"
        exit 2
    }
}

probe_should_run() {
    [ -z "$ONLY_GATE" ] || [ "$ONLY_GATE" = "$1" ]
}

# ----- Gate probes ----------------------------------------------------------

probe_codeql() {
    probe_should_run codeql || return 0
    local count
    count=$(gh api "repos/${REPO}/code-scanning/alerts?state=open" --paginate 2>/dev/null \
            | jq -s 'add | length' 2>/dev/null || echo "ERR")
    [ "$count" = "ERR" ] && { RESULTS[codeql]="?|error"; return; }
    RESULTS[codeql]="${count}|$([ "$count" = "0" ] && echo ok || echo issues)"
}

probe_dependabot() {
    probe_should_run dependabot || return 0
    local count
    count=$(gh api "repos/${REPO}/dependabot/alerts?state=open" --paginate 2>/dev/null \
            | jq -s 'add | length' 2>/dev/null || echo "ERR")
    [ "$count" = "ERR" ] && { RESULTS[dependabot]="?|error"; return; }
    RESULTS[dependabot]="${count}|$([ "$count" = "0" ] && echo ok || echo issues)"
}

probe_secret_scanning() {
    probe_should_run secret_scanning || return 0
    local count
    count=$(gh api "repos/${REPO}/secret-scanning/alerts?state=open" 2>/dev/null \
            | jq 'length' 2>/dev/null || echo "ERR")
    [ "$count" = "ERR" ] && { RESULTS[secret_scanning]="?|error"; return; }
    RESULTS[secret_scanning]="${count}|$([ "$count" = "0" ] && echo ok || echo issues)"
}

probe_github_issues() {
    probe_should_run github_issues || return 0
    local count
    count=$(gh issue list -R "$REPO" --state open --limit 1000 --json number 2>/dev/null \
            | jq 'length' 2>/dev/null || echo "ERR")
    [ "$count" = "ERR" ] && { RESULTS[github_issues]="?|error"; return; }
    # GitHub Issues being non-empty isn't a quality-gate violation — track but
    # don't fail the gate. WU-B12 may decide otherwise.
    RESULTS[github_issues]="${count}|$([ "$count" = "0" ] && echo ok || echo skipped)"
}

probe_sonarcloud() {
    probe_should_run sonarcloud || return 0
    if [ -z "${SONAR_TOKEN:-}" ]; then
        # Public read works without token for public projects.
        :
    fi
    local component_key="${SONAR_COMPONENT_KEY:-Prekzursil_bilbo-app}"
    local resp count
    resp=$(curl -fsS \
        ${SONAR_TOKEN:+-u "${SONAR_TOKEN}:"} \
        "https://sonarcloud.io/api/issues/search?componentKeys=${component_key}&statuses=OPEN&ps=1" \
        2>/dev/null || echo "ERR")
    [ "$resp" = "ERR" ] && { RESULTS[sonarcloud]="?|error"; return; }
    count=$(echo "$resp" | jq '.total' 2>/dev/null || echo "ERR")
    [ "$count" = "ERR" ] && { RESULTS[sonarcloud]="?|error"; return; }
    RESULTS[sonarcloud]="${count}|$([ "$count" = "0" ] && echo ok || echo issues)"
}

probe_codacy() {
    probe_should_run codacy || return 0
    if [ -z "${CODACY_API_TOKEN:-}" ]; then
        RESULTS[codacy]="?|skipped (no CODACY_API_TOKEN)"; return
    fi
    local resp count
    resp=$(curl -fsS \
        -H "api-token: ${CODACY_API_TOKEN}" \
        "https://app.codacy.com/api/v3/analysis/organizations/gh/Prekzursil/repositories/bilbo-app/issues" \
        2>/dev/null || echo "ERR")
    [ "$resp" = "ERR" ] && { RESULTS[codacy]="?|error"; return; }
    count=$(echo "$resp" | jq '.pagination.total // 0' 2>/dev/null || echo "ERR")
    [ "$count" = "ERR" ] && { RESULTS[codacy]="?|error"; return; }
    RESULTS[codacy]="${count}|$([ "$count" = "0" ] && echo ok || echo issues)"
}

probe_deepscan() {
    probe_should_run deepscan || return 0
    if [ -z "${DEEPSCAN_API_TOKEN:-}" ] || [ -z "${DEEPSCAN_PROJECT_ID:-}" ]; then
        RESULTS[deepscan]="?|skipped (no DEEPSCAN_API_TOKEN/PROJECT_ID)"; return
    fi
    local resp count
    resp=$(curl -fsS \
        -H "Authorization: Bearer ${DEEPSCAN_API_TOKEN}" \
        "https://deepscan.io/api/projects/${DEEPSCAN_PROJECT_ID}/issues?status=open" \
        2>/dev/null || echo "ERR")
    [ "$resp" = "ERR" ] && { RESULTS[deepscan]="?|error"; return; }
    count=$(echo "$resp" | jq '. | length' 2>/dev/null || echo "ERR")
    [ "$count" = "ERR" ] && { RESULTS[deepscan]="?|error"; return; }
    RESULTS[deepscan]="${count}|$([ "$count" = "0" ] && echo ok || echo issues)"
}

probe_deepsource() {
    probe_should_run deepsource || return 0
    if [ -z "${DEEPSOURCE_DSN:-}" ]; then
        RESULTS[deepsource]="?|skipped (no DEEPSOURCE_DSN)"; return
    fi
    # DeepSource uses GraphQL; we query the issue count for the open state.
    local resp count
    resp=$(curl -fsS -X POST \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${DEEPSOURCE_DSN}" \
        --data '{"query":"query{repository(login:\"Prekzursil\",name:\"bilbo-app\",vcsProvider:GITHUB){issues(filter:{status:OPEN}){totalCount}}}"}' \
        "https://api.deepsource.io/graphql/" \
        2>/dev/null || echo "ERR")
    [ "$resp" = "ERR" ] && { RESULTS[deepsource]="?|error"; return; }
    count=$(echo "$resp" | jq '.data.repository.issues.totalCount // 0' 2>/dev/null || echo "ERR")
    [ "$count" = "ERR" ] && { RESULTS[deepsource]="?|error"; return; }
    RESULTS[deepsource]="${count}|$([ "$count" = "0" ] && echo ok || echo issues)"
}

probe_sentry() {
    probe_should_run sentry || return 0
    if [ -z "${SENTRY_AUTH_TOKEN:-}" ] || [ -z "${SENTRY_ORG:-}" ] || [ -z "${SENTRY_PROJECT:-}" ]; then
        RESULTS[sentry]="?|skipped (no SENTRY_AUTH_TOKEN/ORG/PROJECT)"; return
    fi
    local resp count
    resp=$(curl -fsS \
        -H "Authorization: Bearer ${SENTRY_AUTH_TOKEN}" \
        "https://sentry.io/api/0/projects/${SENTRY_ORG}/${SENTRY_PROJECT}/issues/?query=is:unresolved&statsPeriod=24h&limit=1" \
        2>/dev/null || echo "ERR")
    [ "$resp" = "ERR" ] && { RESULTS[sentry]="?|error"; return; }
    count=$(echo "$resp" | jq 'length' 2>/dev/null || echo "ERR")
    [ "$count" = "ERR" ] && { RESULTS[sentry]="?|error"; return; }
    RESULTS[sentry]="${count}|$([ "$count" = "0" ] && echo ok || echo issues)"
}

probe_codecov() {
    probe_should_run codecov || return 0
    # Codecov public API for the main branch coverage.
    local resp coverage
    resp=$(curl -fsS "https://codecov.io/api/v2/github/Prekzursil/repos/bilbo-app/branches/main" 2>/dev/null || echo "ERR")
    [ "$resp" = "ERR" ] && { RESULTS[codecov]="?|error"; return; }
    coverage=$(echo "$resp" | jq -r '.head_commit.totals.coverage // "0"' 2>/dev/null || echo "ERR")
    [ "$coverage" = "ERR" ] && { RESULTS[codecov]="?|error"; return; }
    # Coverage of 100.0 is "ok"; anything below is "issues".
    RESULTS[codecov]="${coverage}|$([ "$(echo "$coverage >= 100.0" | bc -l 2>/dev/null || echo 0)" = "1" ] && echo ok || echo issues)"
}

probe_gitleaks() {
    probe_should_run gitleaks || return 0
    command -v gitleaks >/dev/null 2>&1 || { RESULTS[gitleaks]="?|skipped (gitleaks not installed)"; return; }
    if gitleaks detect --no-git --source . --redact -v >/dev/null 2>&1; then
        RESULTS[gitleaks]="0|ok"
    else
        RESULTS[gitleaks]=">=1|issues"
    fi
}

probe_semgrep_local() {
    probe_should_run semgrep || return 0
    [ -f .semgrep.yml ] || { RESULTS[semgrep]="?|skipped (no .semgrep.yml)"; return; }
    command -v semgrep >/dev/null 2>&1 || { RESULTS[semgrep]="?|skipped (semgrep not installed)"; return; }
    if semgrep --config=.semgrep.yml --error --quiet >/dev/null 2>&1; then
        RESULTS[semgrep]="0|ok"
    else
        RESULTS[semgrep]=">=1|issues"
    fi
}

# ----- Main dispatch --------------------------------------------------------

require curl
require jq
require gh

probe_codeql
probe_dependabot
probe_secret_scanning
probe_github_issues
probe_sonarcloud
probe_codacy
probe_deepscan
probe_deepsource
probe_sentry
probe_codecov
probe_gitleaks
probe_semgrep_local

# ----- Output ---------------------------------------------------------------

print_table() {
    printf "| Gate | Count | Status |\n"
    printf "|------|-------|--------|\n"
    for gate in $(echo "${!RESULTS[@]}" | tr ' ' '\n' | sort); do
        IFS='|' read -r count status <<< "${RESULTS[$gate]}"
        local emoji
        case "$status" in
            ok)         emoji="✅" ;;
            issues)     emoji="❌" ;;
            error)      emoji="🟡 ERROR" ;;
            skipped*)   emoji="⏭️" ;;
            *)          emoji="❓" ;;
        esac
        printf "| %-15s | %5s | %s %s |\n" "$gate" "$count" "$emoji" "$status"
    done
}

emit_json() {
    {
        printf '{"timestamp":"%s","repo":"%s","gates":{' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$REPO"
        local first=true
        for gate in $(echo "${!RESULTS[@]}" | tr ' ' '\n' | sort); do
            IFS='|' read -r count status <<< "${RESULTS[$gate]}"
            $first || printf ','
            first=false
            printf '"%s":{"count":"%s","status":"%s"}' "$gate" "$count" "$status"
        done
        printf '}}\n'
    } > "$ARTIFACT_FILE"
}

# Determine overall exit
for gate in "${!RESULTS[@]}"; do
    IFS='|' read -r count status <<< "${RESULTS[$gate]}"
    case "$status" in
        issues)  OVERALL_EXIT=1 ;;
        error)   [ $OVERALL_EXIT -lt 2 ] && OVERALL_EXIT=2 ;;
    esac
done

if $JSON_MODE; then
    emit_json
    cat "$ARTIFACT_FILE"
else
    print_table
    emit_json  # always write the artifact for PR body inclusion
fi

if [ $OVERALL_EXIT -eq 0 ]; then
    echo ""
    echo "✅ ALL DASHBOARDS REPORT 0 OPEN FINDINGS"
else
    echo ""
    echo "❌ AT LEAST ONE DASHBOARD HAS OPEN FINDINGS (exit ${OVERALL_EXIT})"
fi

exit $OVERALL_EXIT
