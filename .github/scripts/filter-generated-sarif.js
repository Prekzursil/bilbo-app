#!/usr/bin/env node
/**
 * Filter Compose-compiler-GENERATED `$stable` field findings out of CodeQL
 * SARIF files. IN PLACE. (SwitchAI-precedent, owner-directed 2026-11-17.)
 *
 * WHY THIS EXISTS (do not remove without owner approval — fleet security
 * standard, owner-directed 2026-11-17):
 *
 * The Kotlin Compose compiler synthesizes a `$stable` field on classes it
 * infers stability for. When a subclass re-declares its own inferred
 * stability, the generated subclass `$stable` "masks" the generated
 * superclass `$stable`, and CodeQL reports `java/field-masks-super-field`.
 * The two live alerts (created 2026-11-04, still open at the time of this
 * filter) are exactly that artifact, with NO source construct behind them:
 *
 *   - code-scanning alert #32: androidApp/src/main/kotlin/dev/bilbo/app/ui/screen/ChallengeScreen.kt:1
 *     "This field shadows another field called $stable in a superclass."
 *   - code-scanning alert #33: androidApp/src/main/kotlin/dev/bilbo/app/ui/screen/CircleScreen.kt:1
 *     "This field shadows another field called $stable in a superclass."
 *
 * Verified absent at source: `grep -n stable` over both files returns
 * nothing — the string "$stable" does not occur in repository source; the
 * fields exist only in compiler output. They cannot be fixed at source.
 *
 * Documented CodeQL exclusion mechanisms were tried on the SwitchAI
 * precedent (same owner decision tree) and none suppresses findings from a
 * traced java-kotlin database:
 *   1. codescanning-config `paths_exclude`  -> CLI rejects the property.
 *   2. codescanning-config `paths_ignore`   -> same CLI rejection.
 *   3. codescanning-config `paths-ignore`   -> accepted, does not filter a
 *      traced database.
 *   4. `build-mode: none` (buildless)       -> fatal for a real Android
 *      build ("CodeQL could not process any code written in Java/Kotlin").
 *   5. `CODEQL_EXTRACTOR_JAVA_EXCLUDE` env  -> accepted, no effect.
 * Additionally, a source-code fix is impossible (no construct to change)
 * and renaming is compiler-internal. Therefore, per the owner's explicit
 * direction, results are removed from the SARIF between analysis and
 * upload, and the next same-key SARIF upload closes the two alerts through
 * normal code-scanning reconciliation. No API dismissals are used.
 *
 * CANARY / GUARDRAILS (binding):
 *   - Filter NARROWLY: ONLY rule `java/field-masks-super-field` findings
 *     whose message names the compiler-synthesized `$stable` field. Every
 *     other finding of this rule (real field shadowing in first-party
 *     Kotlin) MUST keep flowing — if this rule ever reports a genuine
 *     shadowing bug, the filter must not eat it: the message of a real
 *     finding names a real field, not `$stable`.
 *   - First-party Kotlin findings MUST keep being reported by this
 *     pipeline. The script logs kept/removed counts on every run; a kept
 *     count of 0 on this codebase means the pipeline is broken — fix it,
 *     do not touch the filter predicate.
 *
 * Usage: node filter-generated-sarif.js <sarif-directory>
 */

const fs = require('fs');
const path = require('path');

const RULE_ID = 'java/field-masks-super-field';
const SYNTHETIC_FIELD = '$stable';

const dir = process.argv[2];
if (!dir) {
  console.error('usage: node filter-generated-sarif.js <sarif-directory>');
  process.exit(2);
}

function resultMessage(result) {
  // GitHub/CodeQL render placeholder references as markdown links, e.g.
  // "called [$stable](1) in a superclass" (the link points at
  // relatedLocations id 1, whose own message is the bare "$stable").
  // Normalize both forms to the bare text before matching.
  return (result?.message?.text ?? '').replace(/\[([^\]]+)\]\(\d+\)/g, '$1');
}

function ruleId(result, ruleTables) {
  // Observed shapes (raw CodeQL java-kotlin SARIF, bilbo-app analyses
  // 2189643927 / 2190148480): `rule` is an OBJECT {"id": ..., "index": N,
  // "toolComponent": {...}}; some renderings carry a plain `ruleId` string;
  // `ruleIndex` may index into driver.rules OR tool.extensions[].rules.
  // Handle all of them.
  const r = result?.rule;
  if (typeof r === 'string') return r;
  if (r && typeof r === 'object' && typeof r.id === 'string') return r.id;
  if (typeof result?.ruleId === 'string') return result.ruleId;
  const idx = result?.ruleIndex;
  if (typeof idx !== 'number') return null;
  for (const rules of ruleTables) {
    if (rules[idx]) return rules[idx].id ?? null;
  }
  return null;
}

function primaryLocationPath(result) {
  const uri = result?.locations?.[0]?.physicalLocation?.artifactLocation?.uri;
  if (typeof uri !== 'string') return null;
  try {
    return decodeURIComponent(uri).replace(/\\/g, '/');
  } catch {
    return uri.replace(/\\/g, '/');
  }
}

let totalRemoved = 0;
let totalKept = 0;
const removedDetail = [];

for (const file of fs.readdirSync(dir)) {
  if (!file.endsWith('.sarif')) continue;
  const full = path.join(dir, file);
  const sarif = JSON.parse(fs.readFileSync(full, 'utf8'));
  for (const run of sarif.runs ?? []) {
    const ruleTables = [
      run?.tool?.driver?.rules ?? [],
      ...(run?.tool?.extensions ?? []).map((e) => e?.rules ?? []),
    ];
    const results = run.results ?? [];
    const kept = results.filter((r) => {
      const rid = ruleId(r, ruleTables);
      const msg = resultMessage(r);
      const excluded = rid === RULE_ID && msg.includes(`called ${SYNTHETIC_FIELD} in a superclass`);
      if (excluded) {
        totalRemoved++;
        removedDetail.push(`${primaryLocationPath(r)} :: ${msg}`);
        return false;
      }
      totalKept++;
      return true;
    });
    run.results = kept;
  }
  fs.writeFileSync(full, JSON.stringify(sarif));
  console.log(`[filter-generated-sarif] ${file}: processed`);
}

console.log(
  `[filter-generated-sarif] removed ${totalRemoved} Compose-compiler-generated ` +
    `${SYNTHETIC_FIELD} findings (rule ${RULE_ID}), kept ${totalKept} real findings.`,
);
for (const d of removedDetail) {
  console.log(`[filter-generated-sarif] REMOVED generated-code finding: ${d}`);
}
if (totalKept === 0) {
  console.log(
    '[filter-generated-sarif] WARNING: kept count is 0 — first-party findings ' +
      'must keep flowing (canary). Investigate the analysis, not the filter.',
  );
}
