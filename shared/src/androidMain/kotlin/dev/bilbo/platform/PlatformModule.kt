package dev.bilbo.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import dev.bilbo.data.AndroidDatabaseDriver
import dev.bilbo.data.DatabaseDriverFactory
import dev.bilbo.data.BilboDatabase

/**
 * Android-specific dependency wiring for the shared module.
 *
 * Instantiate this once (e.g. in your Application class) and pass it into any
 * shared component or DI graph that requires platform-provided objects.
 *
 * Example (Application subclass):
 * ```kotlin
 * class BilboApp : Application() {
 *     val platformModule by lazy { PlatformModule(this) }
 * }
 * ```
 *
 * @param context The application [Context]. Avoid passing Activity contexts to
 *   prevent memory leaks; always use [Context.getApplicationContext].
 */
class PlatformModule(context: Context) {

    private val appContext: Context = context.applicationContext

    /**
     * Factory that produces an [AndroidSqliteDriver]-backed [SqlDriver].
     */
    val databaseDriverFactory: DatabaseDriverFactory by lazy {
        AndroidDatabaseDriver(appContext)
    }

    /**
     * Fully initialised [BilboDatabase] instance for use by repositories.
     * Lazily created — the database file is opened on first access.
     */
    val database: BilboDatabase by lazy {
        BilboDatabase(databaseDriverFactory.createDriver())
    }

    /**
     * Convenience accessor that returns the [SqlDriver] used by [database].
     * Useful when injecting the driver directly into lower-level components.
     */
    val sqlDriver: SqlDriver by lazy {
        databaseDriverFactory.createDriver()
    }
}
