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
}
