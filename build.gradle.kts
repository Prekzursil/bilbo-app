// Raise Bouncy Castle on the *buildscript* (plugin) classpath.
//
// The Android Gradle Plugin drags Bouncy Castle 1.79 onto this classpath through its
// APK signing/zip stack — com.android.tools.build:builder, :apkzlib and
// com.android.tools:sdk-common all declare bcprov-jdk18on:1.79. That is what
// GHSA-574f-3g2m-x479 / CVE-2025-14813 (critical, CVSS 4.0 9.3 — GOST 28147 CTR
// keystream reuse) is reported against, attributed to settings.gradle.kts because
// that is the build entry point GitHub's dependency submission records.
//
// The `configurations.all { resolutionStrategy }` blocks in androidApp/build.gradle.kts
// and shared/build.gradle.kts CANNOT fix it: they iterate *project* configurations,
// while plugins are resolved into the root project's `buildscript` classpath. That is
// why the bouncycastle = "1.85" pin in gradle/libs.versions.toml never applied here.
// Bumping AGP does not help either — builder 9.3.0, 9.3.1 and 9.4.0-alpha01 all still
// declare 1.79.
//
// Floor is 1.84, NOT the 1.80.2 GitHub reports as "first patched": this one advisory
// has three disjoint vulnerable ranges for bcprov-jdk18on —
//   >= 1.59, <= 1.80.1  (fixed 1.80.2)
//   = 1.81.0            (fixed 1.81.1)
//   >= 1.82, <= 1.83    (fixed 1.84)
// so a 1.80.2 floor would still admit 1.81.0/1.82/1.83, all still vulnerable to the
// SAME advisory. Only >= 1.84 clears all three.
//
// A bounded range, not an exact pin: Gradle takes the highest in range (1.85 today)
// and Dependabot can still raise it, instead of reporting the requirement as already
// satisfied. bcpkix/bcutil ride the same range — Bouncy Castle modules must move in
// lockstep or AGP hits NoSuchMethodError at build time.
buildscript {
    dependencies {
        constraints {
            add("classpath", "org.bouncycastle:bcprov-jdk18on") {
                version { require("[1.84,2.0)") }
                because("GHSA-574f-3g2m-x479 / CVE-2025-14813 — AGP's buildscript classpath pulls 1.79")
            }
            add("classpath", "org.bouncycastle:bcpkix-jdk18on") {
                version { require("[1.84,2.0)") }
                because("keep Bouncy Castle modules in lockstep with bcprov-jdk18on")
            }
            add("classpath", "org.bouncycastle:bcutil-jdk18on") {
                version { require("[1.84,2.0)") }
                because("keep Bouncy Castle modules in lockstep with bcprov-jdk18on")
            }
            // Kover 0.9.11 (the latest) reaches FreeMarker 2.3.32 through
            // kover-features-jvm -> intellij-coverage-reporter -> coverage-report 1.0.25,
            // which is GHSA-27j2-h3m2-8237 / CVE-2026-84939 (critical, template-loader
            // path traversal, >= 2.2.0 <= 2.3.34). Same buildscript-classpath shape as
            // Bouncy Castle above, so the same fix: a bounded floor at the first patched
            // release.
            add("classpath", "org.freemarker:freemarker") {
                version { require("[2.3.35,2.4)") }
                because("GHSA-27j2-h3m2-8237 / CVE-2026-84939 — Kover's report stack pulls 2.3.32")
            }
            // Guava rides the same buildscript classpath through AGP's tooling
            // (com.android.tools:* declare it), resolving to 33.4.0-jre — that is
            // Dependabot alert #52: GHSA-xxph-c9ww-hj94 (CompactHashMap /
            // CompactHashSet / MapMakerInternalMap deserialization triggers
            // uncontrolled memory allocation; vulnerable >= 4.0 <= 33.7.1, first
            // patched 33.7.2). Same shape and same fix as Bouncy Castle above: a
            // bounded floor on the buildscript classpath at the first patched
            // release; project-level resolutionStrategy blocks cannot reach it.
            add("classpath", "com.google.guava:guava") {
                version { require("[33.7.2,34.0)") }
                because("GHSA-xxph-c9ww-hj94 / Dependabot alert #52 — AGP's buildscript classpath pulls 33.4.0-jre")
            }
            // Four more AGP build-classpath transitives. Each one is a Dependabot
            // alert that was dismissed by hand on 2026-04-09 as "no bandwidth"
            // although a patched release already existed, and each is still
            // resolved at a vulnerable version today (dependency-graph SBOM,
            // 2026-11-25): build-time exposure only, but live. Same shape and
            // same fix as above — a bounded floor at the first patched release,
            // so Gradle takes the highest release on the declared line.
            //
            // bundletool 1.18.3 -> jose4j 0.9.5
            //   alert #24, GHSA-3677-xxcr-wjqv / CVE-2024-29371 (high, 7.5 — DoS
            //   via compressed JWE content), vulnerable < 0.9.6.
            add("classpath", "org.bitbucket.b_c:jose4j") {
                version { require("[0.9.6,0.10)") }
                because("GHSA-3677-xxcr-wjqv / Dependabot alert #24 — AGP's bundletool pulls jose4j 0.9.5")
            }
            // jetifier-processor 1.0.0-beta10 -> jdom2 2.0.6
            //   alert #21, GHSA-2363-cqg2-863c / CVE-2021-33813 (high, 7.5 — XXE),
            //   vulnerable < 2.0.6.1.
            add("classpath", "org.jdom:jdom2") {
                version { require("[2.0.6.1,2.1)") }
                because("GHSA-2363-cqg2-863c / Dependabot alert #21 — AGP's jetifier-processor pulls jdom2 2.0.6")
            }
            // commons-compress 1.27.1 -> commons-lang3 3.16.0
            //   alert #28, GHSA-j288-q9x7-2f5v / CVE-2025-48924 (medium, 6.5 —
            //   uncontrolled recursion on long inputs), vulnerable >= 3.0 < 3.18.0.
            add("classpath", "org.apache.commons:commons-lang3") {
                version { require("[3.18.0,4.0)") }
                because("GHSA-j288-q9x7-2f5v / Dependabot alert #28 — commons-compress on AGP's classpath pulls commons-lang3 3.16.0")
            }
            // httpmime 4.5.6 -> httpclient 4.5.6
            //   alert #27, GHSA-7r82-7xv7-xcpj / CVE-2020-13956 (medium, 5.3 —
            //   cross-site scripting via a malformed authority section), vulnerable
            //   < 4.5.13 (and 5.0.0 .. < 5.0.3). Held on the 4.5.x line: 5.x is a
            //   different artifact (httpclient5) that httpmime 4.5.6 does not use.
            add("classpath", "org.apache.httpcomponents:httpclient") {
                version { require("[4.5.13,5.0)") }
                because("GHSA-7r82-7xv7-xcpj / Dependabot alert #27 — httpmime on AGP's classpath pulls httpclient 4.5.6")
            }
        }
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kover) apply false
}
