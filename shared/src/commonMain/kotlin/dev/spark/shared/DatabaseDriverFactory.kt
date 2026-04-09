package dev.spark.shared

import app.cash.sqldelight.db.SqlDriver

/**
 * Expect declaration for the platform-specific SQLDelight driver factory.
 *
 * Each platform (Android, iOS) provides an `actual` implementation that
 * creates the appropriate [SqlDriver] for that platform.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
