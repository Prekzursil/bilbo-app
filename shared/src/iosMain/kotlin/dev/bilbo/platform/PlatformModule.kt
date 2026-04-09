package dev.bilbo.platform

import app.cash.sqldelight.db.SqlDriver
import dev.bilbo.data.DatabaseDriverFactory
import dev.bilbo.data.IosDatabaseDriver
import dev.bilbo.data.BilboDatabase

/**
 * iOS-specific dependency wiring for the shared module.
 *
 * Instantiate this once from Swift (e.g. in AppDelegate or the SwiftUI @main
 * entry point) and pass it into your shared ViewModels or repositories.
 *
 * Swift example:
 * ```swift
 * let platformModule = PlatformModule()
 * let database = platformModule.database
 * ```
 *
 * The class is open so that Swift code can subclass it if additional
 * platform bindings are needed in future.
 */
class PlatformModule {

    /**
     * Factory that produces a [NativeSqliteDriver]-backed [SqlDriver].
     * The database file lands in the app's Documents directory on the
     * iOS filesystem.
     */
    val databaseDriverFactory: DatabaseDriverFactory by lazy {
        IosDatabaseDriver()
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
