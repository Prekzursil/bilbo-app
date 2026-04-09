package dev.spark.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.spark.data.BilboDatabase

/**
 * Provides the SQLDelight [SqlDriver] for Android using [AndroidSqliteDriver],
 * which is backed by the platform's SQLiteOpenHelper.
 *
 * @param context An Android [Context] used to open or create the database file.
 */
class AndroidDatabaseDriver(private val context: Context) : DatabaseDriverFactory {

    /**
     * Create (or open) the "spark.db" SQLite database and return a configured driver.
     * The [BilboDatabase.Schema] callback handles CREATE TABLE / migration statements.
     */
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = BilboDatabase.Schema,
            context = context,
            name = DATABASE_NAME
        )
    }

    companion object {
        const val DATABASE_NAME = "spark.db"
    }
}
