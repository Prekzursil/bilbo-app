package dev.spark.app.enforcement

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.spark.enforcement.CooldownPersistence
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [CooldownPersistence] backed by [SharedPreferences].
 *
 * All cooldown expiry timestamps are stored under keys prefixed with `cdl_`
 * in a dedicated `spark_cooldowns` preference file.
 */
@Singleton
class SharedPrefsCooldownPersistence @Inject constructor(
    @ApplicationContext context: Context,
) : CooldownPersistence {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("spark_cooldowns", Context.MODE_PRIVATE)

    private fun prefKey(packageName: String) = "cdl_$packageName"

    override fun save(packageName: String, expiryEpochSeconds: Long) {
        prefs.edit().putLong(prefKey(packageName), expiryEpochSeconds).apply()
    }

    override fun clear(packageName: String) {
        prefs.edit().remove(prefKey(packageName)).apply()
    }

    override fun loadAll(): Map<String, Long> {
        return prefs.all
            .filter { (key, _) -> key.startsWith("cdl_") }
            .mapNotNull { (key, value) ->
                val packageName = key.removePrefix("cdl_")
                val expiry = (value as? Long) ?: return@mapNotNull null
                packageName to expiry
            }
            .toMap()
    }
}
