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
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Kotlin static analysis: ktlint (formatting) + detekt (lint + complexity).
// Both are wired to fail the build on any finding — Strict-Zero quality.
allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    detekt {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(rootProject.files("detekt.yml"))
        // Source dirs across KMP + Android modules. Non-existent dirs are ignored.
        source.setFrom(
            files(
                "src/main/kotlin",
                "src/commonMain/kotlin",
                "src/androidMain/kotlin",
                "src/iosMain/kotlin",
                "src/commonTest/kotlin",
                "src/test/kotlin",
            ),
        )
        parallel = true
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        ignoreFailures.set(false)
        filter {
            // Never lint generated sources (SQLDelight, KSP, build outputs).
            exclude { entry -> entry.file.path.contains("/build/") }
            exclude { entry -> entry.file.path.contains("/generated/") }
        }
    }

    // Force Bouncy Castle to a non-vulnerable version on every configuration of
    // every module. Transitive pulls (via supabase-kt / Ktor / others) would
    // otherwise resolve to versions < 1.84 that contain 3 CVEs (covert timing
    // channel HIGH, LDAP injection MEDIUM, broken crypto algorithm MEDIUM).
    // Centralised here (instead of duplicated per-module) — WU-B12.dependabot.
    val bouncyCastleVersion =
        rootProject.extensions
            .getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
            .named("libs")
            .findVersion("bouncycastle")
            .get()
            .requiredVersion
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.bouncycastle" &&
                (requested.name == "bcprov-jdk18on" || requested.name == "bcpkix-jdk18on")
            ) {
                useVersion(bouncyCastleVersion)
                because("WU-B12.dependabot — pin to >= 1.84 to mitigate 3 CVEs")
            }
        }
    }

    // Strict-Zero Kover gate: every module that applies Kover must hit 100%
    // line AND branch coverage. Centralised here so the identical verify rule is
    // not duplicated across module build files (the per-module `kover { reports {
    // filters { ... } } }` blocks keep only their module-specific exclusions).
    pluginManager.withPlugin("org.jetbrains.kotlinx.kover") {
        configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
            reports {
                verify {
                    rule {
                        minBound(
                            100,
                            kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE,
                            kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE,
                        )
                        minBound(
                            100,
                            kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH,
                            kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE,
                        )
                    }
                }
            }
        }
    }
}
