package dev.bilbo.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.bilbo.data.BilboDatabase

/**
 * Provides the SQLDelight [SqlDriver] for iOS/macOS using [NativeSqliteDriver],
 * which is backed by SQLite3 via the Kotlin/Native runtime.
 *
 * The database file is stored in the platform's default documents directory,
 * which is the standard location on iOS for user-generated / persistent data.
 */
class IosDatabaseDriver : DatabaseDriverFactory {

    /**
     * Create (or open) the "bilbo.db" SQLite database and return a configured driver.
     * The [BilboDatabase.Schema] callback handles CREATE TABLE / migration statements.
     */
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = BilboDatabase.Schema,
            name = DATABASE_NAME
        )
    }

    companion object {
        const val DATABASE_NAME = "bilbo.db"
    }
}
