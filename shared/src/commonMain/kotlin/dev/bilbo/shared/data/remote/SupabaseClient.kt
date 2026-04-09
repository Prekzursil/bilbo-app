package dev.bilbo.shared.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Factory function that constructs the shared [SupabaseClient].
 *
 * Call this once at application startup and retain the instance
 * for the lifetime of the process (e.g. via a Hilt singleton on Android,
 * or a top-level object on iOS).
 *
 * @param supabaseUrl  Your project's Supabase URL, e.g. https://xyz.supabase.co
 * @param supabaseKey  The project's public anon key.
 */
fun createBilboSupabaseClient(
    supabaseUrl: String,
    supabaseKey: String,
): SupabaseClient = createSupabaseClient(
    supabaseUrl = supabaseUrl,
    supabaseKey = supabaseKey,
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Functions)
}
