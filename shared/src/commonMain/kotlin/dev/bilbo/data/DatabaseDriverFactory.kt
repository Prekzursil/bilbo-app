package dev.bilbo.data

import app.cash.sqldelight.db.SqlDriver

/**
 * Common interface implemented by each platform to supply a [SqlDriver].
 * This follows the SQLDelight multiplatform driver pattern so that shared
 * code can create [dev.bilbo.data.BilboDatabase] without referencing
 * platform-specific classes directly.
 */
interface DatabaseDriverFactory {
    /**
     * Create a platform-appropriate [SqlDriver] connected to the Bilbo database.
     * Each call to [createDriver] may return the same or a new driver instance,
     * depending on the platform implementation.
     */
    fun createDriver(): SqlDriver
}
