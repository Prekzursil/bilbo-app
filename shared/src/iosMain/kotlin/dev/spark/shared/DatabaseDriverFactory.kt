package dev.spark.shared

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.spark.shared.db.SparkDatabase

/**
 * iOS-specific implementation of the SQLDelight driver factory.
 *
 * Instantiate once at app startup (e.g. from SwiftUI's @main entry point)
 * and pass to [SparkDatabase.invoke].
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(SparkDatabase.Schema, "spark.db")
}
