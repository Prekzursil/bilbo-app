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
