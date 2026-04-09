# Spark Android ProGuard rules

# Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.spark.**$$serializer { *; }
-keepclassmembers class dev.spark.** {
    *** Companion;
}
-keepclasseswithmembers class dev.spark.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# OkHttp (used by Ktor Android engine)
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Sentry
-keepattributes LineNumberTable,SourceFile
-dontwarn io.sentry.**
-keep class io.sentry.** { *; }

# PostHog
-keep class com.posthog.** { *; }

# SQLDelight
-keep class app.cash.sqldelight.** { *; }

# Supabase
-keep class io.github.jan.tennert.supabase.** { *; }

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Timber
-dontwarn org.jetbrains.annotations.**

# Remove logging in release
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
