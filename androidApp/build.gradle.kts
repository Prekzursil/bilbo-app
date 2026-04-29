import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

android {
    namespace = "dev.bilbo.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.bilbo.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase config — override with CI secrets or local.properties
        val supabaseUrl = project.findProperty("SUPABASE_URL")?.toString() ?: System.getenv("SUPABASE_URL") ?: ""
        val supabaseAnonKey = project.findProperty("SUPABASE_ANON_KEY")?.toString() ?: System.getenv("SUPABASE_ANON_KEY") ?: ""
        val sentryDsn = project.findProperty("SENTRY_DSN")?.toString() ?: System.getenv("SENTRY_DSN") ?: ""
        val posthogApiKey = project.findProperty("POSTHOG_API_KEY")?.toString() ?: System.getenv("POSTHOG_API_KEY") ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
        buildConfigField("String", "POSTHOG_API_KEY", "\"$posthogApiKey\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("playstore") {
            dimension = "distribution"
            applicationIdSuffix = ""
            versionNameSuffix = ""
            buildConfigField("Boolean", "USE_ACCESSIBILITY_SERVICE", "false")
        }
        create("github") {
            dimension = "distribution"
            applicationIdSuffix = ".github"
            versionNameSuffix = "-github"
            buildConfigField("Boolean", "USE_ACCESSIBILITY_SERVICE", "true")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") // replace with release signing in production
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Force Bouncy Castle to a non-vulnerable version on every configuration.
// Transitive pulls (via supabase-kt / Ktor / others) would otherwise resolve
// to versions < 1.84 that contain CVEs:
//   - Covert timing channel (HIGH)
//   - LDAP injection (MEDIUM)
//   - Broken cryptographic algorithm (MEDIUM)
// All three are fixed in 1.84. WU-B12.dependabot.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.bouncycastle" &&
            (requested.name == "bcprov-jdk18on" || requested.name == "bcpkix-jdk18on")
        ) {
            useVersion(libs.versions.bouncycastle.get())
            because("WU-B12.dependabot — pin to >= 1.84 to mitigate 3 CVEs")
        }
    }
}

dependencies {
    implementation(project(":shared"))

    // Explicit constraint pin so Dependabot tracks this dependency directly
    // and reports any future CVEs against the version we're using.
    constraints {
        implementation(libs.bouncycastle.bcprov) {
            because("WU-B12.dependabot — pin to >= 1.84")
        }
        implementation(libs.bouncycastle.bcpkix) {
            because("WU-B12.dependabot — pin to >= 1.84")
        }
    }

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    // WorkManager
    implementation(libs.workmanager.ktx)
    implementation(libs.workmanager.hilt)
    ksp(libs.workmanager.hilt.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DateTime
    implementation(libs.kotlinx.datetime)

    // Sentry
    implementation(libs.sentry.android)
    implementation(libs.sentry.kotlin.extensions)

    // Ktor (needed for DI module HttpClient)
    implementation(libs.ktor.client.android)

    // Supabase (needed for DI module to reference SupabaseClient type)
    implementation(libs.supabase.postgrest)

    // LocalBroadcastManager
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // PostHog
    implementation(libs.posthog.android)

    // Timber
    implementation(libs.timber)

    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
