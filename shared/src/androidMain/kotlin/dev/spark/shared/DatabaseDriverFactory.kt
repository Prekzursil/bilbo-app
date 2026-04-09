package dev.spark.shared

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.spark.shared.db.SparkDatabase

/**
 * Android-specific implementation of the SQLDelight driver factory.
 *
 * Instantiate once (e.g. via Hilt) and pass to [SparkDatabase.invoke].
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(SparkDatabase.Schema, context, "spark.db")
}
